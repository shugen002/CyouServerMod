package space.shugen.KHBot;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import space.shugen.KHBot.types.KHEventListener;
import space.shugen.KHBot.types.KHResponse;
import space.shugen.KHBot.types.responses.GetGateway;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.apache.logging.log4j.LogManager.getLogger;

public class KHBot extends Thread {
    public static final String getGatewayAPIURL = "https://www.kaiheila.cn/api/v3/gateway/index";
    private static final Logger logger = getLogger(KHBot.class);
    private final String token;
    private final ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(5);
    private final OkHttpClient client;
    private final ArrayBlockingQueue<Action> workQueue;
    private volatile boolean stop = false;

    private KHConnection connection;
    private ScheduledFuture<?> helloTimeout;
    private ScheduledFuture<?> heartbeatTimeout;
    private ScheduledFuture<?> heartbeatInterval;
    private int lastSN = 0;
    private ArrayList<JSONObject> buffer = new ArrayList<>();

    ArrayList<KHEventListener> KHEventListeners = new ArrayList<>();

    public KHBot(String token) {
        this.workQueue = new ArrayBlockingQueue<>(100);
        this.setName("KHBot");
        this.token = token;
        logger.info("starting");
        this.client = new OkHttpClient();
//        this.client = getUnsafeOkHttpClient();
        this.start();
        this.workQueue.add(new Action("connect"));
    }

    public void connect() {
        this.getGateway((url) -> this.workQueue.add(new Action("GetGatewaySuccess", url)), (e) -> this.workQueue.add(new Action("GetGatewayFailed", e)));
    }

    public void connectWebsocket(String url) {
        this.connection = new KHConnection(URI.create(url), false);
        this.connection.onOpen((self, handshakeData) -> {
            if (this.connection != self) {
                self.close();
                return;
            }
            this.workQueue.add(new Action("ConnectionOpen"));
        });
        this.connection.onClose((self, code, reason, remote) -> {
            if (this.connection != self) {
                return;
            }
            this.workQueue.add(new Action("ConnectionClose", new Object[]{code, reason, remote}));

        });
        this.connection.onMessage((self, message) -> {
            if (this.connection != self) {
                self.close();
                return;
            }
            this.workQueue.add(new Action("ConnectionMessage", message));
        });
        this.connection.onError((self, ex) -> {
            if (this.connection != self) {
                self.close();
                return;
            }
            this.workQueue.add(new Action("ConnectionError", ex));
        });
        this.lastSN = 0;
        this.buffer = new ArrayList<>();
        this.connection.connect();
    }

    public void getGateway(Consumer<String> success, Consumer<Throwable> fail) {
        try {
            //noinspection ConstantConditions
            HttpUrl url = HttpUrl.parse(getGatewayAPIURL).newBuilder().addQueryParameter("compress", "0").build();
            this.get(url.toString(), new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    fail.accept(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    try {
                        //noinspection ConstantConditions
                        String data = response.body().string();
                        KHResponse<GetGateway> res = JSONObject.parseObject(data, new TypeReference<>() {
                        });
                        if (res.code == 0) {
                            success.accept(res.data.url);
                        } else {
                            fail.accept(new Exception(data));
                        }
                    } catch (NullPointerException ex) {
                        fail.accept(ex);
                    }

                }
            });
        } catch (Throwable e) {
            fail.accept(e);
        }
    }

    public void get(String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bot " + this.token)
                .build();

        client.newCall(request).enqueue(callback);
    }

    @SuppressWarnings("PlaceholderCountMatchesArgumentCount")
    @Override
    public void run() {
        while (!stop) {
            try {
                Action action = workQueue.poll(1, TimeUnit.SECONDS);
                if (action == null) {
                    continue;
                }
                if (stop) {
                    return;
                }
                switch (action.action) {
                    case "connect" -> this.connect();
                    case "GetGatewaySuccess" -> {
                        this.connectWebsocket((String) action.data);
                        logger.info("Get gateway url, start connecting.");
                    }
                    case "GetGatewayFailed" -> {
                        logger.error("Get Gateway Failed", action.data);
                        sleep(1000);
                        this.connect();
                    }
                    case "ConnectionOpen" -> {
                        logger.info("Websocket Connected , wait for hello packet.");
                        this.helloTimeout = timer.schedule(() -> this.connection.close(4001, "Hello Timeout"), 6, TimeUnit.SECONDS);
                    }
                    case "ConnectionClose" -> {
                        logger.warn("Connection Close ", ((Object[]) action.data)[0], ((Object[]) action.data)[1], ((Object[]) action.data)[2]);
                        if (this.helloTimeout != null) {
                            this.helloTimeout.cancel(true);
                        }
                        if (this.heartbeatTimeout != null) {
                            this.heartbeatTimeout.cancel(true);
                        }
                        if (this.heartbeatInterval != null) {
                            this.heartbeatInterval.cancel(true);
                        }
                        this.connection = null;
                        sleep(1000);
                        this.connect();
                    }
                    case "ConnectionError" -> logger.error("Connection Error ", action.data);
                    case "ConnectionMessage" -> {
                        this.onMessage((String) action.data);
                    }
                    default -> logger.warn("Unknown Action", action);
                }
            } catch (InterruptedException ex) {
                logger.error(ex);
            }
        }
    }

    private void onMessage(String data) {
        try {
            JSONObject packet = JSONObject.parseObject(data);
            switch (packet.getInteger("s")) {
                case 0:
                    // Event Packet
                    int sn = packet.getInteger("sn");
                    if (this.lastSN + 1 == sn) {
                        this.lastSN = sn;
                        this.onKHEvent(packet.getJSONObject("d"));
                        boolean hasMore = true;
                        while (hasMore) {
                            var filtered = this.buffer.stream().filter((e) -> {
                                int asn = e.getInteger("sn");
                                return this.lastSN + 1 == asn;
                            });
                            if (filtered.count() > 0) {
                                var b = filtered.toArray(JSONObject[]::new)[0];
                                this.lastSN = b.getInteger("sn");
                                this.onKHEvent(b.getJSONObject("d"));
                                hasMore = true;
                            } else {
                                hasMore = false;
                            }
                            this.buffer.removeIf((e) -> {
                                int asn = e.getInteger("sn");
                                return this.lastSN >= asn;
                            });
                        }
                    } else if (sn <= this.lastSN) {
                        return;
                    } else {
                        this.buffer.add(packet);
                    }
                    break;
                case 1:
                    // Hello Packet
                    if (this.helloTimeout != null) {
                        this.helloTimeout.cancel(true);
                    }
                    logger.info("Received Hello Packet");
                    this.startHeartBeat();
                    break;
                case 2:
                    logger.info("WTF? Ping Packet!");
                    // Ping Packet should Not Receive
                    break;
                case 3:
                    // Pong Packet
                    if (this.heartbeatTimeout != null) {
                        this.heartbeatTimeout.cancel(true);
                    }
                    break;
                case 5:
                    // Reconnect Packet
                    if (this.connection != null) {
                        this.connection.close(1000, "Server Ask for");
                    }
                    break;
                case 6:
                    logger.info("WTF? Resume Packet! I didn't write any code about resume.");
                    // Resume Packet
                    break;
            }
        } catch (Exception ex) {
            logger.error(ex);
        }
    }

    public void startHeartBeat() {
        this.heartbeatInterval = timer.scheduleAtFixedRate(() -> {
            if (this.connection != null) {
                var packet = new JSONObject();
                packet.fluentPut("s", 2);
                packet.fluentPut("sn", this.lastSN);
                this.connection.send(packet.toString());
                this.heartbeatTimeout = timer.schedule(() -> {
                    if (this.connection != null) {
                        this.connection.close(4502, "Heartbeat Timeout");
                    }
                }, 6, TimeUnit.SECONDS);
            }
        }, 25, 30, TimeUnit.SECONDS);
    }

    static class Action {
        String action;
        Object data;

        Action(String action) {
            this.action = action;
        }

        Action(String action, Object data) {
            this.action = action;
            this.data = data;
        }
    }

    private void onKHEvent(JSONObject data) {
        this.KHEventListeners.forEach((e) -> {
            try {
                e.run(data);
            } catch (Exception ce) {
                logger.error(ce);
            }
        });
    }

    public void onEvent(KHEventListener listener) {
        this.KHEventListeners.add(listener);
    }

    public Call createAssets(File file, String type) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse(type))).build();
        Request request = new Request.Builder()
                .addHeader("Authorization", "Bot " + this.token)
                .url("https://www.kaiheila.cn/api/v3/asset/create")
                .post(requestBody)
                .build();
        return client.newCall(request);
    }

    public Call sendChannelMessage(String targetId, int type, String content) {
        var req = new JSONObject();
        req.put("type", type);
        req.put("target_id", targetId);
        req.put("content", content);
        RequestBody requestBody = RequestBody.create(req.toJSONString(), MediaType.parse("application/json;charset=utf-8"));
        Request request = new Request.Builder()
                .addHeader("Authorization", "Bot " + this.token)
                .url("https://www.kaiheila.cn/api/v3/message/create")
                .post(requestBody)
                .build();
        return client.newCall(request);
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain,
                                                       String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain,
                                                       String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8888)))
                    .addInterceptor(logging)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void dispose() {
        this.stop = true;
        this.timer.shutdownNow();
        this.connection.close();
        this.client.dispatcher().cancelAll();
    }
}


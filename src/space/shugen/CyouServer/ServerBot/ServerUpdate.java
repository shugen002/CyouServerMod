package space.shugen.CyouServer.ServerBot;

import arc.Core;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import mindustry.Vars;
import mindustry.gen.Groups;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.Logger;
import space.shugen.CyouServer.util.DownloadUtil;
import space.shugen.KHBot.types.CardComponent.Card;
import space.shugen.KHBot.types.CardComponent.File;
import space.shugen.KHBot.types.KHEventListener;
import space.shugen.KHBot.types.messages.KHFileMessage;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ServerUpdate {
    private final ServerBot self;
    private ServerUpdateConfig config;
    private String guildId = "";
    private String channelId = "";
    private String authorId = "";
    private final OkHttpClient client = new OkHttpClient();
    private Thread thread;
    private final ArrayBlockingQueue<Message> workQueue;
    private volatile boolean updateReady = false;
    private volatile String updateFile;
    private int idle = 0;
    private Timer idleTimer;

    public ServerUpdate(ServerBot serverBot) {
        this.self = serverBot;
        this.config = self.config.update;
        this.workQueue = new ArrayBlockingQueue<>(100);
        if (config.enable) {
            serverBot.bot.onEvent(this.eventHandler);
            thread = new SUThread();
            thread.start();
            idleTimer = new Timer("idleChecker", true);
            idleTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (Groups.player.size() <= 0) {
                        idle = Math.min(idle + 1, 30);
                        if (updateReady && idle > 1) {
                            self.jobs.add(() -> {
                                Vars.net.dispose();
                                Core.app.exit();
                            });
                        }
                    } else {
                        idle = 0;
                    }
                }
            }, 10000, 20000);
        }
    }


    private final KHEventListener eventHandler = new KHEventListener() {
        @Override
        public void run(JSONObject event) {
            if (!isGroupMessage(event) || !isTargetChannel(event) || !isTargetAuthor(event)) {
                return;
            }
            var messageType = event.getInteger("type");
            if (messageType == 10) {
                try {
                    List<Card> cards = JSONArray.parseArray(event.getString("content"), Card.class);
                    File result = null;
                    for (int i = 0; i < cards.size(); i++) {
                        var card = cards.get(i);
                        for (int j = 0; j < card.modules.size(); j++) {
                            if (card.modules.getJSONObject(j).getString("type").equalsIgnoreCase("file")) {
                                File file = card.modules.getObject(j, File.class);
                                if (file.title.contains("Server") || file.title.equals("server-release.jar")) {
                                    result = file;
                                    break;
                                }
                            }
                        }
                        if (result != null) {
                            self.logger.info("Receive New Server Jar , Update Later. " + result.src);
                            workQueue.add(new Message("newVersion", result.src));
                            break;
                        }
                    }
                } catch (Exception e) {
                    self.logger.error("Fail to parse Card Message " + event.getString("msg_id"), e);
                    self.logger.error(event.toString());
                }

            } else if (messageType == 4) {
                var fileMessage = event.toJavaObject(KHFileMessage.class);
                if (fileMessage.extra.attachments.name.equalsIgnoreCase("server-release.jar")) {
                    self.logger.info("Receive New Server Jar , Update Later. " + fileMessage.extra.attachments.url);
                    workQueue.add(new Message("newVersion", fileMessage.extra.attachments.url));
                }
            }
//            self.logger.info(event.toJSONString());
        }

        private boolean isGroupMessage(JSONObject event) {
            var type = event.getString("channel_type");
            return type != null && type.equalsIgnoreCase("GROUP");
        }

        private boolean isTargetChannel(JSONObject event) {
            var groupId = event.getString("target_id");
            return groupId != null && groupId.equalsIgnoreCase(config.Channel);
        }

        private boolean isTargetAuthor(JSONObject event) {
            var authorId = event.getString("author_id");
            return authorId != null && authorId.equalsIgnoreCase(config.Author);
        }
    };

    public void dispose() {
        this.idleTimer.cancel();
        this.idleTimer.purge();
        this.workQueue.add(new Message("stop", ""));
    }

    private class SUThread extends Thread {
        private boolean stop = false;
        private Logger logger;

        SUThread() {
            super();
            this.logger = self.getLogger();
        }

        @Override
        public void run() {
            while (!stop) {
                try {
                    var message = workQueue.poll(10000, TimeUnit.MILLISECONDS);
                    if (message != null) {
                        switch (message.type) {
                            case "stop":
                                stop = true;
                                break;
                            case "newVersion":
                                var filename = RandomStringUtils.random(10, "abcdefghijklmnopqrstuvwxyz");
                                DownloadUtil.get().download((String) message.data, ".", "server-release.jar", new DownloadUtil.OnDownloadListener() {
                                    @Override
                                    public void onDownloadSuccess() {
                                        self.logger.info("success");
                                        self.logger.info((String) message.data);
                                        updateFile = filename;
                                        updateReady = true;
                                        logger.info(idle);
                                        if (idle > 1) {
                                            self.jobs.add(() -> {
                                                Vars.net.dispose();
                                                Core.app.exit();
                                            });
                                        }
                                    }

                                    @Override
                                    public void onDownloadFailed() {
                                        self.logger.error("fail");
                                    }
                                });
                                break;

                            default:
                                self.logger.error("Unknown Type: " + message.type);
                        }
                    }
                } catch (InterruptedException ex) {
                    logger.error("未知错误", ex);
                }
            }
        }
    }

    private class Message {
        String type;
        Object data;

        public Message(String type, Object data) {
            this.type = type;
            this.data = data;
        }
    }
}

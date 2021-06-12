package space.shugen.KHBot;

import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;

import static org.apache.logging.log4j.LogManager.getLogger;

public class KHConnection extends WebSocketClient {
    private final boolean compress;
    private final Logger logger;

    /**
     * Constructs a WebSocketClient instance and sets it to the connect to the
     * specified URI. The channel does not attampt to connect automatically. The connection
     * will be established once you call <var>connect</var>.
     *
     * @param serverUri the server URI to connect to
     */
    public KHConnection(URI serverUri, boolean compress) {
        super(serverUri);
        this.compress = compress;
        this.logger = getLogger("KHConnection");
    }

    ArrayList<OpenEventListener> openEventListeners = new ArrayList<>();
    ArrayList<MessageEventListener> messageEventListeners = new ArrayList<>();
    ArrayList<CloseEventListener> closeEventListeners = new ArrayList<>();
    ArrayList<ErrorEventListener> errorEventListeners = new ArrayList<>();


    @Override
    public void onOpen(ServerHandshake handshakeData) {
        for (OpenEventListener listener : openEventListeners) {
            try {
                listener.run(this, handshakeData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMessage(String message) {
        for (MessageEventListener listener : messageEventListeners) {
            try {
                listener.run(this, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        for (CloseEventListener listener : closeEventListeners) {
            try {
                listener.run(this, code, reason, remote);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onError(Exception ex) {
        for (ErrorEventListener listener : errorEventListeners) {
            try {
                listener.run(this, ex);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // if the error is fatal then onClose will be called additionally
    }

    public void onOpen(OpenEventListener listener) {
        openEventListeners.add(listener);
    }

    public void onMessage(MessageEventListener listener) {
        messageEventListeners.add(listener);
    }

    public void onClose(CloseEventListener listener) {
        closeEventListeners.add(listener);
    }

    public void onError(ErrorEventListener listener) {
        errorEventListeners.add(listener);
    }
}

interface OpenEventListener {
    public void run(KHConnection self, ServerHandshake handshakeData);
}

interface MessageEventListener {
    public void run(KHConnection self, String message);
}

interface CloseEventListener {
    public void run(KHConnection self, int code, String reason, boolean remote);
}

interface ErrorEventListener {
    public void run(KHConnection self, Exception ex);
}
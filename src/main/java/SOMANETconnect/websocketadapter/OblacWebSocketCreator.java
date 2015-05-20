package SOMANETconnect.websocketadapter;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

public class OblacWebSocketCreator implements WebSocketCreator {
    private OblacWebSocketAdapter oblacWebSocketAdapter;

    public OblacWebSocketCreator() {
        this.oblacWebSocketAdapter = new OblacWebSocketAdapter();
    }

    public OblacWebSocketAdapter getOblacWebSocketAdapter() {
        return oblacWebSocketAdapter;
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
        return oblacWebSocketAdapter;
    }
}

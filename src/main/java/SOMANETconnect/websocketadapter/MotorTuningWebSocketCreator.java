package SOMANETconnect.websocketadapter;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

public class MotorTuningWebSocketCreator implements WebSocketCreator {
    private MotorTuningWebSocketAdapter motorTuningWebSocketAdapter;

    public MotorTuningWebSocketCreator() {
        this.motorTuningWebSocketAdapter = new MotorTuningWebSocketAdapter();
    }

    public MotorTuningWebSocketAdapter getMotorTuningWebSocketAdapter() {
        return motorTuningWebSocketAdapter;
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
        return motorTuningWebSocketAdapter;
    }
}

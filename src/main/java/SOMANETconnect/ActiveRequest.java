package SOMANETconnect;

import org.java_websocket.WebSocket;

public class ActiveRequest {
    private Process process;
    private WebSocket webSocketConnection;

    public ActiveRequest(WebSocket webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public WebSocket getWebSocketConnection() {
        return webSocketConnection;
    }
}

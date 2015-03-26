package SOMANETconnect;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Used for asynchronous process execution that send each line of the process output to the client on the other side of
 * the web socket.
 */
public class SystemProcessLive implements Runnable {

    private static final Logger logger = Logger.getLogger(SystemProcessLive.class.getName());

    class StreamReader extends Thread {
        InputStream is;
        String requestId;
        WebSocket webSocketConnection;

        // Reads everything from the input stream until it is empty
        StreamReader(InputStream is, String requestId, WebSocket webSocketConnection) {
            this.is = is;
            this.requestId = requestId;
            this.webSocketConnection = webSocketConnection;
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                JSONRPC2Response response;
                while ((line = br.readLine()) != null) {
                    Map<String, String> outputLine = new HashMap<>();
                    outputLine.put(Constants.EXEC_LIVE, line);
                    response = new JSONRPC2Response(outputLine, requestId);
                    webSocketConnection.send(response.toString());
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

    }

    private String command;
    private String requestId;
    private Map<String, ActiveRequest> activeRequestRegister;

    public SystemProcessLive(String command, Map<String, ActiveRequest> activeRequestRegister, String requestId)
            throws IOException {
        this.command = command;
        this.requestId = requestId;
        this.activeRequestRegister = activeRequestRegister;
    }

    @Override
    public void run() {
        ActiveRequest thisRequest = activeRequestRegister.get(requestId);
        ProcessBuilder processBuilder = new ProcessBuilder().command(command).redirectErrorStream(true);
        Process process;
        try {
            process = processBuilder.start();
            thisRequest.setProcess(process);
        } catch (IOException e) {
            logger.error("An I/O error occurred during process executing (Command: " + command + "; Request ID: "
                    + requestId + ")");
            activeRequestRegister.remove(requestId);
            return;
        }

        WebSocket webSocketConnection = thisRequest.getWebSocketConnection();

        StreamReader outputReader = new StreamReader(process.getInputStream(), requestId, webSocketConnection);
        outputReader.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            // NO-OP
        }

        JSONRPC2Response response = new JSONRPC2Response(Constants.EXEC_DONE, requestId);
        webSocketConnection.send(response.toString());

        activeRequestRegister.remove(requestId);
    }
}

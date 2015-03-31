package SOMANETconnect;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

/**
 * Used for asynchronous process execution that send each line of the process output to the client on the other side of
 * the web socket.
 */
public class SystemProcessLive implements Runnable {

    private static final Logger logger = Logger.getLogger(SystemProcessLive.class.getName());

    class StreamReader extends Thread {
        InputStream is;

        // Reads everything from the input stream until it is empty
        StreamReader(InputStream is) {
            this.is = is;
        }

        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = readLineWithTerm(br)) != null) {
                    sendWebSocketResponse(line, requestId);
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }

    private List<String> command;
    private String requestId;
    private Map<String, Process> activeRequestRegister;
    private RemoteEndpoint remoteEndpoint;

    public SystemProcessLive(
            List<String> command, Map<String, Process> activeRequestRegister, String requestId, RemoteEndpoint remoteEndpoint)
            throws IOException {
        this.command = command;
        this.requestId = requestId;
        this.activeRequestRegister = activeRequestRegister;
        this.remoteEndpoint = remoteEndpoint;
    }

    @Override
    public void run() {
        ProcessBuilder processBuilder = new ProcessBuilder().command(command).redirectErrorStream(true);
        Process process;
        try {
            process = processBuilder.start();
            activeRequestRegister.put(requestId, process);
        } catch (IOException e) {
            String wholeCommand = "";
            for (String arg : command) {
                wholeCommand += arg + " ";
            }
            logger.error(e.getMessage() + " (Command: " + wholeCommand + "; Request ID: " + requestId + ")");
            sendWebSocketResponse(e.getMessage(), requestId);
            sendWebSocketResponse(Constants.EXEC_DONE, requestId);
            activeRequestRegister.remove(requestId);
            return;
        }


        StreamReader outputReader = new StreamReader(process.getInputStream());
        outputReader.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            // NO-OP
        }

        sendWebSocketResponse(Constants.EXEC_DONE, requestId);

        activeRequestRegister.remove(requestId);
    }

    private void sendWebSocketResponse(String message, String requestId) {
        try {
            JSONRPC2Response response = new JSONRPC2Response(message, requestId);
            remoteEndpoint.sendString(response.toString());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private static String readLineWithTerm(BufferedReader reader) throws IOException {
        int code;
        StringBuilder line = new StringBuilder();

        while ((code = reader.read()) != -1) {
            char ch = (char) code;

            line.append(ch);

            if (ch == '\n') {
                break;
            } else if (ch == '\r') {
                reader.mark(1);
                ch = (char) reader.read();

                if (ch == '\n') {
                    line.append(ch);
                } else {
                    reader.reset();
                }

                break;
            }
        }

        return (line.length() == 0 ? null : line.toString());
    }
}

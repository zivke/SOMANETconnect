package SOMANETconnect.systemprocess;

import SOMANETconnect.miscellaneous.Constants;
import SOMANETconnect.miscellaneous.Util;
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
                String segment;
                while ((segment = readLineWithTermAndLimit(br)) != null) {
                    Util.sendWebSocketResultResponse(remoteEndpoint, segment, requestId);
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
        processBuilder.environment().putAll(Constants.environmentVariables);
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
            Util.sendWebSocketResultResponse(remoteEndpoint, e.getMessage(), requestId);
            Util.sendWebSocketResultResponse(remoteEndpoint, Constants.EXEC_DONE, requestId);
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

        Util.sendWebSocketResultResponse(remoteEndpoint, Constants.EXEC_DONE, requestId);

        activeRequestRegister.remove(requestId);
    }

    private static String readLineWithTermAndLimit(BufferedReader reader) throws IOException {
        int code;
        StringBuilder segment = new StringBuilder();

        int counter = 0;
        while ((code = reader.read()) != -1) {
            char ch = (char) code;

            segment.append(ch);

            if (++counter > 25) {
                break;
            } else if (ch == '\n') {
                break;
            } else if (ch == '\r') {
                reader.mark(1);
                ch = (char) reader.read();

                if (ch == '\n') {
                    segment.append(ch);
                } else {
                    reader.reset();
                }

                break;
            }
        }

        return (segment.length() == 0 ? null : segment.toString());
    }
}

package SOMANETconnect;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import java.io.IOException;
import java.util.ArrayList;

public final class Util {
    private final static Logger logger = Logger.getLogger(Util.class.getName());

    public static void sendWebSocketResultResponse(RemoteEndpoint remoteEndpoint, Object value, Object requestId) {
        try {
            JSONRPC2Response response = new JSONRPC2Response(value, requestId);
            remoteEndpoint.sendString(response.toString());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static void sendWebSocketErrorResponse(RemoteEndpoint remoteEndpoint, JSONRPC2Error error, Object requestId) {
        try {
            JSONRPC2Response response = new JSONRPC2Response(error, requestId);
            remoteEndpoint.sendString(response.toString());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Kill any residual processes (that the main process may have started) by the unique name of the temporary file
     * used in the original command (the temporary file contains the ID of the request that was used to start the
     * process in its name).
     *
     * @param requestId ID of the request used to start the process (that needs cleaning up after)
     * @throws IOException
     */
    public static void linuxProcessCleanup(String requestId) throws IOException {
        // Kill any residual processes (that the main process may have started) by the unique name of
        // the temporary file used in the original command
        ArrayList<String> command = new ArrayList<>();
        command.add("pkill");
        command.add("-f");
        command.add(requestId);
        new SystemProcess(command);
    }
}

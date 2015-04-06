package SOMANETconnect;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import java.io.IOException;

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
}

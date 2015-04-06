package SOMANETconnect;

import SOMANETconnect.command.ListCommand;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.io.IOException;

public class MotorTuningWebSocketAdapter extends WebSocketAdapter {
    private static final int MB = 1024 * 1024;
    private final static Logger logger = Logger.getLogger(MotorTuningWebSocketAdapter.class.getName());

    private XscopeSocket xscopeSocket;

    @Override
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);
        getSession().getPolicy().setMaxTextMessageSize(10 * MB);
        getSession().setIdleTimeout(-1);
        logger.info("Socket connected to " + session.getRemoteAddress());
    }

    @Override
    public void onWebSocketText(String message) {
        super.onWebSocketText(message);

        // Parse request string
        JSONRPC2Request request;

        try {
            request = JSONRPC2Request.parse(message);
        } catch (JSONRPC2ParseException e) {
            logger.error(e.getMessage());
            return;
        }

        try {
            switch (request.getMethod()) {
                case Constants.START_XSCOPE:
                    if (xscopeSocket == null) {
                        xscopeSocket = new XscopeSocket("127.0.0.1", "10101");
                    }
                    if (!xscopeSocket.getListen()) {
                        (new Thread(xscopeSocket)).start();
                    }
                    break;
                case Constants.STOP_XSCOPE:
                    xscopeSocket.close();
                    break;
                case Constants.LIST:
                    ListCommand listCommand = new ListCommand();
                    sendWebSocketResponse(listCommand.getDeviceList(), request.getID());
                    break;
                case Constants.SEND:
                    // TODO
                    break;
                case Constants.START_MOTOR:
                    // TODO
                    break;
                case Constants.STOP_MOTOR:
                    // TODO
                    break;
                case Constants.ERASE_FIRMWARE:
                    // TODO
                    break;
                default:
                    sendWebSocketResponse(JSONRPC2Error.METHOD_NOT_FOUND, request.getID());
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            sendWebSocketResponse(JSONRPC2Error.INTERNAL_ERROR, request.getID());
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        logger.info("Socket Closed: [" + statusCode + "] " + reason);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        logger.error(cause);
    }

    private void sendWebSocketResponse(Object value, Object requestId) {
        try {
            JSONRPC2Response response = new JSONRPC2Response(value, requestId);
            getRemote().sendString(response.toString());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}

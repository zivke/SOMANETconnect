package SOMANETconnect;

import SOMANETconnect.command.ListCommand;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.java_websocket.util.Base64;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SomanetWebSocketAdapter extends WebSocketAdapter {
    private final static Logger logger = Logger.getLogger(SomanetWebSocketAdapter.class.getName());

    private Map<String, Process> activeRequestRegister = new HashMap<>();

    @Override
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);
        getSession().getPolicy().setMaxTextMessageSize(1024 * 1024);
        getSession().setIdleTimeout(-1);
        logger.info("Socket connected to " + session.getRemoteAddress());
    }

    @Override
    public void onWebSocketText(String message) {
        super.onWebSocketText(message);

        // Parse request string
        JSONRPC2Request request;
        JSONRPC2Response response;

        try {
            request = JSONRPC2Request.parse(message);
        } catch (JSONRPC2ParseException e) {
            logger.error(e.getMessage());
            return;
        }

        try {
            switch (request.getMethod()) {
                case Constants.LIST:
                    ListCommand listCommand = new ListCommand();
                    sendWebSocketResponse(listCommand.getDeviceList(), request.getID());
                    break;
                case Constants.FLASH:
                    flash(request);
                    break;
                case Constants.INTERRUPT:
                    String requestIdToInterrupt = String.valueOf(request.getNamedParams().get(Constants.ID));
                    Process process = activeRequestRegister.get(requestIdToInterrupt);
                    if (process != null) {
                        process.destroy();
                    }
                    break;
                default:
                    sendWebSocketResponse(JSONRPC2Error.METHOD_NOT_FOUND, request.getID());
            }
        } catch (IOException e) {
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

    private void flash(JSONRPC2Request request) throws IOException {
        String requestId = String.valueOf(request.getID());
        String deviceId = String.valueOf(request.getNamedParams().get("id"));
        Path flashFilePath = Files.createTempFile("oblac_", null);
        byte[] data = Base64.decode(String.valueOf(request.getNamedParams().get("content")));
        Files.write(flashFilePath, data);
        List<String> command = new ArrayList<>();
        command.add("./xflash");
        command.add("--id");
        command.add(deviceId);
        command.add(flashFilePath.toString());
        (new Thread(new SystemProcessLive(command, activeRequestRegister, requestId, getRemote()))).start();
    }
}

package SOMANETconnect.websocketadapter;

import SOMANETconnect.command.ListCommand;
import SOMANETconnect.miscellaneous.Constants;
import SOMANETconnect.miscellaneous.Util;
import SOMANETconnect.systemprocess.SystemProcessLive;
import SOMANETconnect.systemtray.SomanetConnectSystemTray;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OblacWebSocketAdapter extends WebSocketAdapter {
    private static final int MB = 1024 * 1024;
    private final static Logger logger = Logger.getLogger(OblacWebSocketAdapter.class.getName());

    private Map<String, Process> activeRequestRegister = new HashMap<>();

    @Override
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);
        getSession().getPolicy().setMaxTextMessageSize(10 * MB);
        getSession().setIdleTimeout(-1);
        logger.info("Socket connected to " + session.getRemoteAddress());
        SomanetConnectSystemTray.getInstance().oblacConnected(true);
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
                case Constants.LIST:
                    ListCommand listCommand = new ListCommand();
                    Util.sendWebSocketResultResponse(getRemote(), listCommand.getDeviceList(), request.getID());
                    break;
                case Constants.FLASH:
                    flash(request);
                    break;
                case Constants.RUN:
                    run(request);
                    break;
                case Constants.INTERRUPT:
                    String requestIdToInterrupt = String.valueOf(request.getNamedParams().get(Constants.ID));
                    Process process = activeRequestRegister.get(requestIdToInterrupt);
                    Util.killProcess(process);
                    if (SystemUtils.IS_OS_LINUX) {
                        Util.linuxProcessCleanup(requestIdToInterrupt);
                    }
                    break;
                default:
                    Util.sendWebSocketErrorResponse(getRemote(), JSONRPC2Error.METHOD_NOT_FOUND, request.getID());
            }
        } catch (IOException e) {
            Util.sendWebSocketErrorResponse(getRemote(), JSONRPC2Error.INTERNAL_ERROR, request.getID());
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        logger.info("Socket Closed: [" + statusCode + "] " + reason);
        SomanetConnectSystemTray.getInstance().oblacConnected(false);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        logger.error(cause);
    }

    private Path saveFileFromRequest(JSONRPC2Request request) throws IOException {
        // Add the request ID to the temporary file so it can later be used to identify and kill any residual processes
        Path filePath = Files.createTempFile("SOMANETconnect_" + request.getID() + "_", null);
        byte[] data = Base64.decodeBase64(String.valueOf(request.getNamedParams().get("content")));
        Files.write(filePath, data);
        return filePath;
    }

    private void flash(JSONRPC2Request request) throws IOException {
        String requestId = String.valueOf(request.getID());
        String deviceId = String.valueOf(request.getNamedParams().get("id"));
        Path flashFilePath = saveFileFromRequest(request);
        List<String> command = new ArrayList<>();
        command.add(System.getProperty("user.dir") + "/bin/xflash");
        command.add("--id");
        command.add(deviceId);
        command.add(flashFilePath.toString());
        (new Thread(new SystemProcessLive(command, activeRequestRegister, requestId, getRemote()))).start();
    }

    private void run(JSONRPC2Request request) throws IOException {
        String requestId = String.valueOf(request.getID());
        String deviceId = String.valueOf(request.getNamedParams().get("id"));
        Path runFilePath = saveFileFromRequest(request);
        List<String> command = new ArrayList<>();
        command.add(System.getProperty("user.dir") + "/bin/xrun");
        command.add("--io");
        command.add("--id");
        command.add(deviceId);
        command.add(runFilePath.toString());
        (new Thread(new SystemProcessLive(command, activeRequestRegister, requestId, getRemote()))).start();
    }
}

package SOMANETconnect;

import SOMANETconnect.command.ListCommand;
import com.google.inject.Inject;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.util.Base64;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SomanetServer extends WebSocketServer {

    private final static Logger logger = Logger.getLogger(SomanetServer.class.getName());

    private Map<String, ActiveRequest> activeRequestRegister;

    @Inject
    public SomanetServer(Configuration applicationConfiguration, SSLContext sslContext) throws Exception {
        super(new InetSocketAddress(applicationConfiguration.getInt("application.ws.port")));
//        setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));

        this.activeRequestRegister = new HashMap<>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("Connected ... " + conn.getLocalSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.info(String.format("Session %s closed because of %s", conn.getLocalSocketAddress(), reason));
    }

    @Override
    public void onMessage(WebSocket webSocketConnection, String message) {
        // Parse request string
        JSONRPC2Request request;
        JSONRPC2Response response;

        try {
            request = JSONRPC2Request.parse(message);
        } catch (JSONRPC2ParseException e) {
            logger.error(e.getMessage());
            return;
        }

        String requestId = String.valueOf(request.getID());

        try {
            switch (request.getMethod()) {
                case Constants.LIST:
                    ListCommand listCommand = new ListCommand();
                    response = new JSONRPC2Response(listCommand.getDeviceList(), requestId);
                    webSocketConnection.send(response.toString());
                    break;
                case Constants.FLASH:
                    activeRequestRegister.put(requestId, new ActiveRequest(webSocketConnection));
                    String deviceId = String.valueOf(request.getNamedParams().get("id"));
                    Path flashFilePath = Files.createTempFile("oblac_", null);
                    byte[] data = Base64.decode(String.valueOf(request.getNamedParams().get("content")));
                    Files.write(flashFilePath, data);
                    (new Thread(
                            new SystemProcessLive(
                                    "./xflash --id " + deviceId + " " + flashFilePath.toString(),
                                    activeRequestRegister,
                                    requestId
                            )
                    )).start();
                    break;
                case Constants.INTERRUPT:
                    String requestIdToInterrupt = String.valueOf(request.getNamedParams().get(Constants.ID));
                    ActiveRequest activeRequest = activeRequestRegister.get(requestIdToInterrupt);
                    if (activeRequest != null && activeRequest.getProcess() != null) {
                        activeRequest.getProcess().destroy();
                    }
                    break;
                default:
                    response = new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, request.getID());
                    webSocketConnection.send(response.toString());
            }
        } catch (IOException e) {
            response = new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR, request.getID());
            webSocketConnection.send(response.toString());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        if (conn != null) {
            // some errors like port binding failed may not be assignable to a specific websocket
            logger.error("An error occurred: " + conn.toString());
        }
    }
}

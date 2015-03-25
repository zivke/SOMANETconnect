package SOMANETconnect;

import SOMANETconnect.command.ListCommand;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.KeyStore;

public class SomanetServer extends WebSocketServer {
    // load up the key store
    private final static String STORE_TYPE = "JKS";
    private final static String KEY_STORE_PATH = "keystore.jks";
    private final static String STORE_PASSWORD = "password";
    private final static String KEY_PASSWORD = "password";

    private final static Logger logger = Logger.getLogger(SomanetServer.class.getName());

    public SomanetServer(int port) throws Exception {
        super(new InetSocketAddress(port));
        setWebSocketFactory(new DefaultSSLWebSocketServerFactory(createSSLContext()));
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
    public void onMessage(WebSocket conn, String message) {
        // Parse request string
        JSONRPC2Request request;
        JSONRPC2Response response = null;

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
                    response = new JSONRPC2Response(listCommand.getDeviceList(), request.getID());
                    break;
                default:
                    response = new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, request.getID());
            }
        } catch (IOException e) {
            response = new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR, request.getID());
        } finally {
            if (response == null) {
                response = new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR, request.getID());
            }
            conn.send(response.toString());
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

    private SSLContext createSSLContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(STORE_TYPE);
        URL keyStoreUrl = getClass().getClassLoader().getResource(KEY_STORE_PATH);
        if (keyStoreUrl == null) {
            throw new IOException("The key store file couldn't be read");
        }
        File keyStoreFile = new File(keyStoreUrl.getPath());
        keyStore.load(new FileInputStream(keyStoreFile), STORE_PASSWORD.toCharArray());

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, KEY_PASSWORD.toCharArray());
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStore);

        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }
}

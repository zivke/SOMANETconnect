package SOMANETconnect.guice;

import SOMANETconnect.websocketadapter.MotorTuningWebSocketAdapter;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class MotorTuningServerProvider implements Provider<Server> {
    private final static Logger logger = Logger.getLogger(MotorTuningServerProvider.class.getName());

    @Inject
    private Configuration applicationConfiguration;

    @Override
    public Server get() {
        Server server = new Server();

        // Secure (SSL) web socket connection (wss)
        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        SslContextFactory sslContextFactory = new SslContextFactory();
        try {
            KeyStore keyStore = KeyStore.getInstance("jks");
            // Load the keystore file as a stream because of the problems that happen when its path is used (when the
            // application is ran as a JAR file)
            keyStore.load(
                    MotorTuningServerProvider.class.getResourceAsStream(
                            applicationConfiguration.getString("application.ws.ssl.key_store_path")),
                    applicationConfiguration.getString("application.ws.ssl.key_store_password").toCharArray());
            sslContextFactory.setKeyStore(keyStore);
            sslContextFactory.setKeyStorePassword(applicationConfiguration.getString("application.ws.ssl.key_store_password"));
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            logger.error(e.getMessage());
        }

        ServerConnector sslConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(https));
        sslConnector.setPort(applicationConfiguration.getInt("application.ws.motor_tuning.port"));

        server.addConnector(sslConnector);

        // Setup the basic application "context" for this application at "/" and a web socket handler
        ContextHandler context = new ContextHandler();
        context.setContextPath("/");
        context.setHandler(new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory webSocketServletFactory) {
                webSocketServletFactory.register(MotorTuningWebSocketAdapter.class);
            }
        });
        server.setHandler(context);

        return server;
    }
}

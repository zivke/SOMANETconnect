package SOMANETconnect.guice;

import SOMANETconnect.OblacWebSocketAdapter;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class OblacServerProvider implements Provider<Server> {
    @Inject
    private Configuration applicationConfiguration;

    @Override
    public Server get() {
        Server server = new Server();

        // Normal web socket connection (ws)
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(applicationConfiguration.getInt("application.ws.port"));
        server.addConnector(connector);

        // Secure (SSL) web socket connection (wss)
        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(
                OblacWebSocketAdapter.class.getResource(
                        applicationConfiguration.getString("application.ws.ssl.key_store_path")).getPath());
        sslContextFactory.setKeyStorePassword(applicationConfiguration.getString("application.ws.ssl.key_store_password"));
        sslContextFactory.setKeyManagerPassword(applicationConfiguration.getString("application.ws.ssl.key_manager_password"));

        ServerConnector sslConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(https));
        sslConnector.setPort(applicationConfiguration.getInt("application.ws.ssl.port"));

        server.addConnector(sslConnector);

        // Setup the basic application "context" for this application at "/" and a web socket handler
        ContextHandler context = new ContextHandler();
        context.setContextPath("/");
        context.setHandler(new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory webSocketServletFactory) {
                webSocketServletFactory.register(OblacWebSocketAdapter.class);
            }
        });
        server.setHandler(context);

        return server;
    }
}

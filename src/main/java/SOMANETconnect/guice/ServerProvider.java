package SOMANETconnect.guice;

import SOMANETconnect.SomanetWebSocketAdapter;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class ServerProvider implements Provider<Server> {
    @Inject
    private Configuration applicationConfiguration;

    @Override
    public Server get() {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(applicationConfiguration.getInt("application.ws.port"));
        server.addConnector(connector);

        // Setup the basic application "context" for this application at "/" and a web socket handler
        ContextHandler context = new ContextHandler();
        context.setContextPath("/");
        context.setHandler(new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory webSocketServletFactory) {
                webSocketServletFactory.register(SomanetWebSocketAdapter.class);
            }
        });
        server.setHandler(context);

        return server;
    }
}

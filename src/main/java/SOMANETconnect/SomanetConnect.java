package SOMANETconnect;

import SOMANETconnect.guice.MyModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;

public class SomanetConnect {

    private static final Logger logger = Logger.getLogger(SystemProcessLive.class.getName());

    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new MyModule());
        Server server = injector.getInstance(Server.class);

        try {
            server.start();
            server.join();
        } catch (Throwable t) {
            logger.error(t.getMessage());
        }
    }
}

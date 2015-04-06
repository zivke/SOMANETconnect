package SOMANETconnect;

import SOMANETconnect.guice.MyModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;

public class SomanetConnect {

    private static final Logger logger = Logger.getLogger(SystemProcessLive.class.getName());

    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new MyModule());
        Server oblacServer = injector.getInstance(Key.get(Server.class, Names.named("OBLAC")));
        Server motorTuningServer = injector.getInstance(Key.get(Server.class, Names.named("MotorTuning")));

        try {
            oblacServer.start();
            motorTuningServer.start();
            oblacServer.join();
            motorTuningServer.join();
        } catch (Throwable t) {
            logger.error(t.getMessage());
        }
    }
}

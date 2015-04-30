package SOMANETconnect;

import SOMANETconnect.guice.MyModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.MultiException;

import java.net.BindException;

public class SomanetConnect {

    private static final Logger logger = Logger.getLogger(SystemProcessLive.class.getName());

    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new MyModule());
        Server oblacServer = injector.getInstance(Key.get(Server.class, Names.named("OBLAC")));
        Server motorTuningServer = injector.getInstance(Key.get(Server.class, Names.named("MotorTuning")));
        SomanetConnectSystemTray somanetConnectSystemTray = injector.getInstance(SomanetConnectSystemTray.class);

        try {
            oblacServer.start();
            motorTuningServer.start();
            oblacServer.join();
            motorTuningServer.join();
        } catch (Throwable t) {
            logger.error(t.getMessage());
            if (t instanceof MultiException) {
                for (Throwable nestedThrowable : ((MultiException) t).getThrowables()) {
                    if (nestedThrowable instanceof BindException) {
                        if (nestedThrowable.getMessage().equalsIgnoreCase("Address already in use")) {
                            somanetConnectSystemTray.showError("The port needed for SOMANETconnect to run properly " +
                                    "is currently occupied. Make sure that there isn't another instance of " +
                                    "SOMANETconnect already running and try again.");
                        }
                        break;
                    }
                }
            }
        }
    }
}

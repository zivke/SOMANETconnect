package SOMANETconnect;

import SOMANETconnect.miscellaneous.SomanetConnectServerFactory;
import SOMANETconnect.systemprocess.SystemProcessLive;
import SOMANETconnect.systemtray.SomanetConnectSystemTray;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.MultiException;

import java.net.BindException;

public class SomanetConnect {

    private static final Logger logger = Logger.getLogger(SystemProcessLive.class.getName());

    public static void main(String[] args) throws Exception {
        // Initialize the SomanetConnectSystemTray singleton
        SomanetConnectSystemTray.getInstance();

        Server oblacServer = SomanetConnectServerFactory.createOblacServer();
        Server motorTuningServer = SomanetConnectServerFactory.createMotorTuningServer();

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
                            SomanetConnectSystemTray.getInstance().showError("The port needed for SOMANETconnect to " +
                                    "run properly is currently occupied. Make sure that there isn't another instance " +
                                    "of SOMANETconnect already running and try again.");
                        }
                        break;
                    }
                }
            }
        }
    }
}

package SOMANETconnect;

import SOMANETconnect.device.DeviceManager;
import SOMANETconnect.miscellaneous.SomanetConnectServerFactory;
import SOMANETconnect.systemprocess.SystemProcessLive;
import SOMANETconnect.systemtray.SomanetConnectSystemTray;
import SOMANETconnect.websocketadapter.OblacWebSocketCreator;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.MultiException;

import java.net.BindException;

public class SomanetConnect {

    private static final Logger logger = Logger.getLogger(SystemProcessLive.class.getName());

    public static void main(String[] args) throws Exception {
        DeviceManager deviceManager = DeviceManager.getInstance();

        // Initialize the SomanetConnectSystemTray singleton and set its
        SomanetConnectSystemTray somanetConnectSystemTray = SomanetConnectSystemTray.getInstance();
        deviceManager.addObserver(somanetConnectSystemTray);

        OblacWebSocketCreator oblacWebSocketCreator = new OblacWebSocketCreator();
        deviceManager.addObserver(oblacWebSocketCreator.getOblacWebSocketAdapter());
        Server oblacServer = SomanetConnectServerFactory.createOblacServer(oblacWebSocketCreator);

        try {
            oblacServer.start();
            oblacServer.join();
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
        } finally {
            deviceManager.close();
        }
    }
}

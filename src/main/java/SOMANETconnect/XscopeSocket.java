package SOMANETconnect;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import java.util.HashMap;
import java.util.Map;

public class XscopeSocket implements Runnable {
    private final static Logger logger = Logger.getLogger(XscopeSocket.class.getName());

    public native int initializeSocket(String address, String port);

    public native void handleSockets(int fileDescriptors[], int fileDescriptorCount);

    public native void releaseSocket(int xScopeSocketFileDescriptor);

    static {
        System.loadLibrary("XscopeSocket");
    }

    private String address;
    private String port;
    private int socketFileDescriptors[];

    private boolean listen;
    private RemoteEndpoint remoteEndpoint;

    private Object requestId;

    private int motorParameterProbe;

    public XscopeSocket(String address, String port, RemoteEndpoint remoteEndpoint) {
        this.address = address;
        this.port = port;
        this.listen = false;
        this.remoteEndpoint = remoteEndpoint;
    }

    @Override
    public void run() {
        listen = true;
        int fd = initializeSocket(address, port);
        this.socketFileDescriptors = new int[]{fd};
        if (this.socketFileDescriptors[0] == -1) {
            logger.error("The xScope socket could not be initialized.");
            return;
        }
        handleSockets(socketFileDescriptors, 1);
    }

    public void stop() {
        this.listen = false;
    }

    public void close() {
        stop();
        releaseSocket(socketFileDescriptors[0]);
    }

    public boolean getListen() {
        return listen;
    }

    public void setRequestId(Object requestId) {
        this.requestId = requestId;
    }

    public void hookRegistrationReceived(int sockfd, int xscope_probe, String name) {
        if (Constants.MOTOR_PARAMETERS.equals(name)) {
            motorParameterProbe = xscope_probe;
        } else { // Send the probe registration message to the web client
            Map<String, Object> result = new HashMap<>();
            result.put(Constants.TYPE, Constants.XSCOPE_PROBE_REG);
            result.put(Constants.NUMBER, xscope_probe);
            result.put(Constants.NAME, name);
            Util.sendWebSocketResultResponse(remoteEndpoint, result, requestId);
        }
    }
}

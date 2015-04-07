package SOMANETconnect;

import org.apache.log4j.Logger;

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

    public XscopeSocket(String address, String port) {
        this.address = address;
        this.port = port;
        this.listen = false;
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
}

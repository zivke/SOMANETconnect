package SOMANETconnect;

import java.io.IOException;

public class XscopeSocket implements Runnable {
    public native int initializeSocket(String address, String port);

    public native void handleSockets(int fileDescriptors[], int fileDescriptorCount);

    public native void releaseSocket(int xScopeSocketFileDescriptor);

    static {
        System.loadLibrary("XscopeSocket");
    }

    private int socketFileDescriptors[];
    private boolean listen;

    public XscopeSocket(String address, String port) throws IOException {
        int fd = initializeSocket(address, port);
        this.socketFileDescriptors = new int[]{fd};
        if (this.socketFileDescriptors[0] == -1) {
            throw new IOException("The xScope socket could not be initialized.");
        }
        this.listen = false;
    }

    @Override
    public void run() {
        listen = true;
        handleSockets(socketFileDescriptors, 1);
    }

    public void stop() {
        this.listen = false;
    }

    public void close() {
        stop();
        releaseSocket(socketFileDescriptors[0]);
    }

    public int getIntListen() {
        return listen ? 1 : 0;
    }

    public boolean getListen() {
        return listen;
    }
}

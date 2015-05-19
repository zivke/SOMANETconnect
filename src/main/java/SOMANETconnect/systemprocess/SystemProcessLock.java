package SOMANETconnect.systemprocess;

import java.util.concurrent.locks.ReentrantLock;

public class SystemProcessLock extends ReentrantLock {
    private static SystemProcessLock systemProcessLock = new SystemProcessLock();

    private SystemProcessLock() {
        super();
    }

    public static SystemProcessLock getInstance() {
        return systemProcessLock;
    }
}

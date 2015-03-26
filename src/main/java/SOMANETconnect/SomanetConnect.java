package SOMANETconnect;

import SOMANETconnect.guice.MyModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class SomanetConnect {
    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new MyModule());
        SomanetServer somanetServer = injector.getInstance(SomanetServer.class);
        somanetServer.start();
        System.out.println("SOMANETconnect successfully started on " + somanetServer.getAddress().getHostName() + ":"
                + somanetServer.getPort());
    }
}
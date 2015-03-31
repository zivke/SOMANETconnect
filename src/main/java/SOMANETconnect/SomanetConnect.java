package SOMANETconnect;

import SOMANETconnect.guice.MyModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.java_websocket.WebSocketImpl;

public class SomanetConnect {
    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new MyModule());
        WebSocketImpl.DEBUG = true;
        SomanetServer somanetServer = injector.getInstance(SomanetServer.class);
        somanetServer.start();
        System.out.println("SOMANETconnect successfully started on " + somanetServer.getAddress().getHostName() + ":"
                + somanetServer.getPort());
    }
}
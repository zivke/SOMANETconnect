package SOMANETconnect.guice;

import SOMANETconnect.SomanetServer;
import com.google.inject.AbstractModule;

public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        requestInjection(SomanetServer.class);

        // Application configuration
        bind(org.apache.commons.configuration.Configuration.class)
                .toProvider(ApplicationConfigurationProvider.class).asEagerSingleton();

        bind(javax.net.ssl.SSLContext.class).toProvider(SSLContextProvider.class).asEagerSingleton();
    }
}

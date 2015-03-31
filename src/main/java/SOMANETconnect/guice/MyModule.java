package SOMANETconnect.guice;

import com.google.inject.AbstractModule;

public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        // Application configuration
        bind(org.apache.commons.configuration.Configuration.class)
                .toProvider(ApplicationConfigurationProvider.class).asEagerSingleton();

        // Jetty server
        bind(org.eclipse.jetty.server.Server.class).toProvider(ServerProvider.class).asEagerSingleton();
    }
}

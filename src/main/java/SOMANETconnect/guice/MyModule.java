package SOMANETconnect.guice;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        // Application configuration
        bind(org.apache.commons.configuration.Configuration.class)
                .toProvider(ApplicationConfigurationProvider.class).asEagerSingleton();

        // Jetty OBLAC server
        bind(org.eclipse.jetty.server.Server.class).annotatedWith(Names.named("OBLAC"))
                .toProvider(OblacServerProvider.class).asEagerSingleton();
    }
}

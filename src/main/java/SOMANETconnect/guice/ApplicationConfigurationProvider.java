package SOMANETconnect.guice;

import com.google.inject.Provider;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Global application configuration provided as singleton. All properties reside in
 * application.properties and depend upon the profile (environment) the application is built for.
 * <p/>
 * You should not inject provider, but rather the Configuration. For example:
 *
 * @Inject Configuration applicationConfiguration
 */
public class ApplicationConfigurationProvider implements Provider<Configuration> {

    @Override
    public Configuration get() {

        CompositeConfiguration configuration = new CompositeConfiguration();

        try {
            configuration.addConfiguration(new PropertiesConfiguration(
                    ApplicationConfigurationProvider.class.getResource("/application.properties")));
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

        return configuration;
    }
}

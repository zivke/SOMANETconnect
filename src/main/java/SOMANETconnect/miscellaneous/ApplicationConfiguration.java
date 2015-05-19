package SOMANETconnect.miscellaneous;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

// Singleton
public final class ApplicationConfiguration extends CompositeConfiguration {
    private static ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();

    private ApplicationConfiguration() {
        super();
        try {
            addConfiguration(
                    new PropertiesConfiguration(ApplicationConfiguration.class.getResource("/application.properties")));
        } catch (ConfigurationException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static ApplicationConfiguration getInstance() {
        return applicationConfiguration;
    }
}

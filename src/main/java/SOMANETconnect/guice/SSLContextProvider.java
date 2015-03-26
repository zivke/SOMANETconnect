package SOMANETconnect.guice;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;

public class SSLContextProvider implements Provider<SSLContext> {

    private final static Logger logger = Logger.getLogger(SSLContextProvider.class.getName());

    @Inject
    Configuration applicationConfiguration;

    @Override
    public SSLContext get() {
        SSLContext sslContext = null;
        try {
            KeyStore keyStore = KeyStore.getInstance(applicationConfiguration.getString("application.ssl.store_type"));
            URL keyStoreUrl = getClass().getClassLoader().getResource(
                    applicationConfiguration.getString("application.ssl.key_store_path"));
            if (keyStoreUrl == null) {
                throw new IOException("The key store file couldn't be read");
            }
            File keyStoreFile = new File(keyStoreUrl.getPath());
            keyStore.load(
                    new FileInputStream(keyStoreFile),
                    applicationConfiguration.getString("application.ssl.store_password").toCharArray());

            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(applicationConfiguration.getString("application.ssl.algorithm"));
            keyManagerFactory.init(
                    keyStore, applicationConfiguration.getString("application.ssl.key_password").toCharArray());
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(applicationConfiguration.getString("application.ssl.algorithm"));
            trustManagerFactory.init(keyStore);

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        } catch (Exception e) {
            logger.fatal("The SSL context couldn't be created");
        }
        return sslContext;
    }
}

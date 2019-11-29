package core.system;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Properties;

public class PropertiesManager {

    private String propertyFile;

    public PropertiesManager (String propertyFile) {
        this.propertyFile = propertyFile;
    }

    public String getProperty (String key) throws Exception {
        Properties sysProps = new Properties();
        sysProps.load(new FileInputStream(propertyFile));
        return sysProps.getProperty(key);
    }

    public void storeProperties (Properties properties) throws Exception {
        properties.store(new FileWriter(propertyFile), "Elastic Coin System Properties");
    }
}

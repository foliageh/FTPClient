package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {
    private static final String CONFIG_FILE_SYSTEM_PROPERTY_NAME = "configFile";
    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String propertyValue = properties.getProperty(key);
        if (propertyValue == null || propertyValue.isEmpty())
            return defaultValue;
        if (propertyValue.equals("true"))
            return true;
        else if (propertyValue.equals("false"))
            return false;
        return defaultValue;
    }

    private static void loadProperties() {
        String filePath = System.getProperty(CONFIG_FILE_SYSTEM_PROPERTY_NAME);
        if (filePath != null && !filePath.isEmpty())
            loadPropertiesFromFile(filePath);
        else loadPropertiesFromResources();
    }

    private static void loadPropertiesFromFile(String filePath) {
        try (InputStream input = new FileInputStream(filePath)) {
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Error loading properties from file: " + filePath);
            System.exit(1);
        }
    }

    // Convenient method when running from the IDE
    private static void loadPropertiesFromResources() {
        try (InputStream input = Configuration.class.getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(input);
        } catch (IOException | NullPointerException ignored) {
            // anyway the default values will be loaded
        }
    }
}

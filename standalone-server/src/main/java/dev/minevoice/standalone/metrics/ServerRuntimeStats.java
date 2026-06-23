package dev.minevoice.standalone.metrics;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record ServerRuntimeStats(int activeSessions, BandwidthStats bandwidthStats) {
    public static String currentVersion() {
        try (InputStream input = ServerRuntimeStats.class.getClassLoader()
                .getResourceAsStream("minevoice-version.properties")) {
            if (input == null) {
                return "development";
            }
            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty("version", "development");
        } catch (IOException exception) {
            return "development";
        }
    }
}

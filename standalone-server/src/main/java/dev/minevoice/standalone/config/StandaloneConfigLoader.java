package dev.minevoice.standalone.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class StandaloneConfigLoader {
    public StandaloneConfig load(Path path) {
        StandaloneConfig defaults = StandaloneConfig.defaults();
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException exception) {
                throw new IllegalStateException("failed to load standalone config: " + path, exception);
            }
            normalizeBomKey(properties, "bindHost");
        }

        return new StandaloneConfig(
                stringValue(properties, "bindHost", "MINEVOICE_BIND_HOST", defaults.bindHost()),
                intValue(properties, "bindPort", "MINEVOICE_BIND_PORT", defaults.bindPort()),
                stringValue(properties, "sharedSecret", "MINEVOICE_SHARED_SECRET", defaults.sharedSecret()),
                intValue(properties, "maxPlayers", "MINEVOICE_MAX_PLAYERS", defaults.maxPlayers()),
                doubleValue(properties, "proximityDistance", "MINEVOICE_PROXIMITY_DISTANCE", defaults.proximityDistance()),
                booleanValue(properties, "enableBandwidthStats", "MINEVOICE_ENABLE_BANDWIDTH_STATS", defaults.enableBandwidthStats()),
                booleanValue(properties, "enableDebugLog", "MINEVOICE_ENABLE_DEBUG_LOG", defaults.enableDebugLog())
        );
    }

    private static String stringValue(Properties properties, String key, String envKey, String fallback) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return properties.getProperty(key, fallback);
    }

    private static int intValue(Properties properties, String key, String envKey, int fallback) {
        String value = stringValue(properties, key, envKey, Integer.toString(fallback));
        return Integer.parseInt(value);
    }

    private static boolean booleanValue(Properties properties, String key, String envKey, boolean fallback) {
        String value = stringValue(properties, key, envKey, Boolean.toString(fallback));
        return Boolean.parseBoolean(value);
    }

    private static double doubleValue(Properties properties, String key, String envKey, double fallback) {
        String value = stringValue(properties, key, envKey, Double.toString(fallback));
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static void normalizeBomKey(Properties properties, String key) {
        Object removed = properties.remove("\uFEFF" + key);
        String bomValue = removed instanceof String value ? value : null;
        if (bomValue != null && !properties.containsKey(key)) {
            properties.setProperty(key, bomValue);
        }
    }
}

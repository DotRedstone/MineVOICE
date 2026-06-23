package dev.minevoice.neoforge.config;

import dev.minevoice.common.config.VoiceMode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class MineVoiceModConfigLoader {
    public MineVoiceModConfig load(Path path) {
        MineVoiceModConfig defaults = MineVoiceModConfig.localDefaults();
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException exception) {
                throw new IllegalStateException("failed to load MineVOICE server config: " + path, exception);
            }
            normalizeBomKey(properties, "mode");
        } else {
            writeDefaults(path, defaults);
        }

        return new MineVoiceModConfig(
                modeProperty(properties, "mode", defaults.mode()),
                properties.getProperty("bindHost", defaults.bindHost()),
                intProperty(properties, "bindPort", defaults.bindPort()),
                properties.getProperty("advertiseHost", defaults.advertiseHost()),
                intProperty(properties, "advertisePort", defaults.advertisePort()),
                properties.getProperty("remoteVoiceHost", defaults.remoteVoiceHost()),
                intProperty(properties, "remoteVoicePort", defaults.remoteVoicePort()),
                properties.getProperty("sharedSecret", defaults.sharedSecret()),
                booleanProperty(properties, "enableDebugLog", defaults.enableDebugLog())
        );
    }

    private static VoiceMode modeProperty(Properties properties, String key, VoiceMode fallback) {
        try {
            return VoiceMode.valueOf(properties.getProperty(key, fallback.name()).toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static int intProperty(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static boolean booleanProperty(Properties properties, String key, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)));
    }

    private static void writeDefaults(Path path, MineVoiceModConfig defaults) {
        Properties properties = new Properties();
        properties.setProperty("mode", defaults.mode().name().toLowerCase());
        properties.setProperty("bindHost", defaults.bindHost());
        properties.setProperty("bindPort", Integer.toString(defaults.bindPort()));
        properties.setProperty("advertiseHost", defaults.advertiseHost());
        properties.setProperty("advertisePort", Integer.toString(defaults.advertisePort()));
        properties.setProperty("remoteVoiceHost", "voice.example.com");
        properties.setProperty("remoteVoicePort", Integer.toString(defaults.remoteVoicePort()));
        properties.setProperty("sharedSecret", defaults.sharedSecret());
        properties.setProperty("enableDebugLog", Boolean.toString(defaults.enableDebugLog()));
        try {
            Files.createDirectories(path.getParent());
            try (var output = Files.newOutputStream(path)) {
                properties.store(output, "MineVOICE server configuration. Restart the Minecraft server after changes.");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to create MineVOICE server config: " + path, exception);
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

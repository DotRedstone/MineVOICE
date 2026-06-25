package dev.minevoice.neoforge.config;

import dev.minevoice.common.config.VoiceMode;
import dev.minevoice.common.audio.VoiceCodecFactory;

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
                stringProperty(properties, "localVoiceBindHost", "bindHost", defaults.localVoiceBindHost()),
                intProperty(properties, "localVoiceBindPort", "bindPort", defaults.localVoiceBindPort()),
                stringProperty(properties, "localVoiceAdvertiseHost", "advertiseHost", defaults.localVoiceAdvertiseHost()),
                intProperty(properties, "localVoiceAdvertisePort", "advertisePort", defaults.localVoiceAdvertisePort()),
                properties.getProperty("remoteVoiceHost", defaults.remoteVoiceHost()),
                intProperty(properties, "remoteVoicePort", defaults.remoteVoicePort()),
                properties.getProperty("sharedSecret", defaults.sharedSecret()),
                intProperty(properties, "proximityDistance", defaults.proximityDistance()),
                booleanProperty(properties, "enableLanVoiceServer", defaults.enableLanVoiceServer()),
                booleanProperty(properties, "enableSpatialDebug", defaults.enableSpatialDebug()),
                VoiceCodecFactory.normalizeCodecName(properties.getProperty("voiceCodec", defaults.voiceCodec())),
                properties.getProperty("audioPlaybackBackend", defaults.audioPlaybackBackend()),
                properties.getProperty("spatialBackend", defaults.spatialBackend()),
                booleanProperty(properties, "enableOcclusion", defaults.enableOcclusion()),
                doubleProperty(properties, "occlusionStrength", defaults.occlusionStrength()),
                booleanProperty(properties, "occlusionLowPass", defaults.occlusionLowPass()),
                booleanProperty(properties, "enableSoundPhysicsCompat", defaults.enableSoundPhysicsCompat()),
                intProperty(properties, "jitterBufferMs", defaults.jitterBufferMs()),
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

    private static int intProperty(Properties properties, String key, String legacyKey, int fallback) {
        try {
            return Integer.parseInt(stringProperty(properties, key, legacyKey, Integer.toString(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static double doubleProperty(Properties properties, String key, double fallback) {
        try {
            return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static boolean booleanProperty(Properties properties, String key, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)));
    }

    private static String stringProperty(Properties properties, String key, String legacyKey, String fallback) {
        String value = properties.getProperty(key);
        if (value != null) {
            return value;
        }
        return properties.getProperty(legacyKey, fallback);
    }

    private static void writeDefaults(Path path, MineVoiceModConfig defaults) {
        Properties properties = new Properties();
        properties.setProperty("mode", defaults.mode().name().toLowerCase());
        properties.setProperty("localVoiceBindHost", defaults.localVoiceBindHost());
        properties.setProperty("localVoiceBindPort", Integer.toString(defaults.localVoiceBindPort()));
        properties.setProperty("localVoiceAdvertiseHost", defaults.localVoiceAdvertiseHost());
        properties.setProperty("localVoiceAdvertisePort", Integer.toString(defaults.localVoiceAdvertisePort()));
        properties.setProperty("enableLanVoiceServer", Boolean.toString(defaults.enableLanVoiceServer()));
        properties.setProperty("remoteVoiceHost", "voice.example.com");
        properties.setProperty("remoteVoicePort", Integer.toString(defaults.remoteVoicePort()));
        properties.setProperty("sharedSecret", defaults.sharedSecret());
        properties.setProperty("proximityDistance", Integer.toString(defaults.proximityDistance()));
        properties.setProperty("enableSpatialDebug", Boolean.toString(defaults.enableSpatialDebug()));
        properties.setProperty("voiceCodec", defaults.voiceCodec());
        properties.setProperty("audioPlaybackBackend", defaults.audioPlaybackBackend());
        properties.setProperty("spatialBackend", defaults.spatialBackend());
        properties.setProperty("enableOcclusion", Boolean.toString(defaults.enableOcclusion()));
        properties.setProperty("occlusionStrength", Double.toString(defaults.occlusionStrength()));
        properties.setProperty("occlusionLowPass", Boolean.toString(defaults.occlusionLowPass()));
        properties.setProperty("enableSoundPhysicsCompat", Boolean.toString(defaults.enableSoundPhysicsCompat()));
        properties.setProperty("jitterBufferMs", Integer.toString(defaults.jitterBufferMs()));
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

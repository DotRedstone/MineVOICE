package dev.minevoice.neoforge.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class FileClientSettingsStore implements ClientSettingsStore {
    private final Path path;
    private volatile ClientAudioSettings cachedSettings;

    public FileClientSettingsStore(Path path) {
        this.path = path;
    }

    @Override
    public ClientAudioSettings load() {
        ClientAudioSettings cached = cachedSettings;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cachedSettings == null) {
                cachedSettings = loadFromDisk();
            }
            return cachedSettings;
        }
    }

    private ClientAudioSettings loadFromDisk() {
        ClientAudioSettings defaults = ClientAudioSettings.defaults();
        if (!Files.exists(path)) {
            return defaults;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException exception) {
            return defaults;
        }

        return new ClientAudioSettings(
                properties.getProperty("microphoneDevice", defaults.microphoneDevice()),
                properties.getProperty("outputDevice", defaults.outputDevice()),
                properties.getProperty("pushToTalkKey", defaults.pushToTalkKey()),
                floatProperty(properties, "masterVolume", defaults.masterVolume()),
                floatProperty(properties, "voiceChatVolume", defaults.voiceChatVolume()),
                floatProperty(properties, "microphoneVolume", defaults.microphoneVolume()),
                enumProperty(properties, "activationMode", VoiceActivationMode.class, defaults.activationMode()),
                floatProperty(properties, "voiceActivationThreshold", defaults.voiceActivationThreshold()),
                booleanProperty(properties, "spatialAudioEnabled", defaults.spatialAudioEnabled()),
                properties.getProperty("voiceCodec", defaults.voiceCodec()),
                booleanProperty(properties, "muted", defaults.muted()),
                booleanProperty(properties, "deafened", defaults.deafened()),
                booleanProperty(properties, "showDebugConnectionInfo", defaults.showDebugConnectionInfo())
        );
    }

    @Override
    public void save(ClientAudioSettings settings) {
        cachedSettings = settings;
        Properties properties = new Properties();
        properties.setProperty("microphoneDevice", settings.microphoneDevice());
        properties.setProperty("outputDevice", settings.outputDevice());
        properties.setProperty("pushToTalkKey", settings.pushToTalkKey());
        properties.setProperty("masterVolume", Float.toString(settings.masterVolume()));
        properties.setProperty("voiceChatVolume", Float.toString(settings.voiceChatVolume()));
        properties.setProperty("microphoneVolume", Float.toString(settings.microphoneVolume()));
        properties.setProperty("activationMode", settings.activationMode().name());
        properties.setProperty("voiceActivationThreshold", Float.toString(settings.voiceActivationThreshold()));
        properties.setProperty("spatialAudioEnabled", Boolean.toString(settings.spatialAudioEnabled()));
        properties.setProperty("voiceCodec", settings.voiceCodec());
        properties.setProperty("muted", Boolean.toString(settings.muted()));
        properties.setProperty("deafened", Boolean.toString(settings.deafened()));
        properties.setProperty("showDebugConnectionInfo", Boolean.toString(settings.showDebugConnectionInfo()));

        try {
            Files.createDirectories(path.getParent());
            try (OutputStream output = Files.newOutputStream(path)) {
                properties.store(output, "MineVOICE client settings");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to save MineVOICE client settings", exception);
        }
    }

    private static float floatProperty(Properties properties, String key, float fallback) {
        try {
            return Float.parseFloat(properties.getProperty(key, Float.toString(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static boolean booleanProperty(Properties properties, String key, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)));
    }

    private static <T extends Enum<T>> T enumProperty(Properties properties, String key, Class<T> enumType, T fallback) {
        try {
            return Enum.valueOf(enumType, properties.getProperty(key, fallback.name()));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}

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
                enumProperty(properties, "groupActivationMode", VoiceActivationMode.class, defaults.groupActivationMode()),
                floatProperty(properties, "groupVoiceActivationThreshold", defaults.groupVoiceActivationThreshold()),
                booleanProperty(properties, "spatialAudioEnabled", defaults.spatialAudioEnabled()),
                properties.getProperty("voiceCodec", defaults.voiceCodec()),
                properties.getProperty("audioPlaybackBackend", defaults.audioPlaybackBackend()),
                booleanProperty(properties, "muted", defaults.muted()),
                booleanProperty(properties, "deafened", defaults.deafened()),
                booleanProperty(properties, "hudEnabled", defaults.hudEnabled()),
                booleanProperty(properties, "nameplateIconsEnabled", defaults.nameplateIconsEnabled()),
                intProperty(properties, "hudIconSize", defaults.hudIconSize()),
                debugInfoLevel(properties, defaults.debugInfoLevel()),
                intProperty(properties, "debugRenderRaysMode", defaults.debugRenderRaysMode()),
                intProperty(properties, "groupMemberColor", defaults.groupMemberColor()),
                intProperty(properties, "outOfSightIndicatorMode", defaults.outOfSightIndicatorMode()),
                intProperty(properties, "occludedIndicatorMode", defaults.occludedIndicatorMode()),
                booleanProperty(properties, "hrtfEnabled", defaults.hrtfEnabled())
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
        properties.setProperty("groupActivationMode", settings.groupActivationMode().name());
        properties.setProperty("groupVoiceActivationThreshold", Float.toString(settings.groupVoiceActivationThreshold()));
        properties.setProperty("spatialAudioEnabled", Boolean.toString(settings.spatialAudioEnabled()));
        properties.setProperty("voiceCodec", settings.voiceCodec());
        properties.setProperty("audioPlaybackBackend", settings.audioPlaybackBackend());
        properties.setProperty("muted", Boolean.toString(settings.muted()));
        properties.setProperty("deafened", Boolean.toString(settings.deafened()));
        properties.setProperty("hudEnabled", Boolean.toString(settings.hudEnabled()));
        properties.setProperty("nameplateIconsEnabled", Boolean.toString(settings.nameplateIconsEnabled()));
        properties.setProperty("hudIconSize", Integer.toString(settings.hudIconSize()));
        properties.setProperty("debugInfoLevel", settings.debugInfoLevel().name());
        properties.setProperty("showDebugConnectionInfo", Boolean.toString(settings.showDebugConnectionInfo()));
        properties.setProperty("debugRenderRaysMode", String.valueOf(settings.debugRenderRaysMode()));
        properties.setProperty("groupMemberColor", String.valueOf(settings.groupMemberColor()));
        properties.setProperty("outOfSightIndicatorMode", String.valueOf(settings.outOfSightIndicatorMode()));
        properties.setProperty("occludedIndicatorMode", String.valueOf(settings.occludedIndicatorMode()));
        properties.setProperty("hrtfEnabled", Boolean.toString(settings.hrtfEnabled()));
        properties.setProperty("hrtfEnabled", Boolean.toString(settings.hrtfEnabled()));
        properties.setProperty("hrtfEnabled", Boolean.toString(settings.hrtfEnabled()));

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

    private static int intProperty(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static <T extends Enum<T>> T enumProperty(Properties properties, String key, Class<T> enumType, T fallback) {
        try {
            return Enum.valueOf(enumType, properties.getProperty(key, fallback.name()));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static DebugInfoLevel debugInfoLevel(Properties properties, DebugInfoLevel fallback) {
        if (properties.containsKey("debugInfoLevel")) {
            return enumProperty(properties, "debugInfoLevel", DebugInfoLevel.class, fallback);
        }
        return booleanProperty(properties, "showDebugConnectionInfo", false)
                ? DebugInfoLevel.BASIC
                : fallback;
    }
}

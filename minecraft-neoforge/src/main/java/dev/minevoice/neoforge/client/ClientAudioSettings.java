package dev.minevoice.neoforge.client;

public record ClientAudioSettings(
        String microphoneDevice,
        String outputDevice,
        String pushToTalkKey,
        float masterVolume,
        float voiceChatVolume,
        float microphoneVolume,
        VoiceActivationMode activationMode,
        float voiceActivationThreshold,
        boolean spatialAudioEnabled,
        boolean muted,
        boolean deafened,
        boolean showDebugConnectionInfo
) {
    public static ClientAudioSettings defaults() {
        return new ClientAudioSettings(
                "default",
                "default",
                "V",
                1.0F,
                1.0F,
                1.0F,
                VoiceActivationMode.PUSH_TO_TALK,
                0.35F,
                true,
                false,
                false,
                false
        );
    }

    public ClientAudioSettings withMicrophoneDevice(String value) {
        return new ClientAudioSettings(value, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, spatialAudioEnabled, muted, deafened, showDebugConnectionInfo);
    }

    public ClientAudioSettings withOutputDevice(String value) {
        return new ClientAudioSettings(microphoneDevice, value, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, spatialAudioEnabled, muted, deafened, showDebugConnectionInfo);
    }

    public ClientAudioSettings withPushToTalkKey(String value) {
        return new ClientAudioSettings(microphoneDevice, outputDevice, value, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, spatialAudioEnabled, muted, deafened, showDebugConnectionInfo);
    }

    public ClientAudioSettings withMasterVolume(float value) {
        return new ClientAudioSettings(microphoneDevice, outputDevice, pushToTalkKey, clampVolume(value), voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, spatialAudioEnabled, muted, deafened, showDebugConnectionInfo);
    }

    public ClientAudioSettings withVoiceChatVolume(float value) {
        return new ClientAudioSettings(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, clampVolume(value), microphoneVolume, activationMode, voiceActivationThreshold, spatialAudioEnabled, muted, deafened, showDebugConnectionInfo);
    }

    public ClientAudioSettings withMicrophoneVolume(float value) {
        return new ClientAudioSettings(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, clampVolume(value), activationMode, voiceActivationThreshold, spatialAudioEnabled, muted, deafened, showDebugConnectionInfo);
    }

    public ClientAudioSettings withActivationMode(VoiceActivationMode value) {
        return new ClientAudioSettings(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, value, voiceActivationThreshold, spatialAudioEnabled, muted, deafened, showDebugConnectionInfo);
    }

    public ClientAudioSettings withVoiceActivationThreshold(float value) {
        return new ClientAudioSettings(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, clampVolume(value), spatialAudioEnabled, muted, deafened, showDebugConnectionInfo);
    }

    public ClientAudioSettings withSpatialAudioEnabled(boolean value) {
        return new ClientAudioSettings(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, value, muted, deafened, showDebugConnectionInfo);
    }

    public ClientAudioSettings withMuted(boolean value) {
        return new ClientAudioSettings(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, spatialAudioEnabled, value, deafened, showDebugConnectionInfo);
    }

    public ClientAudioSettings withDeafened(boolean value) {
        return new ClientAudioSettings(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, spatialAudioEnabled, muted, value, showDebugConnectionInfo);
    }

    public ClientAudioSettings withShowDebugConnectionInfo(boolean value) {
        return new ClientAudioSettings(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, spatialAudioEnabled, muted, deafened, value);
    }

    private static float clampVolume(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}

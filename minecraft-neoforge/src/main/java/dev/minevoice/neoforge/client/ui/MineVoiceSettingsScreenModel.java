package dev.minevoice.neoforge.client.ui;

import dev.minevoice.neoforge.client.ClientAudioSettings;
import dev.minevoice.neoforge.client.VoiceActivationMode;

public final class MineVoiceSettingsScreenModel {
    private String microphoneDevice;
    private String outputDevice;
    private String pushToTalkKey;
    private float masterVolume;
    private float voiceChatVolume;
    private float microphoneVolume;
    private VoiceActivationMode activationMode;
    private float voiceActivationThreshold;
    private boolean spatialAudioEnabled;
    private String voiceCodec;
    private boolean muted;
    private boolean deafened;
    private boolean showDebugConnectionInfo;

    private MineVoiceSettingsScreenModel(ClientAudioSettings settings) {
        this.microphoneDevice = settings.microphoneDevice();
        this.outputDevice = settings.outputDevice();
        this.pushToTalkKey = settings.pushToTalkKey();
        this.masterVolume = settings.masterVolume();
        this.voiceChatVolume = settings.voiceChatVolume();
        this.microphoneVolume = settings.microphoneVolume();
        this.activationMode = settings.activationMode();
        this.voiceActivationThreshold = settings.voiceActivationThreshold();
        this.spatialAudioEnabled = settings.spatialAudioEnabled();
        this.voiceCodec = settings.voiceCodec();
        this.muted = settings.muted();
        this.deafened = settings.deafened();
        this.showDebugConnectionInfo = settings.showDebugConnectionInfo();
    }

    public static MineVoiceSettingsScreenModel from(ClientAudioSettings settings) {
        return new MineVoiceSettingsScreenModel(settings);
    }

    public ClientAudioSettings toSettings() {
        return new ClientAudioSettings(
                microphoneDevice,
                outputDevice,
                pushToTalkKey,
                masterVolume,
                voiceChatVolume,
                microphoneVolume,
                activationMode,
                voiceActivationThreshold,
                spatialAudioEnabled,
                voiceCodec,
                muted,
                deafened,
                showDebugConnectionInfo
        );
    }

    public void resetToDefaults() {
        ClientAudioSettings defaults = ClientAudioSettings.defaults();
        microphoneDevice = defaults.microphoneDevice();
        outputDevice = defaults.outputDevice();
        pushToTalkKey = defaults.pushToTalkKey();
        masterVolume = defaults.masterVolume();
        voiceChatVolume = defaults.voiceChatVolume();
        microphoneVolume = defaults.microphoneVolume();
        activationMode = defaults.activationMode();
        voiceActivationThreshold = defaults.voiceActivationThreshold();
        spatialAudioEnabled = defaults.spatialAudioEnabled();
        voiceCodec = defaults.voiceCodec();
        muted = defaults.muted();
        deafened = defaults.deafened();
        showDebugConnectionInfo = defaults.showDebugConnectionInfo();
    }

    public String titleKey() {
        return "screen.minevoice.settings";
    }

    public String microphoneDevice() {
        return microphoneDevice;
    }

    public void setMicrophoneDevice(String microphoneDevice) {
        this.microphoneDevice = microphoneDevice;
    }

    public String outputDevice() {
        return outputDevice;
    }

    public void setOutputDevice(String outputDevice) {
        this.outputDevice = outputDevice;
    }

    public String pushToTalkKey() {
        return pushToTalkKey;
    }

    public void setPushToTalkKey(String pushToTalkKey) {
        this.pushToTalkKey = pushToTalkKey;
    }

    public float masterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(float masterVolume) {
        this.masterVolume = clampVolume(masterVolume);
    }

    public float voiceChatVolume() {
        return voiceChatVolume;
    }

    public void setVoiceChatVolume(float voiceChatVolume) {
        this.voiceChatVolume = clampVolume(voiceChatVolume);
    }

    public float microphoneVolume() {
        return microphoneVolume;
    }

    public void setMicrophoneVolume(float microphoneVolume) {
        this.microphoneVolume = clampVolume(microphoneVolume);
    }

    public VoiceActivationMode activationMode() {
        return activationMode;
    }

    public void setActivationMode(VoiceActivationMode activationMode) {
        this.activationMode = activationMode;
    }

    public float voiceActivationThreshold() {
        return voiceActivationThreshold;
    }

    public void setVoiceActivationThreshold(float voiceActivationThreshold) {
        this.voiceActivationThreshold = clampVolume(voiceActivationThreshold);
    }

    public boolean spatialAudioEnabled() {
        return spatialAudioEnabled;
    }

    public void setSpatialAudioEnabled(boolean spatialAudioEnabled) {
        this.spatialAudioEnabled = spatialAudioEnabled;
    }

    public boolean muted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean deafened() {
        return deafened;
    }

    public void setDeafened(boolean deafened) {
        this.deafened = deafened;
    }

    public boolean showDebugConnectionInfo() {
        return showDebugConnectionInfo;
    }

    public void setShowDebugConnectionInfo(boolean showDebugConnectionInfo) {
        this.showDebugConnectionInfo = showDebugConnectionInfo;
    }

    private static float clampVolume(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}

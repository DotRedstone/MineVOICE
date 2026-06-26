package dev.minevoice.neoforge.client.ui;

import dev.minevoice.neoforge.client.ClientAudioSettings;
import dev.minevoice.neoforge.client.DebugInfoLevel;
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
    private VoiceActivationMode groupActivationMode;
    private float groupVoiceActivationThreshold;
    private boolean spatialAudioEnabled;
    private String voiceCodec;
    private String audioPlaybackBackend;
    private boolean muted;
    private boolean deafened;
    private boolean hudEnabled;
    private boolean nameplateIconsEnabled;
    private int hudIconSize;
    private DebugInfoLevel debugInfoLevel;
    private boolean debugRenderRays;
    private int groupMemberColor;
    private int outOfSightIndicatorMode;
    private int occludedIndicatorMode;

    private MineVoiceSettingsScreenModel(ClientAudioSettings settings) {
        microphoneDevice = settings.microphoneDevice();
        outputDevice = settings.outputDevice();
        pushToTalkKey = settings.pushToTalkKey();
        masterVolume = settings.masterVolume();
        voiceChatVolume = settings.voiceChatVolume();
        microphoneVolume = settings.microphoneVolume();
        activationMode = settings.activationMode();
        voiceActivationThreshold = settings.voiceActivationThreshold();
        groupActivationMode = settings.groupActivationMode();
        groupVoiceActivationThreshold = settings.groupVoiceActivationThreshold();
        spatialAudioEnabled = settings.spatialAudioEnabled();
        voiceCodec = settings.voiceCodec();
        audioPlaybackBackend = settings.audioPlaybackBackend();
        muted = settings.muted();
        deafened = settings.deafened();
        hudEnabled = settings.hudEnabled();
        nameplateIconsEnabled = settings.nameplateIconsEnabled();
        hudIconSize = settings.hudIconSize();
        debugInfoLevel = settings.debugInfoLevel();
        debugRenderRays = settings.debugRenderRays();
        groupMemberColor = settings.groupMemberColor();
        outOfSightIndicatorMode = settings.outOfSightIndicatorMode();
        occludedIndicatorMode = settings.occludedIndicatorMode();
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
                groupActivationMode,
                groupVoiceActivationThreshold,
                spatialAudioEnabled,
                voiceCodec,
                audioPlaybackBackend,
                muted,
                deafened,
                hudEnabled,
                nameplateIconsEnabled,
                hudIconSize,
                debugInfoLevel,
                debugRenderRays,
                groupMemberColor,
                outOfSightIndicatorMode,
                occludedIndicatorMode
        );
    }

    public void resetToDefaults() {
        MineVoiceSettingsScreenModel defaults = from(ClientAudioSettings.defaults());
        microphoneDevice = defaults.microphoneDevice;
        outputDevice = defaults.outputDevice;
        pushToTalkKey = defaults.pushToTalkKey;
        masterVolume = defaults.masterVolume;
        voiceChatVolume = defaults.voiceChatVolume;
        microphoneVolume = defaults.microphoneVolume;
        activationMode = defaults.activationMode;
        voiceActivationThreshold = defaults.voiceActivationThreshold;
        groupActivationMode = defaults.groupActivationMode;
        groupVoiceActivationThreshold = defaults.groupVoiceActivationThreshold;
        spatialAudioEnabled = defaults.spatialAudioEnabled;
        voiceCodec = defaults.voiceCodec;
        audioPlaybackBackend = defaults.audioPlaybackBackend;
        muted = defaults.muted;
        deafened = defaults.deafened;
        hudEnabled = defaults.hudEnabled;
        nameplateIconsEnabled = defaults.nameplateIconsEnabled();
        hudIconSize = defaults.hudIconSize();
        debugInfoLevel = defaults.debugInfoLevel();
        debugRenderRays = defaults.debugRenderRays();
        groupMemberColor = defaults.groupMemberColor();
        outOfSightIndicatorMode = defaults.outOfSightIndicatorMode();
        occludedIndicatorMode = defaults.occludedIndicatorMode();
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

    public VoiceActivationMode groupActivationMode() {
        return groupActivationMode;
    }

    public void setGroupActivationMode(VoiceActivationMode groupActivationMode) {
        this.groupActivationMode = groupActivationMode;
    }

    public float groupVoiceActivationThreshold() {
        return groupVoiceActivationThreshold;
    }

    public void setGroupVoiceActivationThreshold(float groupVoiceActivationThreshold) {
        this.groupVoiceActivationThreshold = clampVolume(groupVoiceActivationThreshold);
    }

    public boolean spatialAudioEnabled() {
        return spatialAudioEnabled;
    }

    public void setSpatialAudioEnabled(boolean spatialAudioEnabled) {
        this.spatialAudioEnabled = spatialAudioEnabled;
    }

    public String audioPlaybackBackend() {
        return audioPlaybackBackend;
    }

    public void setAudioPlaybackBackend(String audioPlaybackBackend) {
        this.audioPlaybackBackend = audioPlaybackBackend;
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

    public boolean hudEnabled() {
        return hudEnabled;
    }

    public void setHudEnabled(boolean hudEnabled) {
        this.hudEnabled = hudEnabled;
    }
public boolean nameplateIconsEnabled() {
        return nameplateIconsEnabled;
    }

    public void setNameplateIconsEnabled(boolean nameplateIconsEnabled) {
        this.nameplateIconsEnabled = nameplateIconsEnabled;
    }
public int hudIconSize() {
        return hudIconSize;
    }

    public void setHudIconSize(int hudIconSize) {        this.hudIconSize = Math.max(12, Math.min(64, hudIconSize));
    }

    public DebugInfoLevel debugInfoLevel() {
        return debugInfoLevel;
    }

    public void setDebugInfoLevel(DebugInfoLevel debugInfoLevel) {
        this.debugInfoLevel = debugInfoLevel;
    }

    private static float clampVolume(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public boolean debugRenderRays() { return debugRenderRays; }
    public void setDebugRenderRays(boolean val) { this.debugRenderRays = val; }
    public int groupMemberColor() { return groupMemberColor; }
    public void setGroupMemberColor(int val) { this.groupMemberColor = val; }
    public int outOfSightIndicatorMode() { return outOfSightIndicatorMode; }
    public void setOutOfSightIndicatorMode(int val) { this.outOfSightIndicatorMode = val; }
    public int occludedIndicatorMode() { return occludedIndicatorMode; }
    public void setOccludedIndicatorMode(int val) { this.occludedIndicatorMode = val; }
}
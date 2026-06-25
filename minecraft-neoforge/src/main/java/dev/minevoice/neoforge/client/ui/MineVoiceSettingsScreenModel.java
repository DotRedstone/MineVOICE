package dev.minevoice.neoforge.client.ui;

import dev.minevoice.neoforge.client.ClientAudioSettings;
import dev.minevoice.neoforge.client.DebugInfoLevel;
import dev.minevoice.neoforge.client.HudAvatarAnchor;
import dev.minevoice.neoforge.client.VoiceActivationMode;
import dev.minevoice.neoforge.client.hud.MineVoiceHudStyle;

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
    private boolean muted;
    private boolean deafened;
    private boolean hudEnabled;
    private boolean speakerHudEnabled;
    private boolean groupHudEnabled;
    private boolean nameplateIconsEnabled;
    private HudAvatarAnchor hudAvatarAnchor;
    private int hudIconSize;
    private DebugInfoLevel debugInfoLevel;

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
        muted = settings.muted();
        deafened = settings.deafened();
        hudEnabled = settings.hudEnabled();
        speakerHudEnabled = settings.speakerHudEnabled();
        groupHudEnabled = settings.groupHudEnabled();
        nameplateIconsEnabled = settings.nameplateIconsEnabled();
        hudAvatarAnchor = settings.hudAvatarAnchor();
        hudIconSize = settings.hudIconSize();
        debugInfoLevel = settings.debugInfoLevel();
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
                muted,
                deafened,
                hudEnabled,
                speakerHudEnabled,
                groupHudEnabled,
                nameplateIconsEnabled,
                hudAvatarAnchor,
                hudIconSize,
                debugInfoLevel
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
        muted = defaults.muted;
        deafened = defaults.deafened;
        hudEnabled = defaults.hudEnabled;
        speakerHudEnabled = defaults.speakerHudEnabled;
        groupHudEnabled = defaults.groupHudEnabled;
        nameplateIconsEnabled = defaults.nameplateIconsEnabled;
        hudAvatarAnchor = defaults.hudAvatarAnchor;
        hudIconSize = defaults.hudIconSize;
        debugInfoLevel = defaults.debugInfoLevel;
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

    public void setHudIconSize(int hudIconSize) {
        this.hudIconSize = MineVoiceHudStyle.clampIconSize(hudIconSize);
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
}

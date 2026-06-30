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
        VoiceActivationMode groupActivationMode,
        float groupVoiceActivationThreshold,
        boolean spatialAudioEnabled,
        String voiceCodec,
        String audioPlaybackBackend,
        boolean muted,
        boolean deafened,
        boolean hudEnabled,
        boolean nameplateIconsEnabled,
        int hudIconSize,
        DebugInfoLevel debugInfoLevel,
        int debugRenderRaysMode,
        int groupMemberColor,
        int outOfSightIndicatorMode,
        int occludedIndicatorMode,
        boolean hrtfEnabled
) {
    public static ClientAudioSettings defaults() {
        return new ClientAudioSettings(
            null, null, null,
            1.0F, 1.0F, 1.0F,
            VoiceActivationMode.VOICE_ACTIVITY, 0.05F,
            VoiceActivationMode.PUSH_TO_TALK, 0.05F,
            true, null, null,
            false, false, true, true, 24, DebugInfoLevel.OFF, 0, 0xFFE040, 0, 0, true
        );
    }

    public ClientAudioSettings withMicrophoneDevice(String value) { return copy(value, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withOutputDevice(String value) { return copy(microphoneDevice, value, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withPushToTalkKey(String value) { return copy(microphoneDevice, outputDevice, value, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withMasterVolume(float value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, clampVolume(value), voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withVoiceChatVolume(float value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, clampVolume(value), microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withMicrophoneVolume(float value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, clampVolume(value), activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withActivationMode(VoiceActivationMode value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, value, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withVoiceActivationThreshold(float value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, value, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withGroupActivationMode(VoiceActivationMode value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, value, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withGroupVoiceActivationThreshold(float value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, value, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withSpatialAudioEnabled(boolean value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, value, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withVoiceCodec(String value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, value, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withAudioPlaybackBackend(String value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, value, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withMuted(boolean value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, value, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withDeafened(boolean value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, value, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withHudEnabled(boolean value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, value, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withNameplateIconsEnabled(boolean value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, value, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withHudIconSize(int value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, value, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withDebugInfoLevel(DebugInfoLevel value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, value, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public boolean showDebugConnectionInfo() { return debugInfoLevel() == DebugInfoLevel.BASIC || debugInfoLevel() == DebugInfoLevel.VERBOSE; }
    public ClientAudioSettings withShowDebugConnectionInfo(boolean value) { return withDebugInfoLevel(value ? DebugInfoLevel.BASIC : DebugInfoLevel.OFF); }
    public ClientAudioSettings withDebugRenderRaysMode(int value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, value, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withGroupMemberColor(int value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, value, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withOutOfSightIndicatorMode(int value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, value, occludedIndicatorMode, hrtfEnabled); }
    public ClientAudioSettings withOccludedIndicatorMode(int value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, value, hrtfEnabled); }
    public ClientAudioSettings withHrtfEnabled(boolean value) { return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume, activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold, spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, value); }

    private static ClientAudioSettings copy(
            String microphoneDevice, String outputDevice, String pushToTalkKey, float masterVolume, float voiceChatVolume, float microphoneVolume,
            VoiceActivationMode activationMode, float voiceActivationThreshold, VoiceActivationMode groupActivationMode, float groupVoiceActivationThreshold,
            boolean spatialAudioEnabled, String voiceCodec, String audioPlaybackBackend, boolean muted, boolean deafened, boolean hudEnabled,
            boolean nameplateIconsEnabled, int hudIconSize, DebugInfoLevel debugInfoLevel, int debugRenderRaysMode, int groupMemberColor, int outOfSightIndicatorMode, int occludedIndicatorMode, boolean hrtfEnabled) {
        return new ClientAudioSettings(
                microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled,
                nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRaysMode, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode, hrtfEnabled
        );
    }

    private static float clampVolume(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}

package dev.minevoice.neoforge.client;

import dev.minevoice.common.audio.VoiceCodecFactory;
import dev.minevoice.neoforge.client.audio.VoicePlaybackBackendFactory;
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
        boolean debugRenderRays,
        int groupMemberColor,
        int outOfSightIndicatorMode,
        int occludedIndicatorMode
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
                VoiceActivationMode.PUSH_TO_TALK,
                0.35F,
                true,
                "opus",
                "auto",
                false,
                false,
                true,
                true,
                16,
                DebugInfoLevel.OFF,
                false,
                0x55FF55,
                2,
                1
        );
    }

    public ClientAudioSettings {
        masterVolume = clampVolume(masterVolume);
        voiceChatVolume = clampVolume(voiceChatVolume);
        microphoneVolume = clampVolume(microphoneVolume);
        voiceActivationThreshold = clampVolume(voiceActivationThreshold);
        groupVoiceActivationThreshold = clampVolume(groupVoiceActivationThreshold);
        activationMode = activationMode == null ? VoiceActivationMode.PUSH_TO_TALK : activationMode;
        groupActivationMode = groupActivationMode == null ? VoiceActivationMode.PUSH_TO_TALK : groupActivationMode;
                hudIconSize = Math.max(12, Math.min(64, hudIconSize));
        debugInfoLevel = debugInfoLevel == null ? DebugInfoLevel.OFF : debugInfoLevel;
        voiceCodec = VoiceCodecFactory.normalizeCodecName(voiceCodec);
        audioPlaybackBackend = VoicePlaybackBackendFactory.normalizeBackendName(audioPlaybackBackend);
    }

    public boolean showDebugConnectionInfo() {
        return debugInfoLevel != DebugInfoLevel.OFF;
    }

    public ClientAudioSettings withMicrophoneDevice(String value) {
        return copy(value, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withOutputDevice(String value) {
        return copy(microphoneDevice, value, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withPushToTalkKey(String value) {
        return copy(microphoneDevice, outputDevice, value, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withMasterVolume(float value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, value, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withVoiceChatVolume(float value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, value, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withMicrophoneVolume(float value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, value,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withActivationMode(VoiceActivationMode value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                value, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withVoiceActivationThreshold(float value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, value, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withGroupActivationMode(VoiceActivationMode value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, value, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withGroupVoiceActivationThreshold(float value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, value,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withSpatialAudioEnabled(boolean value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                value, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withVoiceCodec(String value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, value, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withAudioPlaybackBackend(String value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, value, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withMuted(boolean value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, value, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withDeafened(boolean value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, value, hudEnabled, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withHudEnabled(boolean value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, value, nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }
    public ClientAudioSettings withNameplateIconsEnabled(boolean value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, value, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }
    public ClientAudioSettings withHudIconSize(int value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, value, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withDebugInfoLevel(DebugInfoLevel value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled, nameplateIconsEnabled, hudIconSize, value, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }

    public ClientAudioSettings withShowDebugConnectionInfo(boolean value) {
        return withDebugInfoLevel(value ? DebugInfoLevel.BASIC : DebugInfoLevel.OFF);
    }

    private static ClientAudioSettings copy(
            String microphoneDevice, String outputDevice, String pushToTalkKey, float masterVolume, float voiceChatVolume, float microphoneVolume,
            VoiceActivationMode activationMode, float voiceActivationThreshold, VoiceActivationMode groupActivationMode, float groupVoiceActivationThreshold,
            boolean spatialAudioEnabled, String voiceCodec, String audioPlaybackBackend, boolean muted, boolean deafened, boolean hudEnabled,
            boolean nameplateIconsEnabled, int hudIconSize, DebugInfoLevel debugInfoLevel, boolean debugRenderRays, int groupMemberColor, int outOfSightIndicatorMode, int occludedIndicatorMode) {
        return new ClientAudioSettings(
                microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled,
                nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode
        );
    }

    private static float clampVolume(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public ClientAudioSettings withDebugRenderRays(boolean value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled,
                nameplateIconsEnabled, hudIconSize, debugInfoLevel, value, groupMemberColor, outOfSightIndicatorMode, occludedIndicatorMode);
    }
    public ClientAudioSettings withGroupMemberColor(int value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled,
                nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, value, outOfSightIndicatorMode, occludedIndicatorMode);
    }
    public ClientAudioSettings withOutOfSightIndicatorMode(int value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled,
                nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, value, occludedIndicatorMode);
    }
    public ClientAudioSettings withOccludedIndicatorMode(int value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, audioPlaybackBackend, muted, deafened, hudEnabled,
                nameplateIconsEnabled, hudIconSize, debugInfoLevel, debugRenderRays, groupMemberColor, outOfSightIndicatorMode, value);
    }
}
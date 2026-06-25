package dev.minevoice.neoforge.client;

import dev.minevoice.neoforge.client.hud.MineVoiceHudStyle;

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
        boolean muted,
        boolean deafened,
        boolean hudEnabled,
        boolean speakerHudEnabled,
        boolean groupHudEnabled,
        boolean nameplateIconsEnabled,
        HudAvatarAnchor hudAvatarAnchor,
        int hudIconSize,
        DebugInfoLevel debugInfoLevel
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
                "mock",
                false,
                false,
                true,
                false,
                false,
                true,
                HudAvatarAnchor.BOTTOM_RIGHT,
                16,
                DebugInfoLevel.OFF
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
        hudAvatarAnchor = hudAvatarAnchor == null ? HudAvatarAnchor.BOTTOM_RIGHT : hudAvatarAnchor;
        hudIconSize = MineVoiceHudStyle.clampIconSize(hudIconSize);
        debugInfoLevel = debugInfoLevel == null ? DebugInfoLevel.OFF : debugInfoLevel;
    }

    public boolean showDebugConnectionInfo() {
        return debugInfoLevel != DebugInfoLevel.OFF;
    }

    public ClientAudioSettings withMicrophoneDevice(String value) {
        return copy(value, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withOutputDevice(String value) {
        return copy(microphoneDevice, value, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withPushToTalkKey(String value) {
        return copy(microphoneDevice, outputDevice, value, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withMasterVolume(float value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, value, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withVoiceChatVolume(float value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, value, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withMicrophoneVolume(float value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, value,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withActivationMode(VoiceActivationMode value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                value, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withVoiceActivationThreshold(float value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, value, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withGroupActivationMode(VoiceActivationMode value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, value, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withGroupVoiceActivationThreshold(float value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, value,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withSpatialAudioEnabled(boolean value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                value, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withVoiceCodec(String value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, value, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withMuted(boolean value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, value, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withDeafened(boolean value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, value, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withHudEnabled(boolean value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, value, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withSpeakerHudEnabled(boolean value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, value, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withGroupHudEnabled(boolean value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, value,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withNameplateIconsEnabled(boolean value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                value, hudAvatarAnchor, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withHudAvatarAnchor(HudAvatarAnchor value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, value, hudIconSize, debugInfoLevel);
    }

    public ClientAudioSettings withHudIconSize(int value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, value, debugInfoLevel);
    }

    public ClientAudioSettings withDebugInfoLevel(DebugInfoLevel value) {
        return copy(microphoneDevice, outputDevice, pushToTalkKey, masterVolume, voiceChatVolume, microphoneVolume,
                activationMode, voiceActivationThreshold, groupActivationMode, groupVoiceActivationThreshold,
                spatialAudioEnabled, voiceCodec, muted, deafened, hudEnabled, speakerHudEnabled, groupHudEnabled,
                nameplateIconsEnabled, hudAvatarAnchor, hudIconSize, value);
    }

    public ClientAudioSettings withShowDebugConnectionInfo(boolean value) {
        return withDebugInfoLevel(value ? DebugInfoLevel.BASIC : DebugInfoLevel.OFF);
    }

    private static ClientAudioSettings copy(
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
            boolean muted,
            boolean deafened,
            boolean hudEnabled,
            boolean speakerHudEnabled,
            boolean groupHudEnabled,
            boolean nameplateIconsEnabled,
            HudAvatarAnchor hudAvatarAnchor,
            int hudIconSize,
            DebugInfoLevel debugInfoLevel
    ) {
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

    private static float clampVolume(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}

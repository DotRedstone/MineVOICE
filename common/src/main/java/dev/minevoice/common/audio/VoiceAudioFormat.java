package dev.minevoice.common.audio;

public record VoiceAudioFormat(int sampleRate, int channels, int frameDurationMillis) {
    public static VoiceAudioFormat narrowbandVoice() {
        return new VoiceAudioFormat(48_000, 1, 20);
    }
}

package dev.minevoice.neoforge.client.audio;

public record VoicePlaybackStats(
        int activeSpeakers,
        int bufferedFrames,
        long latePackets,
        long droppedPackets,
        long missingFrames
) {
    public static VoicePlaybackStats empty() {
        return new VoicePlaybackStats(0, 0, 0L, 0L, 0L);
    }
}

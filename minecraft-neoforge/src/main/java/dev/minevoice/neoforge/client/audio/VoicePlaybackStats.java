package dev.minevoice.neoforge.client.audio;

public record VoicePlaybackStats(
        String backendName,
        int activeSpeakers,
        int bufferedFrames,
        long latePackets,
        long droppedPackets,
        long missingFrames
) {
    public static VoicePlaybackStats empty() {
        return new VoicePlaybackStats("none", 0, 0, 0L, 0L, 0L);
    }
}

package dev.minevoice.neoforge.client;

public record VoiceNetworkStats(
        VoiceConnectionStatus status,
        String endpoint,
        int protocolVersion,
        String codec,
        long connectedMillis,
        long udpPacketsSent,
        long udpPacketsReceived,
        long udpBytesSent,
        long udpBytesReceived,
        long voiceFramesSent,
        long voiceFramesReceived,
        long voicePayloadBytesSent,
        long voicePayloadBytesReceived
) {
    public double udpSentKiB() {
        return udpBytesSent / 1024.0D;
    }

    public double udpReceivedKiB() {
        return udpBytesReceived / 1024.0D;
    }

    public double voiceSentKiB() {
        return voicePayloadBytesSent / 1024.0D;
    }

    public double voiceReceivedKiB() {
        return voicePayloadBytesReceived / 1024.0D;
    }

    public double voiceFramesSentPerSecond() {
        return perSecond(voiceFramesSent);
    }

    public double voiceFramesReceivedPerSecond() {
        return perSecond(voiceFramesReceived);
    }

    private double perSecond(long count) {
        return connectedMillis <= 0L ? 0.0D : count * 1000.0D / connectedMillis;
    }
}

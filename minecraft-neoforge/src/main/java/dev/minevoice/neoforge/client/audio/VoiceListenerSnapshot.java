package dev.minevoice.neoforge.client.audio;

public record VoiceListenerSnapshot(
        double x,
        double y,
        double z,
        float yaw
) {
    public static VoiceListenerSnapshot unavailable() {
        return new VoiceListenerSnapshot(0.0D, 0.0D, 0.0D, 0.0F);
    }
}

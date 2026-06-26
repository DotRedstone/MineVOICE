package dev.minevoice.neoforge.client.audio;

public record VoiceListenerSnapshot(
        double x,
        double y,
        double z,
        float yaw,
        boolean environmentKnown,
        float reverbGain,
        float reverbDecaySeconds
) {
    public static VoiceListenerSnapshot unavailable() {
        return new VoiceListenerSnapshot(0.0D, 0.0D, 0.0D, 0.0F, false, 0.0F, 0.7F);
    }
}

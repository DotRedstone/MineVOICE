package dev.minevoice.neoforge.client.audio;

import dev.minevoice.common.protocol.VoiceChannel;

import java.util.Locale;
import java.util.UUID;

public record VoiceSpatialDebugSnapshot(
        UUID speakerId,
        VoiceChannel channel,
        double distance,
        double pan,
        float leftGain,
        float rightGain,
        boolean sourceKnown,
        boolean occlusionApplied,
        String backend
) {
    public static VoiceSpatialDebugSnapshot unavailable(String backend) {
        return new VoiceSpatialDebugSnapshot(null, VoiceChannel.PROXIMITY, -1.0D, 0.0D, 0.0F, 0.0F, false, false, backend);
    }

    public String summary() {
        String speaker = speakerId == null ? "none" : speakerId.toString().substring(0, 8);
        return String.format(
                Locale.ROOT,
                "backend:%s speaker:%s channel:%s known:%s distance:%.1f pan:%.2f gain:%.2f/%.2f occlusion:%s",
                backend,
                speaker,
                channel,
                sourceKnown,
                distance,
                pan,
                leftGain,
                rightGain,
                occlusionApplied
        );
    }
}

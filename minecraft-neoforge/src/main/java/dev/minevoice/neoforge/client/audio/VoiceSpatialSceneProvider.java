package dev.minevoice.neoforge.client.audio;

import java.util.UUID;

public interface VoiceSpatialSceneProvider {
    VoiceListenerSnapshot listenerSnapshot();

    VoiceSourceSnapshot sourceSnapshot(UUID speakerId);
}

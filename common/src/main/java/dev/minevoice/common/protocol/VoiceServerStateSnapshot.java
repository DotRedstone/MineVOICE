package dev.minevoice.common.protocol;

import java.util.List;

/**
 * Signed state snapshot sent by the Minecraft server to the voice relay.
 */
public record VoiceServerStateSnapshot(long generatedAtMillis, List<VoicePlayerState> players) {
    public VoiceServerStateSnapshot {
        players = List.copyOf(players);
    }
}

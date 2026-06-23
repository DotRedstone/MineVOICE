package dev.minevoice.common.spatial;

import dev.minevoice.common.protocol.VoiceFrame;
import dev.minevoice.common.protocol.VoicePlayerState;

/**
 * Resolves the authoritative player and block context needed by an audio algorithm.
 */
public interface VoiceEnvironmentProvider {
    /**
     * Builds the context for one sender, listener, and routed voice frame.
     */
    VoiceEnvironmentContext resolve(VoicePlayerState sender, VoicePlayerState listener, VoiceFrame frame);
}

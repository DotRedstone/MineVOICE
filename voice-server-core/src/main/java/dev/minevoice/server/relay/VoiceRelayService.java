package dev.minevoice.server.relay;

import dev.minevoice.common.protocol.VoiceFrame;
import dev.minevoice.common.protocol.VoiceChannel;
import dev.minevoice.common.protocol.VoicePlayerState;
import dev.minevoice.common.session.VoiceSession;
import dev.minevoice.common.session.VoiceSessionState;
import dev.minevoice.server.session.VoiceSessionRegistry;

import java.util.List;
import java.util.Objects;

/**
 * Handles routing of voice frames to nearby or group players.
 * <p>
 * O(K) spatial partition routing is enabled via Spatial Hash Grid in VoiceSessionRegistry.
 * </p>
 */
public final class VoiceRelayService {
    private final VoiceSessionRegistry sessionRegistry;
    private final double proximityDistanceSquared;

    public VoiceRelayService(VoiceSessionRegistry sessionRegistry, double proximityDistance) {
        this.sessionRegistry = sessionRegistry;
        if (proximityDistance <= 0.0D) {
            throw new IllegalArgumentException("proximity distance must be positive");
        }
        this.proximityDistanceSquared = proximityDistance * proximityDistance;
    }

    public List<VoiceSession> targetsFor(VoiceFrame frame) {
        VoicePlayerState sender = sessionRegistry.playerState(frame.senderPlayerId());
        if (sender == null || sender.muted()) {
            return List.of();
        }
        
        // Use O(K) spatial grid retrieval instead of O(N) activeSessions scan
        List<VoicePlayerState> candidatePlayers = sessionRegistry.getPlayersInGridCells(sender.dimensionId(), sender.x(), sender.z());
        
        return candidatePlayers.stream()
                .filter(recipient -> !recipient.playerId().equals(frame.senderPlayerId()))
                .filter(recipient -> canReceive(sender, recipient, frame.channel()))
                .map(recipient -> sessionRegistry.sessionByPlayerId(recipient.playerId()))
                .filter(Objects::nonNull)
                .filter(session -> session.state() == VoiceSessionState.CONNECTED || session.state() == VoiceSessionState.AUTHENTICATED)
                .toList();
    }

    public int relay(VoiceFrame frame) {
        return targetsFor(frame).size();
    }

    private boolean canReceive(VoicePlayerState sender, VoicePlayerState recipient, VoiceChannel channel) {
        if (sender.mutedPeers().contains(recipient.playerId()) || recipient.mutedPeers().contains(sender.playerId())) {
            return false;
        }
        if (channel == VoiceChannel.GROUP) {
            return sender.groupId() != null && sender.groupId().equals(recipient.groupId());
        }
        if (!sender.dimensionId().equals(recipient.dimensionId())) {
            return false;
        }
        double deltaX = sender.x() - recipient.x();
        double deltaY = sender.y() - recipient.y();
        double deltaZ = sender.z() - recipient.z();
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ <= proximityDistanceSquared;
    }
}

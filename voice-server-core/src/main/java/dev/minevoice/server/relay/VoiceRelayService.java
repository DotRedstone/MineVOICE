package dev.minevoice.server.relay;

import dev.minevoice.common.protocol.VoiceFrame;
import dev.minevoice.common.protocol.VoiceChannel;
import dev.minevoice.common.protocol.VoicePlayerState;
import dev.minevoice.common.session.VoiceSession;
import dev.minevoice.common.session.VoiceSessionState;
import dev.minevoice.server.session.VoiceSessionRegistry;

import java.util.List;

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
        return sessionRegistry.activeSessions().stream()
                .filter(session -> session.state() == VoiceSessionState.CONNECTED || session.state() == VoiceSessionState.AUTHENTICATED)
                .filter(session -> !session.playerInfo().playerUuid().equals(frame.senderPlayerId()))
                .filter(session -> canReceive(sender, session.playerInfo().playerUuid(), frame.channel()))
                .toList();
    }

    public int relay(VoiceFrame frame) {
        return targetsFor(frame).size();
    }

    private boolean canReceive(VoicePlayerState sender, java.util.UUID recipientId, VoiceChannel channel) {
        VoicePlayerState recipient = sessionRegistry.playerState(recipientId);
        if (recipient == null) {
            return false;
        }
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

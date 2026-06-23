package dev.minevoice.common.protocol;

import java.util.Objects;
import java.util.UUID;

/**
 * Server-authoritative player metadata used to route voice frames.
 */
public record VoicePlayerState(
        UUID playerId,
        String playerName,
        String dimensionId,
        double x,
        double y,
        double z,
        UUID groupId,
        String groupName,
        boolean muted
) {
    public VoicePlayerState {
        Objects.requireNonNull(playerId, "playerId");
        playerName = Objects.requireNonNullElse(playerName, "");
        dimensionId = Objects.requireNonNullElse(dimensionId, "");
        groupName = Objects.requireNonNullElse(groupName, "");
    }
}

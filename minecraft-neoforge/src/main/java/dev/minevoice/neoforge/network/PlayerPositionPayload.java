package dev.minevoice.neoforge.network;

import java.util.UUID;

public record PlayerPositionPayload(UUID playerId, double x, double y, double z) {
}

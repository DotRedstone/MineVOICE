package dev.minevoice.neoforge.network;

import java.util.UUID;

public record VoiceRosterEntry(
        UUID playerId,
        String playerName,
        UUID groupId,
        String groupName,
        boolean groupPasswordProtected,
        boolean muted,
        int groupColor
) {
}

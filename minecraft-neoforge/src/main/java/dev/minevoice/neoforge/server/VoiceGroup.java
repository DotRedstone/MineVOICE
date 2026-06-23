package dev.minevoice.neoforge.server;

import java.util.Set;
import java.util.UUID;

public record VoiceGroup(UUID id, String name, UUID ownerId, Set<UUID> members) {
    public VoiceGroup {
        members = Set.copyOf(members);
    }
}

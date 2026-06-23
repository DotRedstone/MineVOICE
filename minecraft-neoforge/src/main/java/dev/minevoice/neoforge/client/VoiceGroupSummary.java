package dev.minevoice.neoforge.client;

import java.util.UUID;

public record VoiceGroupSummary(UUID groupId, String groupName, int memberCount) {
}

package dev.minevoice.common.session;

import java.util.UUID;

public record VoicePlayerInfo(UUID playerUuid, String playerName, VoiceEndpoint endpoint) {
}

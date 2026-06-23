package dev.minevoice.neoforge.server;

import dev.minevoice.neoforge.network.VoiceServerInfoPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public final class VoiceServerInfoSender {
    public void sendToPlayer(UUID playerId, VoiceServerInfoPayload payload) {
        // TODO(minevoice): send mode, endpoint, token, and protocol version through NeoForge networking.
    }

    public void sendToPlayer(ServerPlayer player, VoiceServerInfoPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}

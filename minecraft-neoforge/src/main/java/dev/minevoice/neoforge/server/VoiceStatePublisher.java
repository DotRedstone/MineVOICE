package dev.minevoice.neoforge.server;

import dev.minevoice.common.protocol.BinaryVoicePacketCodec;
import dev.minevoice.common.protocol.VoicePacket;
import dev.minevoice.common.protocol.VoicePacketCodec;
import dev.minevoice.common.protocol.VoicePacketType;
import dev.minevoice.common.protocol.VoicePlayerState;
import dev.minevoice.common.protocol.VoiceProtocolVersion;
import dev.minevoice.common.protocol.VoiceServerStateCodec;
import dev.minevoice.common.protocol.VoiceServerStateSnapshot;
import dev.minevoice.neoforge.config.MineVoiceModConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class VoiceStatePublisher {
    private final VoicePacketCodec packetCodec = new BinaryVoicePacketCodec();
    private final AtomicLong sequence = new AtomicLong();

    public void publish(
            MinecraftServer server,
            MineVoiceModConfig config,
            VoiceGroupManager groupManager,
            PlayerVoiceStateManager playerVoiceStates
    ) {
        if (config.mode().name().equals("LOCAL") || config.remoteVoiceHost().isBlank()) {
            return;
        }
        VoiceServerStateSnapshot snapshot = snapshot(server, groupManager, playerVoiceStates);
        byte[] statePayload = VoiceServerStateCodec.encode(snapshot, config.sharedSecret());
        VoicePacket packet = new VoicePacket(
                VoicePacketType.SERVER_STATE,
                VoiceProtocolVersion.CURRENT,
                null,
                sequence.incrementAndGet(),
                System.currentTimeMillis(),
                statePayload
        );
        byte[] bytes = packetCodec.encode(packet);
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(config.remoteVoiceHost());
            socket.send(new DatagramPacket(bytes, bytes.length, address, config.remoteVoicePort()));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to publish MineVOICE player state", exception);
        }
    }

    public VoiceServerStateSnapshot snapshot(
            MinecraftServer server,
            VoiceGroupManager groupManager,
            PlayerVoiceStateManager playerVoiceStates
    ) {
        List<VoicePlayerState> players = server.getPlayerList().getPlayers().stream()
                .map(player -> stateFor(player, groupManager, playerVoiceStates))
                .toList();
        return new VoiceServerStateSnapshot(System.currentTimeMillis(), players);
    }

    private static VoicePlayerState stateFor(
            ServerPlayer player,
            VoiceGroupManager groupManager,
            PlayerVoiceStateManager playerVoiceStates
    ) {
        VoiceGroup group = groupManager.groupFor(player.getUUID());
        return new VoicePlayerState(
                player.getUUID(),
                player.getGameProfile().getName(),
                player.level().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                group == null ? null : group.id(),
                group == null ? "" : group.name(),
                playerVoiceStates.muted(player.getUUID()),
                playerVoiceStates.mutedPeers(player.getUUID())
        );
    }
}

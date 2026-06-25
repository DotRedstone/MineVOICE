package dev.minevoice.neoforge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record VoicePeerMutePayload(UUID playerId, boolean muted) implements CustomPacketPayload {
    public static final Type<VoicePeerMutePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("minevoice", "voice_peer_mute")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, VoicePeerMutePayload> STREAM_CODEC = StreamCodec.of(
            VoicePeerMutePayload::encode,
            VoicePeerMutePayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, VoicePeerMutePayload payload) {
        buffer.writeLong(payload.playerId().getMostSignificantBits());
        buffer.writeLong(payload.playerId().getLeastSignificantBits());
        buffer.writeBoolean(payload.muted());
    }

    private static VoicePeerMutePayload decode(RegistryFriendlyByteBuf buffer) {
        UUID playerId = new UUID(buffer.readLong(), buffer.readLong());
        return new VoicePeerMutePayload(playerId, buffer.readBoolean());
    }
}

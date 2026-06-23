package dev.minevoice.neoforge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VoicePlayerStatusPayload(boolean muted) implements CustomPacketPayload {
    public static final Type<VoicePlayerStatusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("minevoice", "voice_player_status")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, VoicePlayerStatusPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBoolean(payload.muted()),
            buffer -> new VoicePlayerStatusPayload(buffer.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

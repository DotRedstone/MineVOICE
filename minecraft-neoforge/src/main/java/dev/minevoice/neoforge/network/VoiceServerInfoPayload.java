package dev.minevoice.neoforge.network;

import dev.minevoice.common.config.VoiceMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VoiceServerInfoPayload(
        VoiceMode mode,
        String voiceHost,
        int voicePort,
        String token,
        int protocolVersion,
        String voiceCodec
) implements CustomPacketPayload {
    public static final Type<VoiceServerInfoPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("minevoice", "voice_server_info")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceServerInfoPayload> STREAM_CODEC = StreamCodec.of(
            VoiceServerInfoPayload::encode,
            VoiceServerInfoPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, VoiceServerInfoPayload payload) {
        buffer.writeEnum(payload.mode());
        buffer.writeUtf(payload.voiceHost());
        buffer.writeInt(payload.voicePort());
        buffer.writeUtf(payload.token(), 4096);
        buffer.writeInt(payload.protocolVersion());
        buffer.writeUtf(payload.voiceCodec(), 64);
    }

    private static VoiceServerInfoPayload decode(RegistryFriendlyByteBuf buffer) {
        return new VoiceServerInfoPayload(
                buffer.readEnum(VoiceMode.class),
                buffer.readUtf(),
                buffer.readInt(),
                buffer.readUtf(4096),
                buffer.readInt(),
                buffer.readUtf(64)
        );
    }
}

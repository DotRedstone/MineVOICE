package dev.minevoice.neoforge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record VoiceGroupActionPayload(
        VoiceGroupAction action,
        UUID groupId,
        String groupName,
        String password
) implements CustomPacketPayload {
    public static final Type<VoiceGroupActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("minevoice", "voice_group_action")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceGroupActionPayload> STREAM_CODEC = StreamCodec.of(
            VoiceGroupActionPayload::encode,
            VoiceGroupActionPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, VoiceGroupActionPayload payload) {
        buffer.writeEnum(payload.action());
        buffer.writeBoolean(payload.groupId() != null);
        if (payload.groupId() != null) {
            buffer.writeLong(payload.groupId().getMostSignificantBits());
            buffer.writeLong(payload.groupId().getLeastSignificantBits());
        }
        buffer.writeUtf(payload.groupName() == null ? "" : payload.groupName(), 64);
        buffer.writeUtf(payload.password() == null ? "" : payload.password(), 128);
    }

    private static VoiceGroupActionPayload decode(RegistryFriendlyByteBuf buffer) {
        VoiceGroupAction action = buffer.readEnum(VoiceGroupAction.class);
        UUID groupId = buffer.readBoolean() ? new UUID(buffer.readLong(), buffer.readLong()) : null;
        return new VoiceGroupActionPayload(action, groupId, buffer.readUtf(64), buffer.readUtf(128));
    }
}

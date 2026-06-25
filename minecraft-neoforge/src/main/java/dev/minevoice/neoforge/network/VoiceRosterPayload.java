package dev.minevoice.neoforge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record VoiceRosterPayload(List<VoiceRosterEntry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 2_048;
    public static final Type<VoiceRosterPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("minevoice", "voice_roster")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceRosterPayload> STREAM_CODEC = StreamCodec.of(
            VoiceRosterPayload::encode,
            VoiceRosterPayload::decode
    );

    public VoiceRosterPayload {
        entries = List.copyOf(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, VoiceRosterPayload payload) {
        if (payload.entries().size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("too many MineVOICE roster entries");
        }
        buffer.writeInt(payload.entries().size());
        for (VoiceRosterEntry entry : payload.entries()) {
            writeUuid(buffer, entry.playerId());
            buffer.writeUtf(entry.playerName(), 64);
            buffer.writeBoolean(entry.groupId() != null);
            if (entry.groupId() != null) {
                writeUuid(buffer, entry.groupId());
            }
            buffer.writeUtf(entry.groupName(), 64);
            buffer.writeBoolean(entry.groupPasswordProtected());
            buffer.writeBoolean(entry.muted());
        }
    }

    private static VoiceRosterPayload decode(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("invalid MineVOICE roster entry count: " + count);
        }
        List<VoiceRosterEntry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            UUID playerId = readUuid(buffer);
            String playerName = buffer.readUtf(64);
            UUID groupId = buffer.readBoolean() ? readUuid(buffer) : null;
            String groupName = buffer.readUtf(64);
            boolean groupPasswordProtected = buffer.readBoolean();
            boolean muted = buffer.readBoolean();
            entries.add(new VoiceRosterEntry(playerId, playerName, groupId, groupName, groupPasswordProtected, muted));
        }
        return new VoiceRosterPayload(entries);
    }

    private static void writeUuid(RegistryFriendlyByteBuf buffer, UUID value) {
        buffer.writeLong(value.getMostSignificantBits());
        buffer.writeLong(value.getLeastSignificantBits());
    }

    private static UUID readUuid(RegistryFriendlyByteBuf buffer) {
        return new UUID(buffer.readLong(), buffer.readLong());
    }
}

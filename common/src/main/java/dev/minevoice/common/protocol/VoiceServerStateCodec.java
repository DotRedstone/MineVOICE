package dev.minevoice.common.protocol;

import dev.minevoice.common.auth.HmacMessageSigner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Binary codec for authenticated Minecraft server state snapshots.
 */
public final class VoiceServerStateCodec {
    private static final int MAGIC = 0x4D565354;
    private static final int SIGNATURE_BYTES = 32;
    private static final int MAX_PLAYERS = 2_048;
    private static final int MAX_PLAYER_NAME_BYTES = 64;
    private static final int MAX_DIMENSION_BYTES = 256;
    private static final int MAX_GROUP_NAME_BYTES = 128;
    private static final int MAX_MUTED_PEERS = 2_048;

    private VoiceServerStateCodec() {
    }

    public static byte[] encode(VoiceServerStateSnapshot snapshot, String sharedSecret) {
        byte[] body = encodeBody(snapshot);
        byte[] signature = HmacMessageSigner.sign(body, sharedSecret);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + body.length + signature.length);
        buffer.putInt(MAGIC);
        buffer.put(body);
        buffer.put(signature);
        return buffer.array();
    }

    public static VoiceServerStateSnapshot decode(byte[] bytes, String sharedSecret) {
        if (bytes.length < Integer.BYTES + Long.BYTES + Integer.BYTES + SIGNATURE_BYTES) {
            throw new IllegalArgumentException("voice server state snapshot is too short");
        }
        ByteBuffer envelope = ByteBuffer.wrap(bytes);
        if (envelope.getInt() != MAGIC) {
            throw new IllegalArgumentException("invalid voice server state snapshot magic");
        }
        byte[] body = Arrays.copyOfRange(bytes, Integer.BYTES, bytes.length - SIGNATURE_BYTES);
        byte[] signature = Arrays.copyOfRange(bytes, bytes.length - SIGNATURE_BYTES, bytes.length);
        if (!HmacMessageSigner.matches(body, signature, sharedSecret)) {
            throw new IllegalArgumentException("voice server state signature mismatch");
        }
        return decodeBody(body);
    }

    private static byte[] encodeBody(VoiceServerStateSnapshot snapshot) {
        if (snapshot.players().size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("too many players in voice server state snapshot");
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(output);
            data.writeLong(snapshot.generatedAtMillis());
            data.writeInt(snapshot.players().size());
            for (VoicePlayerState player : snapshot.players()) {
                writeUuid(data, player.playerId());
                writeString(data, player.playerName(), MAX_PLAYER_NAME_BYTES);
                writeString(data, player.dimensionId(), MAX_DIMENSION_BYTES);
                data.writeDouble(player.x());
                data.writeDouble(player.y());
                data.writeDouble(player.z());
                data.writeBoolean(player.groupId() != null);
                if (player.groupId() != null) {
                    writeUuid(data, player.groupId());
                }
                writeString(data, player.groupName(), MAX_GROUP_NAME_BYTES);
                data.writeBoolean(player.muted());
                if (player.mutedPeers().size() > MAX_MUTED_PEERS) {
                    throw new IllegalArgumentException("too many muted peers in voice player state");
                }
                data.writeInt(player.mutedPeers().size());
                for (UUID mutedPeer : player.mutedPeers()) {
                    writeUuid(data, mutedPeer);
                }
            }
            data.flush();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to encode voice server state snapshot", exception);
        }
    }

    private static VoiceServerStateSnapshot decodeBody(byte[] body) {
        try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(body))) {
            long generatedAtMillis = data.readLong();
            int playerCount = data.readInt();
            if (playerCount < 0 || playerCount > MAX_PLAYERS) {
                throw new IllegalArgumentException("invalid voice server state player count: " + playerCount);
            }
            List<VoicePlayerState> players = new ArrayList<>(playerCount);
            for (int index = 0; index < playerCount; index++) {
                UUID playerId = readUuid(data);
                String playerName = readString(data, MAX_PLAYER_NAME_BYTES);
                String dimensionId = readString(data, MAX_DIMENSION_BYTES);
                double x = data.readDouble();
                double y = data.readDouble();
                double z = data.readDouble();
                UUID groupId = data.readBoolean() ? readUuid(data) : null;
                String groupName = readString(data, MAX_GROUP_NAME_BYTES);
                boolean muted = data.readBoolean();
                int mutedPeerCount = data.readInt();
                if (mutedPeerCount < 0 || mutedPeerCount > MAX_MUTED_PEERS) {
                    throw new IllegalArgumentException("invalid muted peer count: " + mutedPeerCount);
                }
                Set<UUID> mutedPeers = new LinkedHashSet<>();
                for (int peerIndex = 0; peerIndex < mutedPeerCount; peerIndex++) {
                    mutedPeers.add(readUuid(data));
                }
                players.add(new VoicePlayerState(playerId, playerName, dimensionId, x, y, z, groupId, groupName, muted, mutedPeers));
            }
            if (data.available() != 0) {
                throw new IllegalArgumentException("unexpected trailing bytes in voice server state snapshot");
            }
            return new VoiceServerStateSnapshot(generatedAtMillis, players);
        } catch (IOException exception) {
            throw new IllegalArgumentException("invalid voice server state snapshot", exception);
        }
    }

    private static void writeUuid(DataOutputStream data, UUID value) throws IOException {
        data.writeLong(value.getMostSignificantBits());
        data.writeLong(value.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream data) throws IOException {
        return new UUID(data.readLong(), data.readLong());
    }

    private static void writeString(DataOutputStream data, String value, int maxBytes) throws IOException {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        if (encoded.length > maxBytes) {
            throw new IllegalArgumentException("voice server state string exceeds " + maxBytes + " bytes");
        }
        data.writeInt(encoded.length);
        data.write(encoded);
    }

    private static String readString(DataInputStream data, int maxBytes) throws IOException {
        int length = data.readInt();
        if (length < 0 || length > maxBytes || length > data.available()) {
            throw new IllegalArgumentException("invalid voice server state string length: " + length);
        }
        byte[] value = data.readNBytes(length);
        return new String(value, StandardCharsets.UTF_8);
    }
}

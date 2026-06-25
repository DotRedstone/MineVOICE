package dev.minevoice.sim;

import dev.minevoice.common.config.VoiceConstants;
import dev.minevoice.common.protocol.BinaryVoicePacketCodec;
import dev.minevoice.common.protocol.VoicePacket;
import dev.minevoice.common.protocol.VoicePacketCodec;
import dev.minevoice.common.protocol.VoicePacketType;
import dev.minevoice.common.protocol.VoiceChannel;
import dev.minevoice.common.protocol.VoicePlayerState;
import dev.minevoice.common.protocol.VoiceProtocolVersion;
import dev.minevoice.common.protocol.VoiceServerStateCodec;
import dev.minevoice.common.protocol.VoiceServerStateSnapshot;
import dev.minevoice.common.session.VoiceEndpoint;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class FakeVoiceLoadTest {
    private FakeVoiceLoadTest() {
    }

    public static void main(String[] args) {
        int clients = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        String host = args.length > 1 ? args[1] : "127.0.0.1";
        int port = args.length > 2 ? Integer.parseInt(args[2]) : VoiceConstants.DEFAULT_UDP_PORT;
        String sharedSecret = args.length > 3 ? args[3] : "change-me";
        int framesPerClient = args.length > 4 ? Integer.parseInt(args[4]) : 3;
        double playerSpacing = args.length > 5 ? Double.parseDouble(args[5]) : 3.0D;
        VoiceChannel channel = args.length > 6 ? VoiceChannel.valueOf(args[6].toUpperCase()) : VoiceChannel.PROXIMITY;
        String codecName = args.length > 7 ? args[7] : "opus";
        VoiceEndpoint endpoint = new VoiceEndpoint(host, port);
        FakeVoiceFrameGenerator frameGenerator = new FakeVoiceFrameGenerator(codecName);
        List<FakeVoiceClient> fakeClients = new ArrayList<>();
        List<UUID> playerIds = new ArrayList<>();
        long startedAtNanos = System.nanoTime();

        for (int index = 0; index < clients; index++) {
            playerIds.add(UUID.randomUUID());
        }
        publishStateSnapshot(endpoint, sharedSecret, playerIds, playerSpacing, channel);

        for (int index = 0; index < clients; index++) {
            FakeVoiceClient client = new FakeVoiceClient(playerIds.get(index), endpoint);
            fakeClients.add(client);
            boolean authenticated = client.connect(sharedSecret);
            System.out.println("client " + index + " authenticated=" + authenticated);
            client.ping();
            for (int frameIndex = 0; frameIndex < framesPerClient; frameIndex++) {
                client.sendFrame(frameGenerator.generate(client.playerId(), frameIndex, channel));
            }
        }

        long sentBytes = fakeClients.stream().mapToLong(FakeVoiceClient::sentBytes).sum();
        fakeClients.forEach(FakeVoiceClient::drainForwardedFrames);
        long receivedBytes = fakeClients.stream().mapToLong(FakeVoiceClient::receivedBytes).sum();
        long packetsSent = fakeClients.stream().mapToLong(FakeVoiceClient::packetsSent).sum();
        long packetsReceived = fakeClients.stream().mapToLong(FakeVoiceClient::packetsReceived).sum();
        int voiceFramesSent = fakeClients.stream().mapToInt(FakeVoiceClient::voiceFramesSent).sum();
        int voiceFramesReceived = fakeClients.stream().mapToInt(FakeVoiceClient::voiceFramesReceived).sum();
        long voicePayloadBytesSent = fakeClients.stream().mapToLong(FakeVoiceClient::voicePayloadBytesSent).sum();
        long voicePayloadBytesReceived = fakeClients.stream().mapToLong(FakeVoiceClient::voicePayloadBytesReceived).sum();
        double elapsedSeconds = Math.max(0.001D, (System.nanoTime() - startedAtNanos) / 1_000_000_000.0D);
        fakeClients.forEach(FakeVoiceClient::close);
        System.out.println("fake clients=" + clients + " framesPerClient=" + framesPerClient);
        System.out.println("playerSpacing=" + playerSpacing + " channel=" + channel);
        System.out.println("codec=" + frameGenerator.codecName() + " pcmBytesPerFrame=" + frameGenerator.pcmBytesPerFrame());
        System.out.println("udpSentBytes=" + sentBytes + " udpReceivedBytes=" + receivedBytes
                + " packets=" + packetsSent + "/" + packetsReceived);
        System.out.println("voicePayloadSentBytes=" + voicePayloadBytesSent
                + " voicePayloadReceivedBytes=" + voicePayloadBytesReceived);
        System.out.println("voiceFrames=" + voiceFramesSent + "/" + voiceFramesReceived
                + " forwardedFrames=" + voiceFramesReceived
                + " fps=" + String.format(Locale.ROOT, "%.1f/%.1f", voiceFramesSent / elapsedSeconds, voiceFramesReceived / elapsedSeconds));
    }

    private static void publishStateSnapshot(
            VoiceEndpoint endpoint,
            String sharedSecret,
            List<UUID> playerIds,
            double playerSpacing,
            VoiceChannel channel
    ) {
        List<VoicePlayerState> players = new ArrayList<>();
        UUID groupId = channel == VoiceChannel.GROUP
                ? UUID.nameUUIDFromBytes("client-sim-group".getBytes(StandardCharsets.UTF_8))
                : null;
        for (int index = 0; index < playerIds.size(); index++) {
            players.add(new VoicePlayerState(
                    playerIds.get(index),
                    "Sim" + index,
                    "minecraft:overworld",
                    index * playerSpacing,
                    64.0D,
                    0.0D,
                    groupId,
                    groupId == null ? "" : "Client Sim Group",
                    false,
                    Set.of()
            ));
        }
        VoiceServerStateSnapshot snapshot = new VoiceServerStateSnapshot(System.currentTimeMillis(), players);
        VoicePacket packet = new VoicePacket(
                VoicePacketType.SERVER_STATE,
                VoiceProtocolVersion.CURRENT,
                null,
                1L,
                System.currentTimeMillis(),
                VoiceServerStateCodec.encode(snapshot, sharedSecret)
        );
        VoicePacketCodec codec = new BinaryVoicePacketCodec();
        byte[] bytes = codec.encode(packet);
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(endpoint.host());
            socket.send(new DatagramPacket(bytes, bytes.length, address, endpoint.port()));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to publish fake voice state snapshot", exception);
        }
    }
}

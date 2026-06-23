package dev.minevoice.sim;

import dev.minevoice.common.auth.AuthToken;
import dev.minevoice.common.auth.AuthTokenCodec;
import dev.minevoice.common.auth.HmacAuthTokenIssuer;
import dev.minevoice.common.network.BandwidthCounter;
import dev.minevoice.common.protocol.BinaryVoicePacketCodec;
import dev.minevoice.common.protocol.VoiceFrame;
import dev.minevoice.common.protocol.VoiceFramePayloadCodec;
import dev.minevoice.common.protocol.VoicePacket;
import dev.minevoice.common.protocol.VoicePacketCodec;
import dev.minevoice.common.protocol.VoicePacketType;
import dev.minevoice.common.protocol.VoiceProtocolVersion;
import dev.minevoice.common.session.VoiceEndpoint;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public final class FakeVoiceClient implements AutoCloseable {
    private final UUID playerId;
    private final VoiceEndpoint endpoint;
    private final DatagramSocket socket;
    private final VoicePacketCodec packetCodec = new BinaryVoicePacketCodec();
    private final BandwidthCounter bandwidthCounter = new BandwidthCounter();
    private long sequence;

    public FakeVoiceClient(UUID playerId, VoiceEndpoint endpoint) {
        this.playerId = playerId;
        this.endpoint = endpoint;
        try {
            this.socket = new DatagramSocket();
            this.socket.setSoTimeout(1_000);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to create fake voice client socket", exception);
        }
    }

    public boolean connect(String sharedSecret) {
        HmacAuthTokenIssuer issuer = new HmacAuthTokenIssuer(sharedSecret);
        AuthToken token = issuer.issue(playerId, "client-sim", Duration.ofMinutes(5));
        System.out.println("fake client connecting to " + endpoint.host() + ":" + endpoint.port() + " player=" + playerId);
        send(VoicePacketType.HELLO, new byte[0]);
        receive();
        send(VoicePacketType.AUTH, AuthTokenCodec.encodeToBytes(token));
        VoicePacket authResponse = receive();
        if (authResponse == null) {
            System.out.println("fake client auth response=<timeout>");
        } else if (authResponse.packetType() != VoicePacketType.AUTH_OK) {
            System.out.println("fake client auth response=" + authResponse.packetType()
                    + " reason=" + new String(authResponse.payload(), StandardCharsets.UTF_8));
        }
        return authResponse != null && authResponse.packetType() == VoicePacketType.AUTH_OK;
    }

    public UUID playerId() {
        return playerId;
    }

    public void ping() {
        send(VoicePacketType.PING, new byte[0]);
        receive();
    }

    public void sendFrame(VoiceFrame frame) {
        send(VoicePacketType.VOICE_FRAME, VoiceFramePayloadCodec.encode(frame));
        receive();
    }

    public long receivedBytes() {
        return bandwidthCounter.receivedBytes();
    }

    public long sentBytes() {
        return bandwidthCounter.sentBytes();
    }

    public int drainForwardedFrames() {
        int forwardedFrames = 0;
        try {
            socket.setSoTimeout(100);
        } catch (IOException exception) {
            return 0;
        }
        try {
            while (true) {
                VoicePacket packet = receive();
                if (packet == null) {
                    return forwardedFrames;
                }
                if (packet.packetType() == VoicePacketType.VOICE_FRAME && packet.payload().length > 0) {
                    forwardedFrames++;
                }
            }
        } finally {
            try {
                socket.setSoTimeout(1_000);
            } catch (IOException ignored) {
                // Socket shutdown is handled by close().
            }
        }
    }

    @Override
    public void close() {
        socket.close();
    }

    private void send(VoicePacketType type, byte[] payload) {
        try {
            VoicePacket packet = new VoicePacket(
                    type,
                    VoiceProtocolVersion.CURRENT,
                    playerId,
                    ++sequence,
                    System.currentTimeMillis(),
                    payload
            );
            byte[] bytes = packetCodec.encode(packet);
            DatagramPacket datagramPacket = new DatagramPacket(
                    bytes,
                    bytes.length,
                    InetAddress.getByName(endpoint.host()),
                    endpoint.port()
            );
            socket.send(datagramPacket);
            bandwidthCounter.recordSent(bytes.length);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to send fake voice packet", exception);
        }
    }

    private VoicePacket receive() {
        byte[] buffer = new byte[65_536];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(packet);
            bandwidthCounter.recordReceived(packet.getLength());
            byte[] bytes = Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getOffset() + packet.getLength());
            return packetCodec.decode(bytes);
        } catch (IOException exception) {
            return null;
        }
    }

}

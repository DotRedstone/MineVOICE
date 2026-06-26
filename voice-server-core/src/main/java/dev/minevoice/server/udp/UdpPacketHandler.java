package dev.minevoice.server.udp;

import dev.minevoice.common.auth.AuthToken;
import dev.minevoice.common.auth.AuthTokenCodec;
import dev.minevoice.common.auth.SessionKeyDeriver;
import dev.minevoice.common.auth.AesGcmCipher;

import dev.minevoice.common.auth.AuthTokenValidator;
import dev.minevoice.common.auth.TokenValidationResult;
import dev.minevoice.common.network.BandwidthCounter;
import dev.minevoice.common.protocol.BinaryVoicePacketCodec;
import dev.minevoice.common.protocol.VoicePacket;
import dev.minevoice.common.protocol.VoicePacketCodec;
import dev.minevoice.common.protocol.VoicePacketType;
import dev.minevoice.common.protocol.VoiceProtocolVersion;
import dev.minevoice.common.protocol.VoiceFrame;
import dev.minevoice.common.protocol.VoiceFramePayloadCodec;
import dev.minevoice.common.protocol.VoiceServerStateCodec;
import dev.minevoice.common.protocol.VoiceServerStateSnapshot;
import dev.minevoice.common.session.VoiceEndpoint;
import dev.minevoice.common.session.VoicePlayerInfo;
import dev.minevoice.common.session.VoiceSession;
import dev.minevoice.common.session.VoiceSessionState;
import dev.minevoice.common.util.MineVoiceLogger;
import dev.minevoice.server.relay.VoiceRelayService;
import dev.minevoice.server.session.VoiceSessionRegistry;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Instant;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

public final class UdpPacketHandler {
    private final MineVoiceLogger logger;
    private final VoicePacketCodec packetCodec = new BinaryVoicePacketCodec();
    private final AuthTokenValidator tokenValidator;
    private final VoiceSessionRegistry sessionRegistry;
    private final VoiceRelayService relayService;
    private final BandwidthCounter bandwidthCounter;
    private final String sharedSecret;

    // Rate limiting per IP
    private final java.util.concurrent.ConcurrentHashMap<String, TokenBucket> rateLimits = new java.util.concurrent.ConcurrentHashMap<>();

    private static class TokenBucket {
        private final int capacity;
        private final double refillRatePerMs;
        private double tokens;
        private long lastRefillTime;
        private long lastLogTime;

        TokenBucket(int capacity, int tokensPerSecond) {
            this.capacity = capacity;
            this.refillRatePerMs = tokensPerSecond / 1000.0;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
            this.lastLogTime = 0;
        }

        synchronized boolean tryConsume(int count) {
            long now = System.currentTimeMillis();
            double delta = (now - lastRefillTime) * refillRatePerMs;
            if (delta > 0) {
                tokens = Math.min(capacity, tokens + delta);
                lastRefillTime = now;
            }
            if (tokens >= count) {
                tokens -= count;
                return true;
            }
            return false;
        }

        synchronized boolean shouldLog(long cooldownMs) {
            long now = System.currentTimeMillis();
            if (now - lastLogTime >= cooldownMs) {
                lastLogTime = now;
                return true;
            }
            return false;
        }
    }


    public UdpPacketHandler(
            MineVoiceLogger logger,
            AuthTokenValidator tokenValidator,
            VoiceSessionRegistry sessionRegistry,
            VoiceRelayService relayService,
            BandwidthCounter bandwidthCounter,
            String sharedSecret
    ) {
        this.logger = logger;
        this.tokenValidator = tokenValidator;
        this.sessionRegistry = sessionRegistry;
        this.relayService = relayService;
        this.bandwidthCounter = bandwidthCounter;
        this.sharedSecret = sharedSecret;
    }

    public void handle(DatagramSocket socket, DatagramPacket packet) {
        String clientIp = packet.getAddress().getHostAddress();
        TokenBucket bucket = rateLimits.computeIfAbsent(clientIp, k -> new TokenBucket(150, 150));
        
        if (!bucket.tryConsume(1)) {
            if (bucket.shouldLog(5000)) {
                logger.debug("Rate limit exceeded for IP: " + clientIp + " (logs throttled for 5s)");
            }
            return;
        }

        sessionRegistry.removeExpired(Duration.ofMinutes(10));
        bandwidthCounter.recordReceived(packet.getLength());
        logger.debug("received UDP packet bytes=" + packet.getLength() + " from=" + packet.getSocketAddress());
        try {
            byte[] bytes = Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getOffset() + packet.getLength());
            VoicePacket voicePacket = packetCodec.decode(bytes);
            dispatch(socket, packet, voicePacket);
        } catch (RuntimeException exception) {
            logger.debug("ignored invalid UDP voice packet: " + exception.getMessage());
            send(socket, packet, VoicePacketType.ERROR, null, 0L, exception.getMessage().getBytes());
        }
    }

    private void dispatch(DatagramSocket socket, DatagramPacket source, VoicePacket packet) {
        switch (packet.packetType()) {
            case HELLO -> send(socket, source, VoicePacketType.PONG, packet.playerId(), packet.sequence(), new byte[0]);
            case AUTH -> handleAuth(socket, source, packet);
            case PING -> {
                if (isAuthenticatedSource(source, packet.playerId())) {
                    sessionRegistry.touch(packet.playerId());
                    send(socket, source, VoicePacketType.PONG, packet.playerId(), packet.sequence(), new byte[0]);
                } else {
                    send(socket, source, VoicePacketType.ERROR, packet.playerId(), packet.sequence(), "unauthenticated ping".getBytes());
                }
            }
            case VOICE_FRAME -> {
                VoiceFrame frame = VoiceFramePayloadCodec.decode(packet.payload());
                if (!frame.senderPlayerId().equals(packet.playerId()) || !isAuthenticatedSource(source, packet.playerId())) {
                    send(socket, source, VoicePacketType.ERROR, packet.playerId(), packet.sequence(), "unauthenticated voice frame".getBytes());
                    return;
                }
                sessionRegistry.touch(packet.playerId());
                int targets = relayFrame(socket, packet, frame);
                logger.debug("relayed voice frame targets=" + targets + " player=" + packet.playerId());
                send(socket, source, VoicePacketType.VOICE_FRAME, packet.playerId(), packet.sequence(), new byte[0]);
            }
            case SERVER_STATE -> handleServerState(packet);
            case DISCONNECT -> {
                if (isAuthenticatedSource(source, packet.playerId())) {
                    sessionRegistry.disconnect(packet.playerId());
                }
            }
            default -> logger.debug("packet type not handled yet: " + packet.packetType());
        }
    }

    private void handleServerState(VoicePacket packet) {
        VoiceServerStateSnapshot snapshot = VoiceServerStateCodec.decode(packet.payload(), sharedSecret);
        sessionRegistry.replacePlayerStates(snapshot.players());
        logger.debug("accepted server state players=" + snapshot.players().size());
    }

    private void handleAuth(DatagramSocket socket, DatagramPacket source, VoicePacket packet) {
        try {
            AuthToken token = AuthTokenCodec.decodeFromBytes(packet.payload());
            TokenValidationResult result = tokenValidator.validate(token);
            if (!result.valid()) {
                logger.warn("auth failed player=" + token.playerUuid() + " reason=" + result.reason());
                send(socket, source, VoicePacketType.AUTH_FAILED, token.playerUuid(), packet.sequence(), result.reason().getBytes());
                return;
            }
            if (!token.playerUuid().equals(packet.playerId())) {
                logger.warn("auth failed player=" + packet.playerId() + " reason=player identity mismatch tokenPlayer=" + token.playerUuid());
                send(socket, source, VoicePacketType.AUTH_FAILED, token.playerUuid(), packet.sequence(), "player identity mismatch".getBytes());
                return;
            }

            UUID sessionId = token.playerUuid();
            VoiceEndpoint endpoint = new VoiceEndpoint(source.getAddress().getHostAddress(), source.getPort());
            VoicePlayerInfo playerInfo = new VoicePlayerInfo(token.playerUuid(), token.playerUuid().toString(), endpoint);
            Instant now = Instant.now();
            sessionRegistry.register(new VoiceSession(sessionId, playerInfo, VoiceSessionState.CONNECTED, now, now));
            send(socket, source, VoicePacketType.AUTH_OK, token.playerUuid(), packet.sequence(), new byte[0]);
        } catch (RuntimeException exception) {
            logger.warn("auth failed player=" + packet.playerId() + " reason=malformed token: " + exception.getMessage());
            send(socket, source, VoicePacketType.AUTH_FAILED, packet.playerId(), packet.sequence(), exception.getMessage().getBytes());
        }
    }

    private boolean isAuthenticatedSource(DatagramPacket source, UUID playerId) {
        return playerId != null && sessionRegistry.matchesEndpoint(
                playerId,
                source.getAddress().getHostAddress(),
                source.getPort()
        );
    }

    private void send(DatagramSocket socket, DatagramPacket source, VoicePacketType type, UUID playerId, long sequence, byte[] payload) {
        try {
            VoicePacket response = new VoicePacket(
                    type,
                    VoiceProtocolVersion.CURRENT,
                    playerId,
                    sequence,
                    System.currentTimeMillis(),
                    payload
            );
            byte[] bytes = packetCodec.encode(response);
            DatagramPacket responsePacket = new DatagramPacket(bytes, bytes.length, source.getAddress(), source.getPort());
            socket.send(responsePacket);
            bandwidthCounter.recordSent(bytes.length);
        } catch (Exception exception) {
            logger.warn("failed to send UDP response: " + exception.getMessage());
        }
    }

    private int relayFrame(DatagramSocket socket, VoicePacket packet, VoiceFrame frame) {
        VoicePacket forwarded = new VoicePacket(
                VoicePacketType.VOICE_FRAME,
                packet.protocolVersion(),
                frame.senderPlayerId(),
                packet.sequence(),
                System.currentTimeMillis(),
                packet.payload()
        );
        byte[] bytes = packetCodec.encode(forwarded);
        int sent = 0;
        for (VoiceSession session : relayService.targetsFor(frame)) {
            try {
                VoiceEndpoint endpoint = session.playerInfo().endpoint();
                DatagramPacket datagramPacket = new DatagramPacket(
                        bytes,
                        bytes.length,
                        InetAddress.getByName(endpoint.host()),
                        endpoint.port()
                );
                socket.send(datagramPacket);
                bandwidthCounter.recordSent(bytes.length);
                sent++;
            } catch (Exception exception) {
                logger.debug("failed to relay voice frame: " + exception.getMessage());
            }
        }
        return sent;
    }
}

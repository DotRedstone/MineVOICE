package dev.minevoice.standalone.udp;

import dev.minevoice.common.util.MineVoiceLogger;
import dev.minevoice.common.auth.AuthTokenValidator;
import dev.minevoice.common.network.BandwidthCounter;
import dev.minevoice.standalone.relay.VoiceRelayService;
import dev.minevoice.standalone.config.StandaloneConfig;
import dev.minevoice.standalone.session.VoiceSessionRegistry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UdpVoiceServer {
    private final StandaloneConfig config;
    private final MineVoiceLogger logger;
    private final UdpPacketHandler packetHandler;
    private final AtomicBoolean running = new AtomicBoolean();
    private DatagramSocket socket;

    public UdpVoiceServer(
            StandaloneConfig config,
            MineVoiceLogger logger,
            AuthTokenValidator tokenValidator,
            VoiceSessionRegistry sessionRegistry,
            VoiceRelayService relayService,
            BandwidthCounter bandwidthCounter
    ) {
        this.config = config;
        this.logger = logger;
        this.packetHandler = new UdpPacketHandler(logger, tokenValidator, sessionRegistry, relayService, bandwidthCounter, config.sharedSecret());
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try (DatagramSocket openedSocket = new DatagramSocket(config.bindPort(), InetAddress.getByName(config.bindHost()))) {
            socket = openedSocket;
            logger.info("UDP voice server listening");
            byte[] buffer = new byte[65_536];
            while (running.get()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                openedSocket.receive(packet);
                packetHandler.handle(openedSocket, packet);
            }
        } catch (SocketException exception) {
            if (running.get()) {
                throw new IllegalStateException("failed to bind UDP voice server", exception);
            }
        } catch (IOException exception) {
            if (running.get()) {
                throw new IllegalStateException("UDP voice server stopped unexpectedly", exception);
            }
        } finally {
            running.set(false);
            socket = null;
        }
    }

    public void stop() {
        running.set(false);
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }
}

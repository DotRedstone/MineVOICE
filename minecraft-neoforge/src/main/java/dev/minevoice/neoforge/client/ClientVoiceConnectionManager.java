package dev.minevoice.neoforge.client;

import dev.minevoice.common.protocol.BinaryVoicePacketCodec;
import dev.minevoice.common.auth.AuthTokenCodec;
import dev.minevoice.common.protocol.VoiceFrame;
import dev.minevoice.common.protocol.VoiceFramePayloadCodec;
import dev.minevoice.common.protocol.VoicePacket;
import dev.minevoice.common.protocol.VoicePacketCodec;
import dev.minevoice.common.protocol.VoicePacketType;
import dev.minevoice.neoforge.client.audio.JavaSoundVoiceAudioPipeline;
import dev.minevoice.neoforge.client.audio.MinecraftVoiceSpatializer;
import dev.minevoice.neoforge.network.VoiceServerInfoPayload;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public final class ClientVoiceConnectionManager {
    private final VoicePacketCodec packetCodec = new BinaryVoicePacketCodec();
    private final ClientSettingsStore settingsStore;
    private final Function<UUID, Float> playerVolumeSupplier;
    private final Function<UUID, Boolean> playerMutedSupplier;
    private final VoiceHudState hudState = new VoiceHudState();
    private final VoiceSpeakerTracker speakerTracker = new VoiceSpeakerTracker();
    private final MinecraftVoiceSpatializer spatializer = new MinecraftVoiceSpatializer();
    private final AtomicBoolean running = new AtomicBoolean();
    private final Object audioPipelineLock = new Object();
    private volatile VoiceConnectionStatus status = VoiceConnectionStatus.DISCONNECTED;
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private int protocolVersion;
    private UUID playerId;
    private final AtomicLong sequence = new AtomicLong();
    private Thread receiveThread;
    private volatile JavaSoundVoiceAudioPipeline audioPipeline;

    public ClientVoiceConnectionManager(
            ClientSettingsStore settingsStore,
            Function<UUID, Float> playerVolumeSupplier,
            Function<UUID, Boolean> playerMutedSupplier
    ) {
        this.settingsStore = settingsStore;
        this.playerVolumeSupplier = playerVolumeSupplier;
        this.playerMutedSupplier = playerMutedSupplier;
    }

    public void connectFromServerPayload(VoiceServerInfoPayload payload) {
        setStatus(VoiceConnectionStatus.CONNECTING);
        try {
            disconnect();
            socket = new DatagramSocket();
            socket.setSoTimeout(1_000);
            serverAddress = InetAddress.getByName(payload.voiceHost());
            serverPort = payload.voicePort();
            protocolVersion = payload.protocolVersion();
            playerId = AuthTokenCodec.decodeFromString(payload.token()).playerUuid();
            send(VoicePacketType.HELLO, new byte[0]);
            receive();
            send(VoicePacketType.AUTH, payload.token().getBytes(StandardCharsets.UTF_8));
            VoicePacket response = receive();
            if (response == null || response.packetType() != VoicePacketType.AUTH_OK) {
                setStatus(VoiceConnectionStatus.AUTH_FAILED);
                closeSocket();
                return;
            }

            running.set(true);
            setStatus(VoiceConnectionStatus.CONNECTED);
            resumeAudioAfterDeviceTest();
            receiveThread = new Thread(this::receiveLoop, "minevoice-udp-receive");
            receiveThread.setDaemon(true);
            receiveThread.start();
        } catch (IOException exception) {
            setStatus(VoiceConnectionStatus.ERROR);
            disconnect();
        }
    }

    public VoiceConnectionStatus status() {
        return status;
    }

    public VoiceHudState hudState() {
        return hudState;
    }

    public VoiceSpeakerTracker speakerTracker() {
        return speakerTracker;
    }

    public void setPushToTalkDown(boolean pressed) {
        hudState.setPushToTalkDown(pressed);
        JavaSoundVoiceAudioPipeline pipeline = audioPipeline;
        if (pipeline != null) {
            pipeline.setPushToTalkDown(pressed);
        }
    }

    public void refreshSpatialState() {
        spatializer.refresh();
    }

    public void setGroupPushToTalkDown(boolean pressed) {
        if (pressed) {
            hudState.setActiveChannel(dev.minevoice.common.protocol.VoiceChannel.GROUP);
        } else if (!hudState.pushToTalkDown()) {
            hudState.setActiveChannel(dev.minevoice.common.protocol.VoiceChannel.PROXIMITY);
        }
        JavaSoundVoiceAudioPipeline pipeline = audioPipeline;
        if (pipeline != null) {
            pipeline.setGroupPushToTalkDown(pressed);
        }
    }

    public void suspendAudioForDeviceTest() {
        synchronized (audioPipelineLock) {
            stopAudioPipeline();
        }
    }

    public void resumeAudioAfterDeviceTest() {
        synchronized (audioPipelineLock) {
            if (!running.get() || audioPipeline != null || playerId == null) {
                return;
            }
            audioPipeline = new JavaSoundVoiceAudioPipeline(
                    playerId,
                    settingsStore::load,
                    this::sendFrame,
                    hudState::setMicrophoneActivity,
                    spatializer,
                    playerVolumeSupplier,
                    playerMutedSupplier
            );
            audioPipeline.start();
        }
    }

    public void disconnect() {
        running.set(false);
        synchronized (audioPipelineLock) {
            stopAudioPipeline();
        }
        closeSocket();
        setStatus(VoiceConnectionStatus.DISCONNECTED);
    }

    private void sendFrame(VoiceFrame frame) {
        try {
            send(VoicePacketType.VOICE_FRAME, VoiceFramePayloadCodec.encode(frame));
        } catch (IOException exception) {
            setStatus(VoiceConnectionStatus.ERROR);
        }
    }

    private void receiveLoop() {
        while (running.get()) {
            try {
                VoicePacket packet = receive();
                if (packet == null) {
                    continue;
                }
                JavaSoundVoiceAudioPipeline pipeline = audioPipeline;
                if (packet.packetType() == VoicePacketType.VOICE_FRAME && pipeline != null && packet.payload().length > 0) {
                    VoiceFrame frame = VoiceFramePayloadCodec.decode(packet.payload());
                    speakerTracker.markSpeaking(frame.senderPlayerId());
                    pipeline.enqueuePlayback(frame);
                }
                if (packet.packetType() == VoicePacketType.AUTH_FAILED || packet.packetType() == VoicePacketType.DISCONNECT) {
                    setStatus(VoiceConnectionStatus.AUTH_FAILED);
                    running.set(false);
                }
            } catch (IOException exception) {
                if (exception instanceof SocketTimeoutException) {
                    continue;
                }
                if (running.get()) {
                    setStatus(VoiceConnectionStatus.ERROR);
                }
            }
        }
    }

    private void send(VoicePacketType type, byte[] payload) throws IOException {
        VoicePacket packet = new VoicePacket(
                type,
                protocolVersion,
                playerId,
                sequence.incrementAndGet(),
                System.currentTimeMillis(),
                payload
        );
        byte[] bytes = packetCodec.encode(packet);
        socket.send(new DatagramPacket(bytes, bytes.length, serverAddress, serverPort));
    }

    private VoicePacket receive() throws IOException {
        byte[] buffer = new byte[65_536];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        byte[] bytes = Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getOffset() + packet.getLength());
        return packetCodec.decode(bytes);
    }

    private void closeSocket() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    private void stopAudioPipeline() {
        JavaSoundVoiceAudioPipeline pipeline = audioPipeline;
        audioPipeline = null;
        if (pipeline != null) {
            pipeline.stop();
        }
    }

    private void setStatus(VoiceConnectionStatus status) {
        this.status = status;
        hudState.setConnectionStatus(status);
    }
}

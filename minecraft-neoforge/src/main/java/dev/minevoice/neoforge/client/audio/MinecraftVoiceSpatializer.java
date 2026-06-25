package dev.minevoice.neoforge.client.audio;

import dev.minevoice.common.protocol.VoiceChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Captures Minecraft entity positions on the client thread for use by the audio thread.
 */
public final class MinecraftVoiceSpatializer implements VoiceSpatializer {
    private static final double FULL_VOLUME_DISTANCE = 4.0D;
    private static final double MAX_PROXIMITY_DISTANCE = 48.0D;
    private static final String BACKEND_NAME = "java-sound-pan";

    private final ConcurrentMap<UUID, SourceSnapshot> sources = new ConcurrentHashMap<>();
    private volatile ListenerSnapshot listener;
    private volatile VoiceSpatialDebugSnapshot debugSnapshot = VoiceSpatialDebugSnapshot.unavailable(BACKEND_NAME);

    public void refresh() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null || minecraft.level == null) {
            listener = null;
            sources.clear();
            return;
        }

        listener = new ListenerSnapshot(
                localPlayer.getX(),
                localPlayer.getEyeY(),
                localPlayer.getZ(),
                minecraft.gameRenderer.getMainCamera().getYRot()
        );
        Set<UUID> visiblePlayers = new HashSet<>();
        for (Player player : minecraft.level.players()) {
            if (player.getUUID().equals(localPlayer.getUUID())) {
                continue;
            }
            UUID playerId = player.getUUID();
            visiblePlayers.add(playerId);
            sources.put(playerId, new SourceSnapshot(player.getX(), player.getEyeY(), player.getZ()));
        }
        sources.keySet().retainAll(visiblePlayers);
    }

    @Override
    public StereoGains gainsFor(UUID speakerId, VoiceChannel channel) {
        if (channel != VoiceChannel.PROXIMITY) {
            debugSnapshot = new VoiceSpatialDebugSnapshot(
                    speakerId,
                    channel,
                    -1.0D,
                    0.0D,
                    StereoGains.CENTER.left(),
                    StereoGains.CENTER.right(),
                    false,
                    false,
                    BACKEND_NAME
            );
            return StereoGains.CENTER;
        }
        ListenerSnapshot currentListener = listener;
        SourceSnapshot source = sources.get(speakerId);
        if (currentListener == null || source == null) {
            debugSnapshot = new VoiceSpatialDebugSnapshot(
                    speakerId,
                    channel,
                    -1.0D,
                    0.0D,
                    StereoGains.CENTER.left(),
                    StereoGains.CENTER.right(),
                    false,
                    false,
                    BACKEND_NAME
            );
            return StereoGains.CENTER;
        }

        double deltaX = source.x() - currentListener.x();
        double deltaY = source.y() - currentListener.y();
        double deltaZ = source.z() - currentListener.z();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        if (distance < 0.0001D) {
            debugSnapshot = new VoiceSpatialDebugSnapshot(
                    speakerId,
                    channel,
                    0.0D,
                    0.0D,
                    StereoGains.CENTER.left(),
                    StereoGains.CENTER.right(),
                    true,
                    false,
                    BACKEND_NAME
            );
            return StereoGains.CENTER;
        }

        float volume = volumeAt(distance);
        if (volume <= 0.0F) {
            debugSnapshot = new VoiceSpatialDebugSnapshot(
                    speakerId,
                    channel,
                    distance,
                    0.0D,
                    0.0F,
                    0.0F,
                    true,
                    false,
                    BACKEND_NAME
            );
            return new StereoGains(0.0F, 0.0F);
        }

        double yaw = Math.toRadians(currentListener.yaw());
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double pan = horizontalDistance < 0.0001D
                ? 0.0D
                : clamp((deltaX * rightX + deltaZ * rightZ) / horizontalDistance, -1.0D, 1.0D);
        double angle = (pan + 1.0D) * Math.PI / 4.0D;
        StereoGains gains = new StereoGains((float) (volume * Math.cos(angle)), (float) (volume * Math.sin(angle)));
        debugSnapshot = new VoiceSpatialDebugSnapshot(
                speakerId,
                channel,
                distance,
                pan,
                gains.left(),
                gains.right(),
                true,
                false,
                BACKEND_NAME
        );
        return gains;
    }

    public VoiceSpatialDebugSnapshot debugSnapshot() {
        return debugSnapshot;
    }

    private static float volumeAt(double distance) {
        if (distance <= FULL_VOLUME_DISTANCE) {
            return 1.0F;
        }
        if (distance >= MAX_PROXIMITY_DISTANCE) {
            return 0.0F;
        }
        return (float) ((MAX_PROXIMITY_DISTANCE - distance) / (MAX_PROXIMITY_DISTANCE - FULL_VOLUME_DISTANCE));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ListenerSnapshot(double x, double y, double z, float yaw) {
    }

    private record SourceSnapshot(double x, double y, double z) {
    }
}

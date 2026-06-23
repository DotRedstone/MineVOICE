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

    private final ConcurrentMap<UUID, SourceSnapshot> sources = new ConcurrentHashMap<>();
    private volatile ListenerSnapshot listener;

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
            return StereoGains.CENTER;
        }
        ListenerSnapshot currentListener = listener;
        SourceSnapshot source = sources.get(speakerId);
        if (currentListener == null || source == null) {
            return StereoGains.CENTER;
        }

        double deltaX = source.x() - currentListener.x();
        double deltaY = source.y() - currentListener.y();
        double deltaZ = source.z() - currentListener.z();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        if (distance < 0.0001D) {
            return StereoGains.CENTER;
        }

        float volume = volumeAt(distance);
        if (volume <= 0.0F) {
            return new StereoGains(0.0F, 0.0F);
        }

        double yaw = Math.toRadians(currentListener.yaw());
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double pan = clamp((deltaX * rightX + deltaZ * rightZ) / distance, -1.0D, 1.0D);
        double angle = (pan + 1.0D) * Math.PI / 4.0D;
        return new StereoGains((float) (volume * Math.cos(angle)), (float) (volume * Math.sin(angle)));
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

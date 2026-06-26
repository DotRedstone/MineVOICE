package dev.minevoice.neoforge.client.audio;

import dev.minevoice.common.protocol.VoiceChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Captures listener-relative acoustic snapshots on the client thread for the audio thread.
 */
public final class MinecraftVoiceSpatializer implements VoiceSpatializer, VoiceSpatialSceneProvider {
    private static final double FULL_VOLUME_DISTANCE = 4.0D;
    private static final double MAX_PROXIMITY_DISTANCE = 48.0D;
    private static final double SOURCE_POSITION_EPSILON_SQUARED = 0.0625D;
    private static final int MAX_REFLECTION_PROBES_PER_SOURCE = 8;
    private static final String BACKEND_NAME = "material-raytrace";

    private final AcousticMaterialConfig acousticConfig = new AcousticMaterialConfig(
            FMLPaths.CONFIGDIR.get().resolve(AcousticMaterialConfig.FILE_NAME)
    );
    private final ConcurrentMap<UUID, SourceSnapshot> sources = new ConcurrentHashMap<>();
    private volatile ListenerSnapshot listener;
    private volatile List<ReflectionProbe> reflectionProbes = List.of();
    private volatile VoiceSpatialDebugSnapshot debugSnapshot = VoiceSpatialDebugSnapshot.unavailable(BACKEND_NAME);
    private volatile AcousticDebugSnapshot acousticDebugSnapshot = AcousticDebugSnapshot.DISABLED;
    private long lastEnvironmentRefreshMillis;
    private long activeConfigRevision = -1L;

    public void refresh() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        ClientLevel level = minecraft.level;
        if (localPlayer == null || level == null) {
            listener = null;
            reflectionProbes = List.of();
            sources.clear();
            acousticDebugSnapshot = AcousticDebugSnapshot.DISABLED;
            return;
        }

        long now = System.currentTimeMillis();
        AcousticMaterialConfig.Snapshot config = acousticConfig.snapshot();
        Vec3 listenerPosition = new Vec3(localPlayer.getX(), localPlayer.getEyeY(), localPlayer.getZ());
        boolean configChanged = activeConfigRevision != config.revision();
        boolean refreshEnvironment = configChanged
                || now - lastEnvironmentRefreshMillis >= config.environmentRefreshIntervalMillis();
        if (refreshEnvironment) {
            reflectionProbes = config.enabled()
                    ? sampleReflectionProbes(level, localPlayer, listenerPosition, config)
                    : List.of();
            lastEnvironmentRefreshMillis = now;
            activeConfigRevision = config.revision();
        }
        AcousticEnvironment environment = environmentFor(reflectionProbes, config);
        listener = new ListenerSnapshot(listenerPosition, minecraft.gameRenderer.getMainCamera().getYRot(), environment);

        Set<UUID> visiblePlayers = new HashSet<>();
        for (Player player : level.players()) {
            if (player.getUUID().equals(localPlayer.getUUID())) {
                continue;
            }
            UUID playerId = player.getUUID();
            visiblePlayers.add(playerId);
            Vec3 sourcePosition = new Vec3(player.getX(), player.getEyeY(), player.getZ());
            float yaw = player.getYHeadRot();
            float pitch = player.getXRot();
            SourceSnapshot previous = sources.get(playerId);
            if (shouldRefreshSource(previous, sourcePosition, listenerPosition, config, now, configChanged)) {
                AcousticPath path = config.enabled()
                        ? analyzePath(level, localPlayer, listenerPosition, sourcePosition, config, reflectionProbes)
                        : AcousticPath.clear(sourcePosition, listenerPosition.distanceTo(sourcePosition));
                sources.put(playerId, new SourceSnapshot(sourcePosition, yaw, pitch, listenerPosition, path, now, config.revision()));
            }
        }
        sources.keySet().retainAll(visiblePlayers);
        acousticDebugSnapshot = buildAcousticDebugSnapshot(listenerPosition);
    }

    @Override
    public StereoGains gainsFor(UUID speakerId, VoiceChannel channel) {
        if (channel != VoiceChannel.PROXIMITY) {
            debugSnapshot = debugSnapshot(speakerId, channel, -1.0D, 0.0D, StereoGains.CENTER, null);
            return StereoGains.CENTER;
        }
        ListenerSnapshot currentListener = listener;
        SourceSnapshot source = sources.get(speakerId);
        if (currentListener == null || source == null) {
            debugSnapshot = debugSnapshot(speakerId, channel, -1.0D, 0.0D, StereoGains.CENTER, null);
            return StereoGains.CENTER;
        }

        AcousticPath path = source.path();
        double deltaX = path.virtualPosition().x - currentListener.position().x;
        double deltaZ = path.virtualPosition().z - currentListener.position().z;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double yaw = Math.toRadians(currentListener.yaw());
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double pan = horizontalDistance < 0.0001D
                ? 0.0D
                : clamp((deltaX * rightX + deltaZ * rightZ) / horizontalDistance, -1.0D, 1.0D);
        double angle = (pan + 1.0D) * Math.PI / 4.0D;
        float gain = path.directGain();
        StereoGains gains = new StereoGains((float) (gain * Math.cos(angle)), (float) (gain * Math.sin(angle)));
        debugSnapshot = debugSnapshot(speakerId, channel, source.position().distanceTo(currentListener.position()), pan, gains, path);
        return gains;
    }

    @Override
    public boolean occluded(UUID speakerId, VoiceChannel channel) {
        if (channel != VoiceChannel.PROXIMITY) {
            return false;
        }
        SourceSnapshot source = sources.get(speakerId);
        return source != null && source.path().occluded();
    }

    @Override
    public VoiceListenerSnapshot listenerSnapshot() {
        ListenerSnapshot currentListener = listener;
        if (currentListener == null) {
            return VoiceListenerSnapshot.unavailable();
        }
        return new VoiceListenerSnapshot(
                currentListener.position().x,
                currentListener.position().y,
                currentListener.position().z,
                currentListener.yaw(),
                currentListener.environment().known(),
                currentListener.environment().reverbGain(),
                currentListener.environment().reverbDecaySeconds()
        );
    }

    @Override
    public VoiceSourceSnapshot sourceSnapshot(UUID speakerId) {
        SourceSnapshot source = sources.get(speakerId);
        if (source == null) {
            return VoiceSourceSnapshot.unknown(speakerId);
        }
        AcousticPath path = source.path();
        return new VoiceSourceSnapshot(
                speakerId,
                path.virtualPosition().x,
                path.virtualPosition().y,
                path.virtualPosition().z,
                source.yaw(),
                source.pitch(),
                true,
                path.occluded(),
                path.directGain(),
                path.highFrequencyGain(),
                path.reflectionGain(),
                path.reflectionProbeCount()
        );
    }

    public VoiceSpatialDebugSnapshot debugSnapshot() {
        return debugSnapshot;
    }

    public AcousticDebugSnapshot acousticDebugSnapshot() {
        return acousticDebugSnapshot;
    }

    private static boolean shouldRefreshSource(
            SourceSnapshot previous,
            Vec3 sourcePosition,
            Vec3 listenerPosition,
            AcousticMaterialConfig.Snapshot config,
            long now,
            boolean configChanged
    ) {
        return previous == null
                || configChanged
                || previous.configRevision() != config.revision()
                || now - previous.computedAtMillis() >= config.sourceRefreshIntervalMillis()
                || previous.position().distanceToSqr(sourcePosition) > SOURCE_POSITION_EPSILON_SQUARED
                || previous.listenerPosition().distanceToSqr(listenerPosition) > SOURCE_POSITION_EPSILON_SQUARED;
    }

    private static AcousticPath analyzePath(
            ClientLevel level,
            LocalPlayer listener,
            Vec3 listenerPosition,
            Vec3 sourcePosition,
            AcousticMaterialConfig.Snapshot config,
            List<ReflectionProbe> probes
    ) {
        double distance = listenerPosition.distanceTo(sourcePosition);
        DirectPath direct = traceDirectPath(level, listener, listenerPosition, sourcePosition, config);
        ReflectionPath reflection = traceFirstOrderReflection(level, listener, listenerPosition, sourcePosition, probes, config);
        return new AcousticPath(
                sourcePosition,
                volumeAt(distance) * direct.transmissionGain(),
                direct.highFrequencyGain(),
                reflection.gain(),
                direct.occluded(),
                reflection.probeCount(),
                reflection.usedPositions()
        );
    }

    private static DirectPath traceDirectPath(
            ClientLevel level,
            LocalPlayer listener,
            Vec3 listenerPosition,
            Vec3 sourcePosition,
            AcousticMaterialConfig.Snapshot config
    ) {
        double distance = listenerPosition.distanceTo(sourcePosition);
        int samples = Math.min(config.maxOcclusionSamples(), Math.max(1, (int) Math.ceil(distance * 4.0D)));
        Set<Long> visitedBlocks = new HashSet<>();
        float transmission = 1.0F;
        float highFrequency = 1.0F;
        boolean occluded = false;
        double rayThickness = 0.25D;
        
        for (int sample = 1; sample < samples; sample++) {
            Vec3 point = listenerPosition.lerp(sourcePosition, sample / (double) samples);
            int minX = (int) Math.floor(point.x - rayThickness);
            int maxX = (int) Math.floor(point.x + rayThickness);
            int minY = (int) Math.floor(point.y - rayThickness);
            int maxY = (int) Math.floor(point.y + rayThickness);
            int minZ = (int) Math.floor(point.z - rayThickness);
            int maxZ = (int) Math.floor(point.z + rayThickness);
            
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        if (!visitedBlocks.add(blockPos.asLong())) {
                            continue;
                        }
                        BlockState blockState = level.getBlockState(blockPos);
                        if (!isAcousticObstacle(level, blockPos, blockState)) {
                            continue;
                        }
                        AcousticMaterialConfig.Material material = materialFor(level, blockPos, blockState, config);
                        transmission *= material.transmissionGain();
                        highFrequency *= material.highFrequencyGain();
                        occluded = true;
                    }
                }
            }
            if (transmission <= 0.02F && highFrequency <= 0.02F) {
                break;
            }
        }
        return new DirectPath(
                Math.max(0.0F, Math.min(1.0F, transmission)),
                Math.max(0.02F, Math.min(1.0F, highFrequency)),
                occluded
        );
    }

    private static List<ReflectionProbe> sampleReflectionProbes(
            ClientLevel level,
            LocalPlayer listener,
            Vec3 listenerPosition,
            AcousticMaterialConfig.Snapshot config
    ) {
        List<ReflectionProbe> probes = new ArrayList<>();
        for (int index = 0; index < config.probeCount(); index++) {
            // Use Fibonacci sphere distribution for even ray distribution
            double phi = Math.PI * (3.0D - Math.sqrt(5.0D));
            double y = 1.0D - (index / (double) (config.probeCount() - 1)) * 2.0D;
            double radius = Math.sqrt(1.0D - y * y);
            double theta = phi * index;
            double x = Math.cos(theta) * radius;
            double z = Math.sin(theta) * radius;
            Vec3 direction = new Vec3(x, y, z).normalize();
            Vec3 end = listenerPosition.add(direction.scale(config.probeDistance()));
            BlockHitResult hit = level.clip(new ClipContext(
                    listenerPosition,
                    end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    listener
            ));
            if (hit.getType() != HitResult.Type.BLOCK) {
                continue;
            }
            double hitDistance = hit.getLocation().distanceTo(listenerPosition);
            if (hitDistance < 0.35D) {
                continue;
            }
            BlockState blockState = level.getBlockState(hit.getBlockPos());
            AcousticMaterialConfig.Material material = materialFor(level, hit.getBlockPos(), blockState, config);
            Direction normal = hit.getDirection();
            Vec3 surfacePosition = hit.getLocation().add(normal.getStepX() * 0.06D, normal.getStepY() * 0.06D, normal.getStepZ() * 0.06D);
            probes.add(new ReflectionProbe(surfacePosition, hitDistance, material));
        }
        return List.copyOf(probes);
    }

    private static ReflectionPath traceFirstOrderReflection(
            ClientLevel level,
            LocalPlayer listener,
            Vec3 listenerPosition,
            Vec3 sourcePosition,
            List<ReflectionProbe> probes,
            AcousticMaterialConfig.Snapshot config
    ) {
        if (probes.isEmpty() || config.reflectionStrength() <= 0.0F) {
            return ReflectionPath.none();
        }
        double totalWeight = 0.0D;
        double weightedX = 0.0D;
        double weightedY = 0.0D;
        double weightedZ = 0.0D;
        int usedProbes = 0;
        List<Vec3> usedPositions = new ArrayList<>();
        for (ReflectionProbe probe : probes) {
            if (!hasClearPath(level, listener, sourcePosition, probe.position())) {
                continue;
            }
            double pathLength = probe.listenerDistance() + sourcePosition.distanceTo(probe.position());
            if (pathLength < 0.001D) {
                continue;
            }
            double weight = probe.material().reflectivity() / (1.0D + pathLength * pathLength);
            totalWeight += weight;
            weightedX += probe.position().x * weight;
            weightedY += probe.position().y * weight;
            weightedZ += probe.position().z * weight;
            usedPositions.add(probe.position());
            usedProbes++;
        }
        if (totalWeight <= 0.0001D) {
            return ReflectionPath.none();
        }
        Vec3 virtualPosition = new Vec3(weightedX / totalWeight, weightedY / totalWeight, weightedZ / totalWeight);
        float gain = (float) Math.min(0.75D, Math.sqrt(totalWeight) * config.reflectionStrength());
        return new ReflectionPath(virtualPosition, gain, usedProbes, List.copyOf(usedPositions));
    }

    private static boolean hasClearPath(ClientLevel level, LocalPlayer listener, Vec3 from, Vec3 to) {
        HitResult result = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, listener));
        return result.getType() == HitResult.Type.MISS
                || result.getLocation().distanceToSqr(from) >= to.distanceToSqr(from) - 0.02D;
    }

    private static AcousticEnvironment environmentFor(List<ReflectionProbe> probes, AcousticMaterialConfig.Snapshot config) {
        if (!config.enabled() || probes.isEmpty()) {
            return AcousticEnvironment.NONE;
        }
        double reflectivity = 0.0D;
        double density = 0.0D;
        for (ReflectionProbe probe : probes) {
            reflectivity += probe.material().reflectivity();
            density += 1.0D - Math.min(1.0D, probe.listenerDistance() / config.probeDistance());
        }
        float averageReflectivity = (float) (reflectivity / probes.size());
        float coverage = Math.min(1.0F, probes.size() / (float) config.probeCount());
        float closeness = (float) (density / probes.size());
        float reverbGain = clampFloat(averageReflectivity * coverage * (0.18F + closeness * 0.42F), 0.0F, 0.65F);
        float reverbDecay = clampFloat(0.35F + coverage * averageReflectivity * 1.75F, 0.25F, 2.5F);
        return new AcousticEnvironment(true, reverbGain, reverbDecay);
    }

    private static AcousticMaterialConfig.Material materialFor(
            ClientLevel level,
            BlockPos blockPos,
            BlockState blockState,
            AcousticMaterialConfig.Snapshot config
    ) {
        String blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
        String fallback = blockState.getFluidState().isEmpty()
                ? materialCategory(blockState.getSoundType(level, blockPos, null))
                : "water";
        return config.materialFor(blockId, fallback);
    }

    private static boolean isAcousticObstacle(ClientLevel level, BlockPos blockPos, BlockState blockState) {
        return !blockState.isAir() && (!blockState.getFluidState().isEmpty() || !blockState.getCollisionShape(level, blockPos).isEmpty());
    }

    private static String materialCategory(SoundType soundType) {
        if (soundType == SoundType.METAL || soundType == SoundType.ANVIL || soundType == SoundType.NETHERITE_BLOCK) {
            return "metal";
        }
        if (soundType == SoundType.GLASS || soundType == SoundType.AMETHYST) {
            return "glass";
        }
        if (soundType == SoundType.SNOW || soundType == SoundType.POWDER_SNOW) {
            return "snow";
        }
        if (soundType == SoundType.WOOL || soundType == SoundType.MOSS) {
            return "wool";
        }
        if (soundType == SoundType.WOOD || soundType == SoundType.BAMBOO_WOOD || soundType == SoundType.CHERRY_WOOD) {
            return "wood";
        }
        if (soundType == SoundType.GRAVEL || soundType == SoundType.GRASS || soundType == SoundType.SAND) {
            return "soil";
        }
        return "stone";
    }

    private static VoiceSpatialDebugSnapshot debugSnapshot(
            UUID speakerId,
            VoiceChannel channel,
            double distance,
            double pan,
            StereoGains gains,
            AcousticPath path
    ) {
        return new VoiceSpatialDebugSnapshot(
                speakerId,
                channel,
                distance,
                pan,
                gains.left(),
                gains.right(),
                path != null,
                path != null && path.occluded(),
                path == null ? 1.0F : path.directGain(),
                path == null ? 1.0F : path.highFrequencyGain(),
                path == null ? 0.0F : path.reflectionGain(),
                path == null ? 0 : path.reflectionProbeCount(),
                BACKEND_NAME
        );
    }

    private AcousticDebugSnapshot buildAcousticDebugSnapshot(Vec3 listenerPosition) {
        List<AcousticDebugSnapshot.Line> lines = new ArrayList<>();
        // Environmental probes (the ones that hit a wall)
        for (ReflectionProbe probe : reflectionProbes) {
            lines.add(new AcousticDebugSnapshot.Line(listenerPosition, probe.position(), 0, 255, 255, 180));
        }
        for (SourceSnapshot source : sources.values()) {
            AcousticPath path = source.path();
            // Direct path: listener -> source (red if occluded, green if clear)
            if (path.occluded()) {
                lines.add(new AcousticDebugSnapshot.Line(listenerPosition, source.position(), 232, 72, 60, 240));
            } else {
                lines.add(new AcousticDebugSnapshot.Line(listenerPosition, source.position(), 80, 230, 130, 240));
            }
            // Reflection legs: source -> reflection surface -> listener (orange)
            for (Vec3 reflectionPoint : path.reflectionPoints()) {
                lines.add(new AcousticDebugSnapshot.Line(source.position(), reflectionPoint, 245, 185, 60, 210));
                lines.add(new AcousticDebugSnapshot.Line(reflectionPoint, listenerPosition, 245, 185, 60, 140));
            }
            // Virtual position indicator: source -> virtual position (magenta, if differs)
            Vec3 virtualPos = path.virtualPosition();
            if (virtualPos.distanceToSqr(source.position()) > 0.25D) {
                lines.add(new AcousticDebugSnapshot.Line(source.position(), virtualPos, 200, 80, 230, 200));
            }
        }
        return new AcousticDebugSnapshot(true, lines);
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

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ListenerSnapshot(Vec3 position, float yaw, AcousticEnvironment environment) {
    }

    private record SourceSnapshot(
            Vec3 position,
            float yaw,
            float pitch,
            Vec3 listenerPosition,
            AcousticPath path,
            long computedAtMillis,
            long configRevision
    ) {
    }

    private record DirectPath(float transmissionGain, float highFrequencyGain, boolean occluded) {
    }

    private record AcousticPath(
            Vec3 virtualPosition,
            float directGain,
            float highFrequencyGain,
            float reflectionGain,
            boolean occluded,
            int reflectionProbeCount,
            List<Vec3> reflectionPoints
    ) {
        static AcousticPath clear(Vec3 sourcePosition, double distance) {
            return new AcousticPath(sourcePosition, volumeAt(distance), 1.0F, 0.0F, false, 0, List.of());
        }
    }

    private record ReflectionProbe(Vec3 position, double listenerDistance, AcousticMaterialConfig.Material material) {
    }

    private record ReflectionPath(Vec3 virtualPosition, float gain, int probeCount, List<Vec3> usedPositions) {
        static ReflectionPath none() {
            return new ReflectionPath(null, 0.0F, 0, List.of());
        }
    }

    private record AcousticEnvironment(boolean known, float reverbGain, float reverbDecaySeconds) {
        private static final AcousticEnvironment NONE = new AcousticEnvironment(false, 0.0F, 0.7F);
    }
}

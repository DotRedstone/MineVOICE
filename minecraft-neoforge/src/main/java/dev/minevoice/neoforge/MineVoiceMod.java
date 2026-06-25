package dev.minevoice.neoforge;

import dev.minevoice.common.config.VoiceConstants;
import dev.minevoice.neoforge.config.MineVoiceModConfig;
import dev.minevoice.neoforge.config.MineVoiceModConfigLoader;
import dev.minevoice.neoforge.network.MineVoicePayloads;
import dev.minevoice.neoforge.server.AdvertisedVoiceEndpointResolver;
import dev.minevoice.neoforge.server.IntegratedVoiceServerManager;
import dev.minevoice.neoforge.server.PlayerVoiceStateManager;
import dev.minevoice.neoforge.server.VoiceGroup;
import dev.minevoice.neoforge.server.VoiceGroupManager;
import dev.minevoice.neoforge.server.VoiceServerInfoSender;
import dev.minevoice.neoforge.server.VoiceStatePublisher;
import dev.minevoice.neoforge.server.VoiceTokenService;
import dev.minevoice.neoforge.network.VoiceGroupAction;
import dev.minevoice.neoforge.network.VoiceGroupActionPayload;
import dev.minevoice.neoforge.network.VoicePeerMutePayload;
import dev.minevoice.neoforge.network.VoicePlayerStatusPayload;
import dev.minevoice.neoforge.network.VoiceRosterEntry;
import dev.minevoice.neoforge.network.VoiceRosterPayload;
import dev.minevoice.neoforge.network.VoiceServerInfoPayload;
import dev.minevoice.common.auth.AuthToken;
import dev.minevoice.common.auth.AuthTokenCodec;
import dev.minevoice.common.config.VoiceMode;
import dev.minevoice.common.protocol.VoiceProtocolVersion;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;

@Mod(MineVoiceMod.MOD_ID)
public final class MineVoiceMod {
    public static final String MOD_ID = VoiceConstants.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(VoiceConstants.PROJECT_NAME);
    private static MineVoiceMod instance;

    private MineVoiceModConfig config = MineVoiceModConfig.localDefaults();
    private IntegratedVoiceServerManager integratedVoiceServerManager = new IntegratedVoiceServerManager(config);
    private AdvertisedVoiceEndpointResolver advertisedEndpointResolver = new AdvertisedVoiceEndpointResolver(new dev.minevoice.common.util.MineVoiceLogger("neoforge-endpoint", false));
    private VoiceTokenService voiceTokenService = new VoiceTokenService(config.sharedSecret());
    private final VoiceServerInfoSender voiceServerInfoSender = new VoiceServerInfoSender();
    private final PlayerVoiceStateManager playerVoiceStates = new PlayerVoiceStateManager();
    private final VoiceGroupManager voiceGroupManager = new VoiceGroupManager();
    private final VoiceStatePublisher voiceStatePublisher = new VoiceStatePublisher();
    private int stateSyncTicks;

    public MineVoiceMod(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;
        modEventBus.addListener(MineVoicePayloads::register);
        NeoForge.EVENT_BUS.register(this);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registerClient(modEventBus, modContainer);
        }
        LOGGER.info("{} loaded", VoiceConstants.DISPLAY_NAME);
    }

    private static void registerClient(IEventBus modEventBus, ModContainer modContainer) {
        try {
            Class<?> bootstrap = Class.forName("dev.minevoice.neoforge.client.MineVoiceClientBootstrap");
            bootstrap.getMethod("register", IEventBus.class, ModContainer.class).invoke(null, modEventBus, modContainer);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("failed to register MineVOICE client bootstrap", exception);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        reloadServerConfig();
        voiceGroupManager.clear();
        stateSyncTicks = 0;
        if (config.mode() == VoiceMode.LOCAL) {
            integratedVoiceServerManager.start();
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        integratedVoiceServerManager.stop();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        playerVoiceStates.markConnected(player.getUUID());
        sendVoiceServerInfo(player);
        broadcastVoiceRoster();
        publishVoiceState(player.server);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        playerVoiceStates.markDisconnected(player.getUUID());
        voiceGroupManager.leave(player.getUUID());
        broadcastVoiceRoster();
        publishVoiceState(player.server);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        stateSyncTicks++;
        if (stateSyncTicks >= 20) {
            stateSyncTicks = 0;
            publishVoiceState(event.getServer());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("minevoice")
                .then(Commands.literal("status")
                        .executes(context -> {
                            sendStatus(context.getSource());
                            return 1;
                        }))
                .then(Commands.literal("debug")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            sendDebug(context.getSource());
                            return 1;
                        }))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            reloadFromCommand(context.getSource());
                            return 1;
                        }))
                .then(Commands.literal("test-endpoint")
                        .executes(context -> {
                            sendEndpointTest(context.getSource());
                            return 1;
                        })));
    }

    public static void handleGroupAction(ServerPlayer player, VoiceGroupActionPayload payload) {
        if (instance != null) {
            instance.applyGroupAction(player, payload);
        }
    }

    public static void handlePlayerStatus(ServerPlayer player, VoicePlayerStatusPayload payload) {
        if (instance != null) {
            instance.playerVoiceStates.setMuted(player.getUUID(), payload.muted());
            instance.broadcastVoiceRoster();
            instance.publishVoiceState(player.server);
        }
    }

    public static void handlePeerMute(ServerPlayer player, VoicePeerMutePayload payload) {
        if (instance != null) {
            instance.playerVoiceStates.setPeerMuted(player.getUUID(), payload.playerId(), payload.muted());
            instance.publishVoiceState(player.server);
        }
    }

    private void applyGroupAction(ServerPlayer player, VoiceGroupActionPayload payload) {
        try {
            switch (payload.action()) {
                case CREATE -> voiceGroupManager.create(player.getUUID(), payload.groupName(), payload.password());
                case JOIN -> {
                    if (payload.groupId() == null || !voiceGroupManager.join(player.getUUID(), payload.groupId(), payload.password())) {
                        player.displayClientMessage(Component.literal("MineVOICE: group is no longer available"), true);
                        return;
                    }
                }
                case LEAVE -> voiceGroupManager.leave(player.getUUID());
            }
            broadcastVoiceRoster();
            publishVoiceState(player.server);
        } catch (IllegalArgumentException exception) {
            player.displayClientMessage(Component.literal("MineVOICE: " + exception.getMessage()), true);
        }
    }

    private void broadcastVoiceRoster() {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        List<VoiceRosterEntry> entries = server.getPlayerList().getPlayers().stream()
                .map(player -> rosterEntry(player))
                .toList();
        PacketDistributor.sendToAllPlayers(new VoiceRosterPayload(entries));
    }

    private VoiceRosterEntry rosterEntry(ServerPlayer player) {
        VoiceGroup group = voiceGroupManager.groupFor(player.getUUID());
        return new VoiceRosterEntry(
                player.getUUID(),
                player.getGameProfile().getName(),
                group == null ? null : group.id(),
                group == null ? "" : group.name(),
                group != null && group.passwordProtected(),
                playerVoiceStates.muted(player.getUUID())
        );
    }

    private void publishVoiceState(net.minecraft.server.MinecraftServer server) {
        try {
            if (config.mode() == VoiceMode.LOCAL) {
                integratedVoiceServerManager.replacePlayerStates(
                        voiceStatePublisher.snapshot(server, voiceGroupManager, playerVoiceStates).players()
                );
                return;
            }
            voiceStatePublisher.publish(server, config, voiceGroupManager, playerVoiceStates);
        } catch (RuntimeException exception) {
            LOGGER.warn("failed to publish MineVOICE player state: {}", exception.getMessage());
        }
    }

    private String resolveAdvertiseHost(ServerPlayer player) {
        return advertisedEndpointResolver.resolveFor(config, player);
    }

    private void reloadServerConfig() {
        reloadServerConfig(false);
    }

    private void reloadServerConfig(boolean restartLocalServer) {
        if (restartLocalServer) {
            integratedVoiceServerManager.stop();
        }
        config = new MineVoiceModConfigLoader().load(FMLPaths.CONFIGDIR.get().resolve("minevoice-server.properties"));
        integratedVoiceServerManager = new IntegratedVoiceServerManager(config);
        advertisedEndpointResolver = new AdvertisedVoiceEndpointResolver(new dev.minevoice.common.util.MineVoiceLogger("neoforge-endpoint", config.enableDebugLog()));
        voiceTokenService = new VoiceTokenService(config.sharedSecret());
        if (restartLocalServer && config.mode() == VoiceMode.LOCAL) {
            integratedVoiceServerManager.start();
        }
        String endpoint = config.mode() == VoiceMode.REMOTE
                ? config.remoteVoiceHost() + ":" + config.remoteVoicePort()
                : config.localVoiceAdvertiseHost() + ":" + config.localVoiceAdvertisePort();
        LOGGER.info("MineVOICE server config loaded: mode={}, endpoint={}", config.mode(), endpoint);
    }

    private void sendVoiceServerInfo(ServerPlayer player) {
        AuthToken token = voiceTokenService.issue(player.getUUID(), "minevoice-local", Duration.ofMinutes(5));
        String host = config.mode() == VoiceMode.LOCAL
                ? resolveAdvertiseHost(player)
                : config.remoteVoiceHost();
        int port = config.mode() == VoiceMode.LOCAL
                ? config.localVoiceAdvertisePort()
                : config.remoteVoicePort();
        if (config.mode() == VoiceMode.LOCAL) {
            LOGGER.info("MineVOICE local voice endpoint advertised as {}:{}", host, port);
        }

        voiceServerInfoSender.sendToPlayer(player, new VoiceServerInfoPayload(
                config.mode(),
                host,
                port,
                AuthTokenCodec.encodeToString(token),
                VoiceProtocolVersion.CURRENT,
                config.voiceCodec()
        ));
    }

    private void sendStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("MineVOICE status: " + statusSummary()), false);
    }

    private void sendDebug(CommandSourceStack source) {
        boolean secretConfigured = !"change-me".equals(config.sharedSecret());
        source.sendSuccess(() -> Component.literal("MineVOICE debug: " + statusSummary()
                + " secretConfigured=" + secretConfigured
                + " enableDebugLog=" + config.enableDebugLog()
                + " occlusion=" + config.enableOcclusion()
                + " soundPhysicsCompat=" + config.enableSoundPhysicsCompat()
                + " jitterBufferMs=" + config.jitterBufferMs()), false);
    }

    private void reloadFromCommand(CommandSourceStack source) {
        reloadServerConfig(true);
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            sendVoiceServerInfo(player);
        }
        broadcastVoiceRoster();
        publishVoiceState(source.getServer());
        source.sendSuccess(() -> Component.literal("MineVOICE config reloaded: " + statusSummary()), true);
    }

    private void sendEndpointTest(CommandSourceStack source) {
        String host = endpointHostFor(source);
        int port = endpointPort();
        String warning = endpointWarning(host);
        try {
            InetAddress address = InetAddress.getByName(host);
            source.sendSuccess(() -> Component.literal("MineVOICE endpoint: "
                    + host + ":" + port
                    + " resolved=" + address.getHostAddress()
                    + warning), false);
        } catch (Exception exception) {
            source.sendFailure(Component.literal("MineVOICE endpoint resolve failed: "
                    + host + ":" + port + " reason=" + exception.getMessage() + warning));
        }
    }

    private String statusSummary() {
        return "mode=" + config.mode()
                + " endpoint=" + endpointDescription()
                + " codec=" + config.voiceCodec()
                + " protocol=" + VoiceProtocolVersion.CURRENT
                + " localRunning=" + integratedVoiceServerManager.running()
                + " players=" + playerVoiceStates.connectedCount()
                + " muted=" + playerVoiceStates.mutedCount()
                + " groups=" + voiceGroupManager.groups().size()
                + " proximityDistance=" + config.proximityDistance();
    }

    private String endpointDescription() {
        return config.mode() == VoiceMode.REMOTE
                ? config.remoteVoiceHost() + ":" + config.remoteVoicePort()
                : config.localVoiceAdvertiseHost() + ":" + config.localVoiceAdvertisePort();
    }

    private String endpointHostFor(CommandSourceStack source) {
        if (config.mode() == VoiceMode.REMOTE) {
            return config.remoteVoiceHost();
        }
        if (source.getEntity() instanceof ServerPlayer player) {
            return resolveAdvertiseHost(player);
        }
        return "auto".equalsIgnoreCase(config.localVoiceAdvertiseHost())
                ? "127.0.0.1"
                : config.localVoiceAdvertiseHost();
    }

    private int endpointPort() {
        return config.mode() == VoiceMode.REMOTE ? config.remoteVoicePort() : config.localVoiceAdvertisePort();
    }

    private String endpointWarning(String host) {
        if (config.mode() == VoiceMode.REMOTE
                && ("127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host))) {
            return " warning=remote host is loopback; remote clients usually cannot reach it";
        }
        if (config.mode() == VoiceMode.LOCAL && "auto".equalsIgnoreCase(config.localVoiceAdvertiseHost())) {
            return " note=auto advertise host is resolved per player";
        }
        return "";
    }
}

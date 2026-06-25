package dev.minevoice.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.minevoice.neoforge.client.audio.VoicePlaybackStats;
import dev.minevoice.neoforge.client.screen.MineVoiceSettingsScreen;
import dev.minevoice.neoforge.client.screen.MineVoiceMenuScreen;
import dev.minevoice.neoforge.client.hud.MineVoiceHudOverlay;
import dev.minevoice.neoforge.network.VoiceGroupAction;
import dev.minevoice.neoforge.network.VoiceGroupActionPayload;
import dev.minevoice.neoforge.network.VoicePeerMutePayload;
import dev.minevoice.neoforge.network.VoicePlayerStatusPayload;
import dev.minevoice.neoforge.network.VoiceRosterEntry;
import dev.minevoice.neoforge.network.VoiceRosterPayload;
import dev.minevoice.neoforge.network.VoiceServerInfoPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

import java.util.Locale;

public final class MineVoiceClientBootstrap {
    private static final ResourceLocation NAMEPLATE_MICROPHONE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "minevoice", "textures/gui/sprites/microphone_status.png"
    );
    private static final ResourceLocation NAMEPLATE_MICROPHONE_MUTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "minevoice", "textures/gui/sprites/microphone_muted.png"
    );
    private static final ResourceLocation NAMEPLATE_SPEAKER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "minevoice", "textures/gui/sprites/speaker_status.png"
    );
    private static final ResourceLocation NAMEPLATE_SPEAKER_MUTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "minevoice", "textures/gui/sprites/speaker_muted.png"
    );
    private static final ClientSettingsStore SETTINGS_STORE = new FileClientSettingsStore(
            FMLPaths.CONFIGDIR.get().resolve("minevoice-client.properties")
    );
    private static final FilePlayerVolumeStore PLAYER_VOLUME_STORE = new FilePlayerVolumeStore(
            FMLPaths.CONFIGDIR.get().resolve("minevoice-player-volumes.properties")
    );
    private static final MineVoiceClientUiController UI_CONTROLLER = new MineVoiceClientUiController(SETTINGS_STORE);
    private static final ClientVoiceConnectionManager VOICE_CONNECTION_MANAGER = new ClientVoiceConnectionManager(
            SETTINGS_STORE,
            PLAYER_VOLUME_STORE::volume,
            PLAYER_VOLUME_STORE::muted
    );
    private static final VoicePlayerDirectory VOICE_DIRECTORY = new VoicePlayerDirectory();
    private static final KeybindManager KEYBIND_MANAGER = new KeybindManager(
            VOICE_CONNECTION_MANAGER,
            MineVoiceClientBootstrap::openVoiceMenuScreen,
            MineVoiceClientBootstrap::toggleMuted,
            MineVoiceClientBootstrap::toggleDeafened
    );

    private MineVoiceClientBootstrap() {
    }

    public static void register(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (minecraft, parent) -> new MineVoiceSettingsScreen(parent, UI_CONTROLLER)
        );
        modEventBus.addListener(MineVoiceClientBootstrap::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(MineVoiceClientBootstrap::onClientTick);
        NeoForge.EVENT_BUS.addListener(MineVoiceClientBootstrap::onRenderGui);
        NeoForge.EVENT_BUS.addListener(MineVoiceClientBootstrap::onRenderNameTag);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEYBIND_MANAGER.openSettingsMapping());
        event.register(KEYBIND_MANAGER.pushToTalkMapping());
        event.register(KEYBIND_MANAGER.groupTalkMapping());
        event.register(KEYBIND_MANAGER.toggleMuteMapping());
        event.register(KEYBIND_MANAGER.toggleDeafenMapping());
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        VOICE_CONNECTION_MANAGER.refreshSpatialState();
        KEYBIND_MANAGER.handleClientTick();
    }

    public static void handleVoiceServerInfo(VoiceServerInfoPayload payload) {
        VOICE_CONNECTION_MANAGER.connectFromServerPayload(payload);
        syncLocalVoiceStatus();
        syncPeerMuteStates();
        Minecraft minecraft = Minecraft.getInstance();
        ClientAudioSettings settings = SETTINGS_STORE.load();
        if (minecraft.player != null && settings.debugInfoLevel() != DebugInfoLevel.OFF) {
            minecraft.player.displayClientMessage(Component.literal(
                    "MineVOICE: " + VOICE_CONNECTION_MANAGER.status()
                            + " " + payload.voiceHost() + ":" + payload.voicePort()
            ), false);
            if (settings.debugInfoLevel() == DebugInfoLevel.VERBOSE) {
                minecraft.player.displayClientMessage(Component.literal(
                        "MineVOICE: mode=" + payload.mode()
                                + " proto=" + payload.protocolVersion()
                                + " codec=" + payload.voiceCodec()
                                + " activation=" + settings.activationMode()
                                + " spatial=" + settings.spatialAudioEnabled()
                ), false);
            }
        }
    }

    public static void handleVoiceRoster(VoiceRosterPayload payload) {
        VOICE_DIRECTORY.update(payload);
    }

    public static MineVoiceClientUiController uiController() {
        return UI_CONTROLLER;
    }

    public static ClientAudioSettings settings() {
        return SETTINGS_STORE.load();
    }

    public static VoiceConnectionStatus connectionStatus() {
        return VOICE_CONNECTION_MANAGER.status();
    }

    public static VoiceNetworkStats voiceNetworkStats() {
        return VOICE_CONNECTION_MANAGER.networkStats();
    }

    public static VoicePlaybackStats voicePlaybackStats() {
        return VOICE_CONNECTION_MANAGER.playbackStats();
    }

    public static String debugConnectionSummary() {
        VoiceNetworkStats stats = voiceNetworkStats();
        VoicePlaybackStats playbackStats = voicePlaybackStats();
        return String.format(
                Locale.ROOT,
                "status=%s endpoint=%s proto=%d codec=%s up=%ds udp=%.1f/%.1fKiB pkts=%d/%d voice=%.1f/%.1fKiB frames=%d/%d fps=%.1f/%.1f jitter=speakers:%d buffered:%d late:%d dropped:%d missing:%d",
                stats.status(),
                stats.endpoint(),
                stats.protocolVersion(),
                stats.codec(),
                stats.connectedMillis() / 1000L,
                stats.udpSentKiB(),
                stats.udpReceivedKiB(),
                stats.udpPacketsSent(),
                stats.udpPacketsReceived(),
                stats.voiceSentKiB(),
                stats.voiceReceivedKiB(),
                stats.voiceFramesSent(),
                stats.voiceFramesReceived(),
                stats.voiceFramesSentPerSecond(),
                stats.voiceFramesReceivedPerSecond(),
                playbackStats.activeSpeakers(),
                playbackStats.bufferedFrames(),
                playbackStats.latePackets(),
                playbackStats.droppedPackets(),
                playbackStats.missingFrames()
        );
    }

    public static VoicePlayerDirectory voiceDirectory() {
        return VOICE_DIRECTORY;
    }

    public static VoiceSpeakerTracker speakerTracker() {
        return VOICE_CONNECTION_MANAGER.speakerTracker();
    }

    public static void toggleMuted() {
        ClientAudioSettings current = SETTINGS_STORE.load();
        SETTINGS_STORE.save(current.withMuted(!current.muted()));
        syncLocalVoiceStatus();
    }

    public static void toggleDeafened() {
        ClientAudioSettings current = SETTINGS_STORE.load();
        SETTINGS_STORE.save(current.withDeafened(!current.deafened()));
    }

    public static void syncLocalVoiceStatus() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(new VoicePlayerStatusPayload(SETTINGS_STORE.load().muted()));
        }
    }

    public static void createGroup(String groupName, String password) {
        sendGroupAction(new VoiceGroupActionPayload(VoiceGroupAction.CREATE, null, groupName, password));
    }

    public static void joinGroup(java.util.UUID groupId, String password) {
        sendGroupAction(new VoiceGroupActionPayload(VoiceGroupAction.JOIN, groupId, "", password));
    }

    public static void leaveGroup() {
        sendGroupAction(new VoiceGroupActionPayload(VoiceGroupAction.LEAVE, null, "", ""));
    }

    public static float playerVolume(java.util.UUID playerId) {
        return PLAYER_VOLUME_STORE.volume(playerId);
    }

    public static void setPlayerVolume(java.util.UUID playerId, float volume) {
        PLAYER_VOLUME_STORE.setVolume(playerId, volume);
    }

    public static boolean playerMuted(java.util.UUID playerId) {
        return PLAYER_VOLUME_STORE.muted(playerId);
    }

    public static void togglePlayerMuted(java.util.UUID playerId) {
        boolean muted = !PLAYER_VOLUME_STORE.muted(playerId);
        PLAYER_VOLUME_STORE.setMuted(playerId, muted);
        sendPeerMute(playerId, muted);
    }

    private static void syncPeerMuteStates() {
        for (java.util.UUID playerId : PLAYER_VOLUME_STORE.mutedPlayers()) {
            sendPeerMute(playerId, true);
        }
    }

    private static void sendPeerMute(java.util.UUID playerId, boolean muted) {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new VoicePeerMutePayload(playerId, muted));
        }
    }

    private static void sendGroupAction(VoiceGroupActionPayload payload) {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(payload);
        }
    }

    private static void openVoiceMenuScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof MineVoiceMenuScreen)) {
            minecraft.setScreen(new MineVoiceMenuScreen(minecraft.screen));
        }
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        MineVoiceHudOverlay.render(
                event,
                VOICE_CONNECTION_MANAGER.hudState(),
                SETTINGS_STORE.load(),
                VOICE_DIRECTORY,
                VOICE_CONNECTION_MANAGER.speakerTracker()
        );
    }

    private static void onRenderNameTag(RenderNameTagEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientAudioSettings settings = SETTINGS_STORE.load();
        if (minecraft.player == null
                || minecraft.options.hideGui
                || !settings.nameplateIconsEnabled()
                || !(event.getEntity() instanceof Player)
                || event.getEntity().equals(minecraft.player)) {
            return;
        }
        VoiceRosterEntry entry = VOICE_DIRECTORY.get(event.getEntity().getUUID());
        ResourceLocation icon = nameplateStatusIcon(settings, entry, event.getEntity().getUUID());
        if (icon != null) {
            renderNameplateIcon(event, icon);
        }
    }

    private static ResourceLocation nameplateStatusIcon(
            ClientAudioSettings settings,
            VoiceRosterEntry entry,
            java.util.UUID playerId
    ) {
        if (settings.deafened() || PLAYER_VOLUME_STORE.muted(playerId)) {
            return NAMEPLATE_SPEAKER_MUTED_TEXTURE;
        }
        if (entry != null && entry.muted()) {
            return NAMEPLATE_MICROPHONE_MUTED_TEXTURE;
        }
        if (VOICE_CONNECTION_MANAGER.speakerTracker().isSpeaking(playerId)) {
            return NAMEPLATE_MICROPHONE_TEXTURE;
        }
        return entry == null ? null : NAMEPLATE_SPEAKER_TEXTURE;
    }

    private static void renderNameplateIcon(RenderNameTagEvent event, ResourceLocation icon) {
        Entity entity = event.getEntity();
        Vec3 attachment = entity.getAttachments().getNullable(
                EntityAttachment.NAME_TAG,
                0,
                entity.getViewYRot(event.getPartialTick())
        );
        if (attachment == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(attachment.x, attachment.y + 0.5D, attachment.z);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);

        Font font = event.getEntityRenderer().getFont();
        Matrix4f matrix = poseStack.last().pose();
        float x = font.width(event.getContent()) / 2.0F + 3.0F;
        float y = "deadmau5".equals(event.getContent().getString()) ? -11.0F : -1.0F;
        drawNameplateQuad(event, icon, matrix, x, y, 9.0F);
        poseStack.popPose();
    }

    private static void drawNameplateQuad(
            RenderNameTagEvent event,
            ResourceLocation icon,
            Matrix4f matrix,
            float x,
            float y,
            float size
    ) {
        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.text(icon));
        float right = x + size;
        float bottom = y + size;
        consumer.addVertex(matrix, x, bottom, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setLight(event.getPackedLight());
        consumer.addVertex(matrix, right, bottom, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setLight(event.getPackedLight());
        consumer.addVertex(matrix, right, y, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setLight(event.getPackedLight());
        consumer.addVertex(matrix, x, y, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setLight(event.getPackedLight());
    }

    public static void applyPushToTalkBinding(String configuredKey) {
        KEYBIND_MANAGER.applyPushToTalkBinding(configuredKey);
    }

    public static void suspendAudioForDeviceTest() {
        VOICE_CONNECTION_MANAGER.suspendAudioForDeviceTest();
    }

    public static void resumeAudioAfterDeviceTest() {
        VOICE_CONNECTION_MANAGER.resumeAudioAfterDeviceTest();
    }
}

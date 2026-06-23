package dev.minevoice.neoforge.client;

import dev.minevoice.neoforge.client.screen.MineVoiceSettingsScreen;
import dev.minevoice.neoforge.client.screen.MineVoiceMenuScreen;
import dev.minevoice.neoforge.client.hud.MineVoiceHudOverlay;
import dev.minevoice.neoforge.network.VoiceGroupAction;
import dev.minevoice.neoforge.network.VoiceGroupActionPayload;
import dev.minevoice.neoforge.network.VoicePlayerStatusPayload;
import dev.minevoice.neoforge.network.VoiceRosterEntry;
import dev.minevoice.neoforge.network.VoiceRosterPayload;
import dev.minevoice.neoforge.network.VoiceServerInfoPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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

public final class MineVoiceClientBootstrap {
    private static final ClientSettingsStore SETTINGS_STORE = new FileClientSettingsStore(
            FMLPaths.CONFIGDIR.get().resolve("minevoice-client.properties")
    );
    private static final MineVoiceClientUiController UI_CONTROLLER = new MineVoiceClientUiController(SETTINGS_STORE);
    private static final ClientVoiceConnectionManager VOICE_CONNECTION_MANAGER = new ClientVoiceConnectionManager(SETTINGS_STORE);
    private static final VoicePlayerDirectory VOICE_DIRECTORY = new VoicePlayerDirectory();
    private static final KeybindManager KEYBIND_MANAGER = new KeybindManager(
            VOICE_CONNECTION_MANAGER,
            MineVoiceClientBootstrap::openVoiceMenuScreen
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
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        VOICE_CONNECTION_MANAGER.refreshSpatialState();
        KEYBIND_MANAGER.handleClientTick();
    }

    public static void handleVoiceServerInfo(VoiceServerInfoPayload payload) {
        VOICE_CONNECTION_MANAGER.connectFromServerPayload(payload);
        syncLocalVoiceStatus();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && SETTINGS_STORE.load().showDebugConnectionInfo()) {
            minecraft.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("MineVOICE voice connection: " + VOICE_CONNECTION_MANAGER.status()),
                    false
            );
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

    public static void createGroup(String groupName) {
        sendGroupAction(new VoiceGroupActionPayload(VoiceGroupAction.CREATE, null, groupName));
    }

    public static void joinGroup(java.util.UUID groupId) {
        sendGroupAction(new VoiceGroupActionPayload(VoiceGroupAction.JOIN, groupId, ""));
    }

    public static void leaveGroup() {
        sendGroupAction(new VoiceGroupActionPayload(VoiceGroupAction.LEAVE, null, ""));
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
        if (minecraft.player == null || minecraft.options.hideGui || event.getEntity().equals(minecraft.player)) {
            return;
        }
        VoiceRosterEntry entry = VOICE_DIRECTORY.get(event.getEntity().getUUID());
        if (entry != null && entry.muted()) {
            event.setContent(event.getContent().copy().append(Component.literal(" \u2715").withStyle(ChatFormatting.RED)));
        } else if (VOICE_CONNECTION_MANAGER.speakerTracker().isSpeaking(event.getEntity().getUUID())) {
            event.setContent(event.getContent().copy().append(Component.literal(" \u25CF").withStyle(ChatFormatting.GREEN)));
        } else if (entry != null && entry.groupId() != null) {
            event.setContent(event.getContent().copy().append(Component.literal(" \u25C6").withStyle(ChatFormatting.AQUA)));
        }
    }

    public static void applyPushToTalkBinding(String configuredKey) {
        KEYBIND_MANAGER.applyPushToTalkBinding(configuredKey);
    }
}

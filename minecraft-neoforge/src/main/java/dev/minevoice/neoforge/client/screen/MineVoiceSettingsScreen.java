package dev.minevoice.neoforge.client.screen;

import dev.minevoice.neoforge.client.AudioDevice;
import dev.minevoice.neoforge.client.ClientAudioDeviceScanner;
import dev.minevoice.neoforge.client.DebugInfoLevel;
import dev.minevoice.neoforge.client.MineVoiceClientBootstrap;
import dev.minevoice.neoforge.client.MineVoiceClientUiController;
import dev.minevoice.neoforge.client.VoiceActivationMode;
import dev.minevoice.neoforge.client.VoiceNetworkStats;
import dev.minevoice.neoforge.client.audio.JavaSoundAudioDeviceTester;
import dev.minevoice.neoforge.client.audio.SoundPhysicsCompat;
import dev.minevoice.neoforge.client.audio.VoicePlaybackStats;
import dev.minevoice.neoforge.client.audio.VoiceSpatialDebugSnapshot;

import dev.minevoice.neoforge.client.ui.MineVoiceSettingsScreenModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class MineVoiceSettingsScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 292;
    private static final int PANEL_MAX_HEIGHT = 250;
    private static final int ROW_HEIGHT = 20;

    private final Screen parent;
    private final MineVoiceClientUiController uiController;
    private final MineVoiceSettingsScreenModel model;
    private final List<AudioDevice> inputDevices = ClientAudioDeviceScanner.inputDevices();
    private final List<AudioDevice> outputDevices = ClientAudioDeviceScanner.outputDevices();

    private SettingsTab selectedTab = SettingsTab.AUDIO;
    private Button microphoneTestButton;
    private volatile String deviceTestStatusKey;
    private volatile boolean deviceTestRunning;
    private volatile float microphoneTestLevel;
    private JavaSoundAudioDeviceTester.InputTestSession microphoneTest;

    public MineVoiceSettingsScreen(Screen parent, MineVoiceClientUiController uiController) {
        super(Component.translatable("screen.minevoice.settings"));
        this.parent = parent;
        this.uiController = uiController;
        this.model = uiController.openSettingsScreen();
    }

    @Override
    protected void init() {
        clearWidgets();

        int panelLeft = panelLeft();
        int panelTop = panelTop();
        int contentLeft = panelLeft + 6;
        int contentWidth = panelWidth() - 12;

        int tabGap = 3;
        int tabWidth = (contentWidth - tabGap * (SettingsTab.values().length - 1)) / SettingsTab.values().length;
        for (int index = 0; index < SettingsTab.values().length; index++) {
            SettingsTab tab = SettingsTab.values()[index];
            Button tabButton = Button.builder(tab.title(), button -> {
                        stopDeviceTest();
                        selectedTab = tab;
                        rebuildWidgets();
                    })
                    .bounds(contentLeft + index * (tabWidth + tabGap), panelTop + 25, tabWidth, 18)
                    .build();
            tabButton.active = tab != selectedTab;
            addRenderableWidget(tabButton);
        }

        int rowY = panelTop + 50;
        switch (selectedTab) {
            case AUDIO -> initAudioTab(contentLeft, rowY, contentWidth);
            case VOICE -> initVoiceTab(contentLeft, rowY, contentWidth);
            case UI -> initUiTab(contentLeft, rowY, contentWidth);
            case DEBUG -> initDebugTab(contentLeft, rowY, contentWidth);
        }

        int actionGap = 3;
        int actionWidth = (contentWidth - actionGap * 2) / 3;
        int actionTop = panelTop + panelHeight() - 25;
        addButton(Component.translatable("gui.done"), contentLeft, actionTop, actionWidth, 19, button -> saveAndClose());
        addButton(Component.translatable("controls.reset"), contentLeft + actionWidth + actionGap, actionTop, actionWidth, 19, button -> reset());
        addButton(Component.translatable("gui.cancel"), contentLeft + (actionWidth + actionGap) * 2, actionTop, actionWidth, 19, button -> closeWithoutSaving());
    }

    private void initAudioTab(int left, int y, int contentWidth) {
        addDeviceButton(left, y, contentWidth, "screen.minevoice.microphone_device", "screen.minevoice.select_microphone", model.microphoneDevice(), inputDevices,
                model::setMicrophoneDevice);
        addDeviceButton(left, y + 21, contentWidth, "screen.minevoice.output_device", "screen.minevoice.select_output_device", model.outputDevice(), outputDevices,
                model::setOutputDevice);
        addRenderableWidget(new VolumeSlider(left, y + 42, contentWidth, ROW_HEIGHT, "screen.minevoice.master_volume", model.masterVolume()) {
            @Override
            protected void applyValue() {
                model.setMasterVolume((float) value);
            }
        });
        addRenderableWidget(new VolumeSlider(left, y + 63, contentWidth, ROW_HEIGHT, "screen.minevoice.voice_chat_volume", model.voiceChatVolume()) {
            @Override
            protected void applyValue() {
                model.setVoiceChatVolume((float) value);
            }
        });
        addRenderableWidget(new VolumeSlider(left, y + 84, contentWidth, ROW_HEIGHT, "screen.minevoice.microphone_volume", model.microphoneVolume()) {
            @Override
            protected void applyValue() {
                model.setMicrophoneVolume((float) value);
            }
        });
        addButton(Component.translatable("screen.minevoice.test_output"),
                left, y + 105, (contentWidth - 3) / 2, ROW_HEIGHT, button -> {
                    startOutputTest();
                });
        microphoneTestButton = addButton(microphoneTestButtonMessage(),
                left + (contentWidth + 3) / 2, y + 105, (contentWidth - 3) / 2, ROW_HEIGHT, button -> {
                    toggleMicrophoneTest();
                });
    }

    private void initVoiceTab(int left, int y, int contentWidth) {
        int halfWidth = (contentWidth - 3) / 2;
        int rightX = left + (contentWidth + 3) / 2;

        addButton(publicActivationModeMessage(), left, y, halfWidth, ROW_HEIGHT, button -> {
            model.setActivationMode(nextActivationMode());
            button.setMessage(publicActivationModeMessage());
        });
        addButton(groupActivationModeMessage(), rightX, y, halfWidth, ROW_HEIGHT, button -> {
            model.setGroupActivationMode(nextGroupActivationMode());
            button.setMessage(groupActivationModeMessage());
        });
        
        addRenderableWidget(new VolumeSlider(left, y + 21, halfWidth, ROW_HEIGHT, "screen.minevoice.public_voice_activation_threshold", model.voiceActivationThreshold()) {
            @Override
            protected void applyValue() {
                model.setVoiceActivationThreshold((float) value);
            }
        });
        addRenderableWidget(new VolumeSlider(rightX, y + 21, halfWidth, ROW_HEIGHT, "screen.minevoice.group_voice_activation_threshold", model.groupVoiceActivationThreshold()) {
            @Override
            protected void applyValue() {
                model.setGroupVoiceActivationThreshold((float) value);
            }
        });
        
        addToggle(left, y + 42, contentWidth, "screen.minevoice.spatial_audio", model.spatialAudioEnabled(),
                model::setSpatialAudioEnabled);
        
        addToggle(left, y + 63, contentWidth, "screen.minevoice.hrtf_enabled", model.hrtfEnabled(),
                model::setHrtfEnabled);
        
        addButton(playbackBackendMessage(), left, y + 84, contentWidth, ROW_HEIGHT, button -> {
            model.setAudioPlaybackBackend(nextPlaybackBackend());
            button.setMessage(playbackBackendMessage());
        });
            }

    private void initUiTab(int left, int y, int contentWidth) {
        addToggle(left, y, contentWidth, "screen.minevoice.hud_enabled", model.hudEnabled(),
                model::setHudEnabled);
        addToggle(left, y + 21, contentWidth, "screen.minevoice.nameplate_icons_enabled", model.nameplateIconsEnabled(),
                model::setNameplateIconsEnabled);
        addButton(hudIconSizeMessage(), left, y + 42, contentWidth, ROW_HEIGHT, button -> {
            int current = model.hudIconSize();
            int next = current <= 16 ? 24 : (current <= 24 ? 32 : 16);
            model.setHudIconSize(next);
            button.setMessage(hudIconSizeMessage());
        });
        addButton(outOfSightIndicatorMessage(), left, y + 63, contentWidth, ROW_HEIGHT, button -> {
            model.setOutOfSightIndicatorMode((model.outOfSightIndicatorMode() + 1) % 3);
            button.setMessage(outOfSightIndicatorMessage());
        });
        addButton(occludedIndicatorMessage(), left, y + 84, contentWidth, ROW_HEIGHT, button -> {
            model.setOccludedIndicatorMode((model.occludedIndicatorMode() + 1) % 2);
            button.setMessage(occludedIndicatorMessage());
        });
    }

    private void initDebugTab(int left, int y, int contentWidth) {
        addButton(debugLevelMessage(), left, y, contentWidth, ROW_HEIGHT, button -> {
            model.setDebugInfoLevel(model.debugInfoLevel().next());
            button.setMessage(debugLevelMessage());
        });
        addToggle(left, y + 21, contentWidth, "screen.minevoice.debug_render_rays", model.debugRenderRays(),
                model::setDebugRenderRays);
        addButton(Component.translatable("screen.minevoice.print_debug_snapshot"),
                left, y + 42, contentWidth, ROW_HEIGHT, button -> {
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.displayClientMessage(debugSnapshot(), false);
                    }
                });
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int panelLeft = panelLeft();
        int panelTop = panelTop();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        Component title = Component.literal("MineVOICE");
        guiGraphics.drawCenteredString(font, title, panelLeft + panelWidth / 2, panelTop + 7, 0xFF404040);
        if (selectedTab == SettingsTab.AUDIO) {
            renderDeviceTestStatus(guiGraphics, panelLeft, panelTop, panelWidth, panelHeight);
        }
        if (selectedTab == SettingsTab.DEBUG) {
            renderDebugSummary(guiGraphics, panelLeft + 8, panelTop + 104, panelWidth - 16);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        MineVoicePanelStyle.render(graphics, panelLeft(), panelTop(), panelWidth(), panelHeight());
    }

    @Override
    public void onClose() {
        saveAndClose();
    }

    @Override
    public void tick() {
        super.tick();
        if (microphoneTestButton != null) {
            microphoneTestButton.setMessage(microphoneTestButtonMessage());
        }
    }

    private void addDeviceButton(int left, int y, int width, String labelKey, String titleKey, String current, List<AudioDevice> devices, DeviceSetter setter) {
        AudioDevice currentDevice = resolveDevice(devices, current);
        Component currentName = currentDevice.systemDefault()
                ? Component.translatable("screen.minevoice.system_default")
                : Component.literal(currentDevice.displayName());
        Component message = Component.translatable(labelKey).append(": ").append(currentName);
        addButton(message, left, y, width, ROW_HEIGHT, button ->
                Minecraft.getInstance().setScreen(new MineVoiceDeviceSelectScreen(this,
                        Component.translatable(titleKey), devices, current, setter::set)));
    }

    private void addToggle(int left, int y, int width, String labelKey, boolean initialValue, Consumer<Boolean> setter) {
        boolean[] selected = {initialValue};
        Button[] toggle = new Button[1];
        toggle[0] = Button.builder(toggleMessage(labelKey, selected[0]), button -> {
            selected[0] = !selected[0];
            setter.accept(selected[0]);
            toggle[0].setMessage(toggleMessage(labelKey, selected[0]));
        }).bounds(left, y, width, ROW_HEIGHT).build();
        addRenderableWidget(toggle[0]);
    }

    private Button addButton(Component message, int x, int y, int width, int height, Button.OnPress onPress) {
        Button button = Button.builder(message, onPress).bounds(x, y, width, height).build();
        addRenderableWidget(button);
        return button;
    }

    private Component toggleMessage(String labelKey, boolean enabled) {
        return Component.translatable(labelKey)
                .append(": ")
                .append(Component.translatable(enabled ? "options.on" : "options.off"));
    }

    private AudioDevice resolveDevice(List<AudioDevice> devices, String storedId) {
        for (AudioDevice device : devices) {
            if (device.id().equals(storedId) || (!device.systemDefault() && device.displayName().equals(storedId))) {
                return device;
            }
        }
        return AudioDevice.defaultDevice();
    }

    private int panelLeft() {
        return width / 2 - panelWidth() / 2;
    }

    private int panelTop() {
        return Math.max(4, height / 2 - panelHeight() / 2);
    }

    private int panelWidth() {
        return Math.min(PANEL_MAX_WIDTH, Math.max(250, width - 12));
    }

    private int panelHeight() {
        return Math.min(PANEL_MAX_HEIGHT, Math.max(190, height - 8));
    }

    private Component publicActivationModeMessage() {
        String key = model.activationMode() == VoiceActivationMode.PUSH_TO_TALK
                ? "screen.minevoice.activation_push_to_talk"
                : "screen.minevoice.activation_voice_activity";
        return Component.translatable("screen.minevoice.public_activation_mode").append(": ").append(Component.translatable(key));
    }

    private VoiceActivationMode nextActivationMode() {
        return model.activationMode() == VoiceActivationMode.PUSH_TO_TALK
                ? VoiceActivationMode.VOICE_ACTIVITY
                : VoiceActivationMode.PUSH_TO_TALK;
    }

    private Component groupActivationModeMessage() {
        String key = model.groupActivationMode() == VoiceActivationMode.PUSH_TO_TALK
                ? "screen.minevoice.activation_push_to_talk"
                : "screen.minevoice.activation_voice_activity";
        return Component.translatable("screen.minevoice.group_activation_mode").append(": ").append(Component.translatable(key));
    }

    private VoiceActivationMode nextGroupActivationMode() {
        return model.groupActivationMode() == VoiceActivationMode.PUSH_TO_TALK
                ? VoiceActivationMode.VOICE_ACTIVITY
                : VoiceActivationMode.PUSH_TO_TALK;
    }

    private Component hudIconSizeMessage() {
        return Component.translatable("screen.minevoice.hud_icon_size", model.hudIconSize());
    }

    private Component outOfSightIndicatorMessage() {
        String key = switch (model.outOfSightIndicatorMode()) {
            case 1 -> "screen.minevoice.indicator.avatar";
            case 2 -> "screen.minevoice.indicator.arrow";
            default -> "screen.minevoice.indicator.none";
        };
        return Component.translatable("screen.minevoice.out_of_sight_indicator").append(": ").append(Component.translatable(key));
    }

    private Component occludedIndicatorMessage() {
        String key = switch (model.occludedIndicatorMode()) {
            case 1 -> "screen.minevoice.indicator.avatar";
            default -> "screen.minevoice.indicator.none";
        };
        return Component.translatable("screen.minevoice.occluded_indicator").append(": ").append(Component.translatable(key));
    }

    


    private Component playbackBackendMessage() {
        return Component.translatable("screen.minevoice.audio_playback_backend")
                .append(": ")
                .append(Component.literal(model.audioPlaybackBackend()));
    }

    private String nextPlaybackBackend() {
        return switch (model.audioPlaybackBackend()) {
            case "auto" -> "java-sound";
            case "java-sound" -> "openal";
            default -> "auto";
        };
    }

    private Component debugLevelMessage() {
        DebugInfoLevel level = model.debugInfoLevel();
        return Component.translatable("screen.minevoice.debug_level")
                .append(": ")
                .append(Component.translatable(level.translationKey()));
    }

    private Component debugSnapshot() {
        return Component.literal("MineVOICE: "
                + "mode=" + model.activationMode()
                + " groupMode=" + model.groupActivationMode()
                + " mic=" + deviceSummary(model.microphoneDevice())
                + " out=" + deviceSummary(model.outputDevice())
                + " playbackBackend=" + model.audioPlaybackBackend()
                + " hud=" + model.hudEnabled()
                + " debug=" + model.debugInfoLevel()
                + " "
                + MineVoiceClientBootstrap.debugConnectionSummary());
    }

    private void renderDebugSummary(GuiGraphics graphics, int left, int top, int width) {
        VoiceNetworkStats networkStats = MineVoiceClientBootstrap.voiceNetworkStats();
        VoicePlaybackStats playbackStats = MineVoiceClientBootstrap.voicePlaybackStats();
        VoiceSpatialDebugSnapshot spatialDebug = MineVoiceClientBootstrap.voiceSpatialDebugSnapshot();
        String[] lines = {
                "状态: " + networkStats.status() + "  " + networkStats.endpoint(),
                "协议: " + networkStats.protocolVersion() + "  codec: " + networkStats.codec()
                        + "  playback: " + playbackStats.backendName(),
                String.format(java.util.Locale.ROOT, "UDP: %.1f/%.1f KiB  packets: %d/%d",
                        networkStats.udpSentKiB(),
                        networkStats.udpReceivedKiB(),
                        networkStats.udpPacketsSent(),
                        networkStats.udpPacketsReceived()),
                String.format(java.util.Locale.ROOT, "Voice: %.1f/%.1f KiB  frames: %d/%d  fps: %.1f/%.1f",
                        networkStats.voiceSentKiB(),
                        networkStats.voiceReceivedKiB(),
                        networkStats.voiceFramesSent(),
                        networkStats.voiceFramesReceived(),
                        networkStats.voiceFramesSentPerSecond(),
                        networkStats.voiceFramesReceivedPerSecond()),
                "Jitter: speakers " + playbackStats.activeSpeakers()
                        + " buffered " + playbackStats.bufferedFrames()
                        + " late " + playbackStats.latePackets()
                        + " dropped " + playbackStats.droppedPackets()
                        + " missing " + playbackStats.missingFrames(),
                "Spatial: " + spatialDebug.summary(),
                "Compat: " + SoundPhysicsCompat.backendName()
        };
        int y = top;
        for (String line : lines) {
            String trimmed = font.plainSubstrByWidth(line, width);
            graphics.drawString(font, trimmed, left, y, 0xFF404040, false);
            y += 12;
        }
    }

    private static String deviceSummary(String deviceId) {
        return "default".equalsIgnoreCase(deviceId) ? "default" : "custom";
    }

    private void startOutputTest() {
        if (deviceTestRunning) {
            return;
        }
        deviceTestRunning = true;
        deviceTestStatusKey = "screen.minevoice.device_test_output_starting";
        MineVoiceClientBootstrap.suspendAudioForDeviceTest();
        JavaSoundAudioDeviceTester.playTone(model.toSettings(), this::finishDeviceTest);
    }

    private void toggleMicrophoneTest() {
        if (deviceTestRunning) {
            if (microphoneTest != null) {
                microphoneTest.stop();
            }
            return;
        }
        deviceTestRunning = true;
        microphoneTestLevel = 0.0F;
        deviceTestStatusKey = "screen.minevoice.device_test_input_starting";
        MineVoiceClientBootstrap.suspendAudioForDeviceTest();
        microphoneTest = JavaSoundAudioDeviceTester.startInputLevelTest(
                model.toSettings(),
                update -> {
                    microphoneTestLevel = update.level();
                    deviceTestStatusKey = update.statusKey();
                },
                () -> finishDeviceTest(deviceTestStatusKey)
        );
    }

    private void stopDeviceTest() {
        if (microphoneTest != null && microphoneTest.running()) {
            microphoneTest.stop();
        }
    }

    private void finishDeviceTest(String statusKey) {
        deviceTestStatusKey = statusKey;
        microphoneTest = null;
        deviceTestRunning = false;
        microphoneTestLevel = 0.0F;
        MineVoiceClientBootstrap.resumeAudioAfterDeviceTest();
    }

    private Component microphoneTestButtonMessage() {
        return Component.translatable(deviceTestRunning
                ? "screen.minevoice.test_input_stop"
                : "screen.minevoice.test_input");
    }

    private void renderDeviceTestStatus(GuiGraphics graphics, int panelLeft, int panelTop, int panelWidth, int panelHeight) {
        int statusY = panelTop + panelHeight - 39;
        if (deviceTestRunning && microphoneTest != null) {
            int meterLeft = panelLeft + 6;
            int meterWidth = panelWidth - 12;
            int meterTop = statusY - 8;
            int filledWidth = Math.round(meterWidth * microphoneTestLevel);
            graphics.fill(meterLeft, meterTop, meterLeft + meterWidth, meterTop + 4, 0xFF151A20);
            graphics.fill(meterLeft, meterTop, meterLeft + filledWidth, meterTop + 4, microphoneTestLevel > 0.70F ? 0xFFE8B14B : 0xFF63D597);
            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.minevoice.device_test_input_level", Math.round(microphoneTestLevel * 100.0F)),
                    panelLeft + panelWidth / 2,
                    statusY,
                    0xFFFFFFFF
            );
            return;
        }
        if (deviceTestStatusKey != null) {
            graphics.drawCenteredString(font, Component.translatable(deviceTestStatusKey), panelLeft + panelWidth / 2, statusY, 0xFFA0A0A0);
        }
    }

    private void saveAndClose() {
        stopDeviceTest();
        uiController.saveAndClose(model);
        MineVoiceClientBootstrap.syncLocalVoiceStatus();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void reset() {
        stopDeviceTest();
        model.resetToDefaults();
        rebuildWidgets();
    }

    private void closeWithoutSaving() {
        stopDeviceTest();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private enum SettingsTab {
        AUDIO("screen.minevoice.tab.audio"),
        VOICE("screen.minevoice.tab.voice"),
        UI("screen.minevoice.tab.ui"),
        DEBUG("screen.minevoice.tab.debug");

        private final String titleKey;

        SettingsTab(String titleKey) {
            this.titleKey = titleKey;
        }

        private Component title() {
            return Component.translatable(titleKey);
        }

    }

    private interface DeviceSetter {
        void set(String deviceId);
    }

    private abstract static class VolumeSlider extends AbstractSliderButton {
        private final String labelKey;

        private VolumeSlider(int x, int y, int width, int height, String labelKey, float value) {
            super(x, y, width, height, Component.empty(), value);
            this.labelKey = labelKey;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(labelKey, Math.round(value * 100.0D)));
        }



    }
}

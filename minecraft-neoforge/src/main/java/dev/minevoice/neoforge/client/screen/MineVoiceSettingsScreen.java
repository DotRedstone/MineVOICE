package dev.minevoice.neoforge.client.screen;

import dev.minevoice.neoforge.client.AudioDevice;
import dev.minevoice.neoforge.client.ClientAudioDeviceScanner;
import dev.minevoice.neoforge.client.MineVoiceClientBootstrap;
import dev.minevoice.neoforge.client.MineVoiceClientUiController;
import dev.minevoice.neoforge.client.VoiceActivationMode;
import dev.minevoice.neoforge.client.audio.JavaSoundAudioDeviceTester;
import dev.minevoice.neoforge.client.ui.MineVoiceSettingsScreenModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class MineVoiceSettingsScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 248;
    private static final int PANEL_MAX_HEIGHT = 220;
    private static final int ROW_HEIGHT = 20;

    private final Screen parent;
    private final MineVoiceClientUiController uiController;
    private final MineVoiceSettingsScreenModel model;
    private final List<AudioDevice> inputDevices = ClientAudioDeviceScanner.inputDevices();
    private final List<AudioDevice> outputDevices = ClientAudioDeviceScanner.outputDevices();

    private SettingsTab selectedTab = SettingsTab.AUDIO;
    private EditBox pushToTalkKey;
    private volatile Component deviceTestStatus;

    public MineVoiceSettingsScreen(Screen parent, MineVoiceClientUiController uiController) {
        super(Component.translatable("screen.minevoice.settings"));
        this.parent = parent;
        this.uiController = uiController;
        this.model = uiController.openSettingsScreen();
    }

    @Override
    protected void init() {
        syncOpenFields();
        clearWidgets();
        pushToTalkKey = null;

        int panelLeft = panelLeft();
        int panelTop = panelTop();
        int contentLeft = panelLeft + 6;
        int contentWidth = panelWidth() - 12;

        int tabGap = 3;
        int tabWidth = (contentWidth - tabGap * (SettingsTab.values().length - 1)) / SettingsTab.values().length;
        for (int index = 0; index < SettingsTab.values().length; index++) {
            SettingsTab tab = SettingsTab.values()[index];
            Button tabButton = Button.builder(tab.title(), button -> {
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
            case CONTROLS -> initControlsTab(contentLeft, rowY, contentWidth);
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
                    JavaSoundAudioDeviceTester.playTone(model.toSettings(), this::setDeviceTestStatus);
                });
        addButton(Component.translatable("screen.minevoice.test_input"),
                left + (contentWidth + 3) / 2, y + 105, (contentWidth - 3) / 2, ROW_HEIGHT, button -> {
                    JavaSoundAudioDeviceTester.probeInput(model.toSettings(), this::setDeviceTestStatus);
                });
    }

    private void initVoiceTab(int left, int y, int contentWidth) {
        addButton(activationModeMessage(), left, y, contentWidth, ROW_HEIGHT, button -> {
                    model.setActivationMode(nextActivationMode());
                    button.setMessage(activationModeMessage());
                });
        addRenderableWidget(new VolumeSlider(left, y + 21, contentWidth, ROW_HEIGHT, "screen.minevoice.voice_activation_threshold", model.voiceActivationThreshold()) {
            @Override
            protected void applyValue() {
                model.setVoiceActivationThreshold((float) value);
            }
        });
        addToggle(left, y + 42, contentWidth, "screen.minevoice.spatial_audio", model.spatialAudioEnabled(),
                model::setSpatialAudioEnabled);
        addToggle(left, y + 63, contentWidth, "screen.minevoice.muted", model.muted(), model::setMuted);
        addToggle(left, y + 84, contentWidth, "screen.minevoice.deafened", model.deafened(), model::setDeafened);
    }

    private void initControlsTab(int left, int y, int contentWidth) {
        pushToTalkKey = new EditBox(font, left, y + 17, contentWidth, 20, Component.translatable("screen.minevoice.push_to_talk"));
        pushToTalkKey.setMaxLength(32);
        pushToTalkKey.setValue(model.pushToTalkKey());
        addRenderableWidget(pushToTalkKey);

        addButton(Component.translatable("screen.minevoice.set_push_to_talk_default"),
                left, y + 42, contentWidth, ROW_HEIGHT, button -> {
                    model.setPushToTalkKey("V");
                    pushToTalkKey.setValue("V");
                });
    }

    private void initDebugTab(int left, int y, int contentWidth) {
        addToggle(left, y, contentWidth, "screen.minevoice.debug_info", model.showDebugConnectionInfo(),
                model::setShowDebugConnectionInfo);
        addButton(Component.translatable("screen.minevoice.print_debug_snapshot"),
                left, y + 24, contentWidth, ROW_HEIGHT, button -> {
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
        int contentLeft = panelLeft + 6;
        Component title = Component.literal("MineVOICE");
        guiGraphics.drawCenteredString(font, title, panelLeft + panelWidth / 2, panelTop + 7, 0xFFFFFFFF);
        if (selectedTab == SettingsTab.AUDIO && deviceTestStatus != null) {
            guiGraphics.drawCenteredString(font, deviceTestStatus, panelLeft + panelWidth / 2, panelTop + 177, 0xFFFFFFFF);
        }
        if (selectedTab == SettingsTab.CONTROLS && pushToTalkKey != null) {
            guiGraphics.drawString(font, Component.translatable("screen.minevoice.push_to_talk"), pushToTalkKey.getX(), pushToTalkKey.getY() - 11, 0xFFA0A0A0);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int padding = 8;
        renderMenuBackground(
                graphics,
                panelLeft() - padding,
                panelTop() - padding,
                panelWidth() + padding * 2,
                panelHeight() + padding * 2
        );
    }

    @Override
    public void onClose() {
        closeWithoutSaving();
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
        return Math.min(PANEL_MAX_WIDTH, Math.max(220, width - 12));
    }

    private int panelHeight() {
        return Math.min(PANEL_MAX_HEIGHT, Math.max(180, height - 8));
    }

    private Component activationModeMessage() {
        String key = model.activationMode() == VoiceActivationMode.PUSH_TO_TALK
                ? "screen.minevoice.activation_push_to_talk"
                : "screen.minevoice.activation_voice_activity";
        return Component.translatable("screen.minevoice.activation_mode").append(": ").append(Component.translatable(key));
    }

    private VoiceActivationMode nextActivationMode() {
        return model.activationMode() == VoiceActivationMode.PUSH_TO_TALK
                ? VoiceActivationMode.VOICE_ACTIVITY
                : VoiceActivationMode.PUSH_TO_TALK;
    }

    private Component debugSnapshot() {
        return Component.literal("MineVOICE: mode=" + model.activationMode()
                + ", mic=" + model.microphoneDevice()
                + ", out=" + model.outputDevice()
                + ", muted=" + model.muted()
                + ", deafened=" + model.deafened());
    }

    private void setDeviceTestStatus(String translationKey) {
        deviceTestStatus = Component.translatable(translationKey);
    }

    private void saveAndClose() {
        syncOpenFields();
        uiController.saveAndClose(model);
        MineVoiceClientBootstrap.applyPushToTalkBinding(model.pushToTalkKey());
        MineVoiceClientBootstrap.syncLocalVoiceStatus();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void reset() {
        model.resetToDefaults();
        pushToTalkKey = null;
        rebuildWidgets();
    }

    private void closeWithoutSaving() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void syncOpenFields() {
        if (pushToTalkKey != null) {
            model.setPushToTalkKey(pushToTalkKey.getValue());
        }
    }

    private enum SettingsTab {
        AUDIO("screen.minevoice.tab.audio"),
        VOICE("screen.minevoice.tab.voice"),
        CONTROLS("screen.minevoice.tab.controls"),
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

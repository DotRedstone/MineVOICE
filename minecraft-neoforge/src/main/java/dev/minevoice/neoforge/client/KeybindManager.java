package dev.minevoice.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class KeybindManager {
    public static final String OPEN_SETTINGS_KEY = "key.minevoice.open_settings";
    public static final String PUSH_TO_TALK_KEY = "key.minevoice.push_to_talk";
    public static final String GROUP_TALK_KEY = "key.minevoice.group_talk";
    public static final String CATEGORY = "key.categories.minevoice";

    private final KeyMapping openSettingsMapping = new KeyMapping(
            OPEN_SETTINGS_KEY,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY
    );

    private final KeyMapping pushToTalkMapping = new KeyMapping(
            PUSH_TO_TALK_KEY,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    private final KeyMapping groupTalkMapping = new KeyMapping(
            GROUP_TALK_KEY,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    private final ClientVoiceConnectionManager voiceConnectionManager;
    private final Runnable settingsScreenOpener;
    private boolean settingsKeyWasDown;

    public KeybindManager(
            ClientVoiceConnectionManager voiceConnectionManager,
            Runnable settingsScreenOpener
    ) {
        this.voiceConnectionManager = voiceConnectionManager;
        this.settingsScreenOpener = settingsScreenOpener;
    }

    public void register() {
        // TODO(minevoice): register OPEN_SETTINGS_KEY and PUSH_TO_TALK_KEY with the NeoForge client event bus.
    }

    public KeyMapping openSettingsMapping() {
        return openSettingsMapping;
    }

    public KeyMapping pushToTalkMapping() {
        return pushToTalkMapping;
    }

    public KeyMapping groupTalkMapping() {
        return groupTalkMapping;
    }

    public void handleClientTick() {
        boolean settingsKeyDown = openSettingsMapping.isDown();
        if (openSettingsMapping.consumeClick() || (settingsKeyDown && !settingsKeyWasDown)) {
            handleOpenSettingsPressed();
        }
        settingsKeyWasDown = settingsKeyDown;
        handlePushToTalkPressed(pushToTalkMapping.isDown());
        voiceConnectionManager.setGroupPushToTalkDown(groupTalkMapping.isDown());
    }

    public void handleOpenSettingsPressed() {
        settingsScreenOpener.run();
    }

    public void handlePushToTalkPressed(boolean pressed) {
        voiceConnectionManager.setPushToTalkDown(pressed);
    }

    public void applyPushToTalkBinding(String configuredKey) {
        String normalized = configuredKey.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return;
        }
        InputConstants.Key key = InputConstants.getKey("key.keyboard." + normalized);
        pushToTalkMapping.setKey(key);
        KeyMapping.resetMapping();
    }
}

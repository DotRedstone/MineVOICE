package dev.minevoice.neoforge.client.ui;

import dev.minevoice.neoforge.client.MineVoiceClientUiController;

public final class MineVoiceSettingsButtonBinding {
    public static final String LABEL_KEY = "button.minevoice.settings";

    private final MineVoiceClientUiController uiController;

    public MineVoiceSettingsButtonBinding(MineVoiceClientUiController uiController) {
        this.uiController = uiController;
    }

    public MineVoiceSettingsScreenModel click() {
        return uiController.openSettingsScreen();
    }
}

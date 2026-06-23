package dev.minevoice.neoforge.client;

import dev.minevoice.neoforge.client.ui.MineVoiceSettingsScreenModel;

public final class MineVoiceClientUiController {
    private final ClientSettingsStore settingsStore;
    private MineVoiceSettingsScreenModel currentScreen;

    public MineVoiceClientUiController(ClientSettingsStore settingsStore) {
        this.settingsStore = settingsStore;
    }

    public MineVoiceSettingsScreenModel openSettingsScreen() {
        currentScreen = MineVoiceSettingsScreenModel.from(settingsStore.load());
        // TODO(minevoice): replace this model handoff with Minecraft#setScreen after NeoForge version is selected.
        return currentScreen;
    }

    public void saveAndClose(MineVoiceSettingsScreenModel screenModel) {
        settingsStore.save(screenModel.toSettings());
        currentScreen = null;
    }

    public MineVoiceSettingsScreenModel currentScreen() {
        return currentScreen;
    }
}

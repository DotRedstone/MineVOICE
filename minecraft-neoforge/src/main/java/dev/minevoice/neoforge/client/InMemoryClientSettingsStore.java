package dev.minevoice.neoforge.client;

public final class InMemoryClientSettingsStore implements ClientSettingsStore {
    private ClientAudioSettings settings = ClientAudioSettings.defaults();

    @Override
    public ClientAudioSettings load() {
        return settings;
    }

    @Override
    public void save(ClientAudioSettings settings) {
        this.settings = settings;
    }
}

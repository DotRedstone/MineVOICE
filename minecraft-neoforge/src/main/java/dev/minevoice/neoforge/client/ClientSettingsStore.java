package dev.minevoice.neoforge.client;

/**
 * Stores client-only settings used by the in-game MineVOICE configuration screen.
 */
public interface ClientSettingsStore {
    ClientAudioSettings load();

    void save(ClientAudioSettings settings);
}

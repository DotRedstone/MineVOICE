package dev.minevoice.neoforge.client.ui;

import java.util.List;

public final class MineVoiceSettingsScreenLayout {
    private final List<String> sections = List.of(
            "audio_devices",
            "voice_controls",
            "spatial_audio",
            "debug"
    );

    public List<String> sections() {
        return sections;
    }

    public List<String> actions() {
        return List.of(
                MineVoiceSettingsAction.SAVE.name(),
                MineVoiceSettingsAction.CANCEL.name(),
                MineVoiceSettingsAction.RESET_TO_DEFAULTS.name()
        );
    }
}

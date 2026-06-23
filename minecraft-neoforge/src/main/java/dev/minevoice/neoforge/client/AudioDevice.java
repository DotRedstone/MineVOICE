package dev.minevoice.neoforge.client;

import javax.sound.sampled.Mixer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record AudioDevice(String id, String displayName, boolean systemDefault) {
    public static final String DEFAULT_ID = "default";

    public static AudioDevice defaultDevice() {
        return new AudioDevice(DEFAULT_ID, "", true);
    }

    public static AudioDevice fromMixer(Mixer.Info mixerInfo) {
        String raw = mixerInfo.getName()
                + "\u0000" + mixerInfo.getVendor()
                + "\u0000" + mixerInfo.getDescription()
                + "\u0000" + mixerInfo.getVersion();
        String id = "mixer:" + Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return new AudioDevice(id, mixerInfo.getName(), false);
    }

    public boolean matches(Mixer.Info mixerInfo) {
        return !systemDefault && id.equals(fromMixer(mixerInfo).id);
    }
}

package dev.minevoice.neoforge.client.audio;

import dev.minevoice.neoforge.client.AudioDevice;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;

public final class JavaSoundDeviceSelector {
    private JavaSoundDeviceSelector() {
    }

    public static Mixer.Info selectMixer(String configuredId, DataLine.Info lineInfo) {
        if (configuredId != null && !AudioDevice.DEFAULT_ID.equalsIgnoreCase(configuredId)) {
            for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                if (AudioDevice.fromMixer(mixerInfo).id().equals(configuredId)
                        && AudioSystem.getMixer(mixerInfo).isLineSupported(lineInfo)) {
                    return mixerInfo;
                }
            }
            for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                if (mixerInfo.getName().equals(configuredId) && AudioSystem.getMixer(mixerInfo).isLineSupported(lineInfo)) {
                    return mixerInfo;
                }
            }
        }
        return null;
    }

    public static DataLine getLine(String configuredId, DataLine.Info lineInfo) {
        try {
            Mixer.Info mixerInfo = selectMixer(configuredId, lineInfo);
            if (mixerInfo == null) {
                return (DataLine) AudioSystem.getLine(lineInfo);
            }
            return (DataLine) AudioSystem.getMixer(mixerInfo).getLine(lineInfo);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to open audio line for device: " + configuredId, exception);
        }
    }
}

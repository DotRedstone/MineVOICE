package dev.minevoice.neoforge.client.audio;

import dev.minevoice.neoforge.client.AudioDevice;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import java.util.StringJoiner;

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

    public static String lineSignature(String configuredId, DataLine.Info lineInfo) {
        Mixer.Info selected = selectMixer(configuredId, lineInfo);
        if (selected != null) {
            return "selected:" + AudioDevice.fromMixer(selected).id();
        }
        try {
            Mixer defaultMixer = AudioSystem.getMixer(null);
            if (defaultMixer.isLineSupported(lineInfo)) {
                return "default:" + AudioDevice.fromMixer(defaultMixer.getMixerInfo()).id();
            }
        } catch (Exception ignored) {
            // Some Java Sound providers do not expose the default mixer through getMixer(null).
        }

        StringJoiner joiner = new StringJoiner("|", "default-supported:", "");
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(lineInfo)) {
                joiner.add(AudioDevice.fromMixer(mixerInfo).id());
            }
        }
        return joiner.toString();
    }
}

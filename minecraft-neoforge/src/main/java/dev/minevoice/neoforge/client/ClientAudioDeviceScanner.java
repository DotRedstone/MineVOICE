package dev.minevoice.neoforge.client;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.List;

public final class ClientAudioDeviceScanner {
    private static final AudioFormat CAPTURE_FORMAT = new AudioFormat(48_000.0F, 16, 1, true, false);
    private static final AudioFormat PLAYBACK_FORMAT = new AudioFormat(48_000.0F, 16, 2, true, false);

    private ClientAudioDeviceScanner() {
    }

    public static List<AudioDevice> devices() {
        return audioDevices(
                new DataLine.Info(TargetDataLine.class, CAPTURE_FORMAT),
                new DataLine.Info(SourceDataLine.class, PLAYBACK_FORMAT)
        );
    }

    public static List<AudioDevice> inputDevices() {
        return audioDevices(new DataLine.Info(TargetDataLine.class, CAPTURE_FORMAT));
    }

    public static List<AudioDevice> outputDevices() {
        return audioDevices(new DataLine.Info(SourceDataLine.class, PLAYBACK_FORMAT));
    }

    private static List<AudioDevice> audioDevices(DataLine.Info... supportedLines) {
        List<AudioDevice> devices = new ArrayList<>();
        devices.add(AudioDevice.defaultDevice());
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(info);
            boolean supported = false;
            for (DataLine.Info supportedLine : supportedLines) {
                if (mixer.isLineSupported(supportedLine)) {
                    supported = true;
                    break;
                }
            }
            AudioDevice device = AudioDevice.fromMixer(info);
            if (supported && !device.displayName().isBlank() && devices.stream().noneMatch(existing -> existing.id().equals(device.id()))) {
                devices.add(device);
            }
        }
        return List.copyOf(devices);
    }
}

package dev.minevoice.neoforge.client.audio;

import dev.minevoice.neoforge.client.ClientAudioSettings;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.function.Consumer;

public final class JavaSoundAudioDeviceTester {
    private static final int SAMPLE_RATE = 48_000;

    private JavaSoundAudioDeviceTester() {
    }

    public static void playTone(ClientAudioSettings settings, Consumer<String> resultConsumer) {
        Thread thread = new Thread(() -> {
            try (SourceDataLine line = openOutput(settings)) {
                line.start();
                byte[] samples = sineWave(440.0D, 0.25D, settings.masterVolume() * settings.voiceChatVolume());
                line.write(samples, 0, samples.length);
                line.drain();
                resultConsumer.accept("screen.minevoice.device_test_output_ok");
            } catch (RuntimeException exception) {
                resultConsumer.accept("screen.minevoice.device_test_failed");
            }
        }, "minevoice-output-device-test");
        thread.setDaemon(true);
        thread.start();
    }

    public static void probeInput(ClientAudioSettings settings, Consumer<String> resultConsumer) {
        Thread thread = new Thread(() -> {
            try (TargetDataLine line = openInput(settings)) {
                line.start();
                byte[] buffer = new byte[960];
                line.read(buffer, 0, buffer.length);
                resultConsumer.accept("screen.minevoice.device_test_input_ok");
            } catch (RuntimeException exception) {
                resultConsumer.accept("screen.minevoice.device_test_failed");
            }
        }, "minevoice-input-device-test");
        thread.setDaemon(true);
        thread.start();
    }

    private static SourceDataLine openOutput(ClientAudioSettings settings) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, JavaSoundVoiceAudioPipeline.PLAYBACK_FORMAT);
        SourceDataLine line = (SourceDataLine) JavaSoundDeviceSelector.getLine(settings.outputDevice(), info);
        try {
            line.open(JavaSoundVoiceAudioPipeline.PLAYBACK_FORMAT);
            return line;
        } catch (Exception exception) {
            line.close();
            throw new IllegalStateException("failed to open output test line", exception);
        }
    }

    private static TargetDataLine openInput(ClientAudioSettings settings) {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, JavaSoundVoiceAudioPipeline.CAPTURE_FORMAT);
        TargetDataLine line = (TargetDataLine) JavaSoundDeviceSelector.getLine(settings.microphoneDevice(), info);
        try {
            line.open(JavaSoundVoiceAudioPipeline.CAPTURE_FORMAT);
            return line;
        } catch (Exception exception) {
            line.close();
            throw new IllegalStateException("failed to open input test line", exception);
        }
    }

    private static byte[] sineWave(double frequency, double seconds, float volume) {
        int sampleCount = (int) (SAMPLE_RATE * seconds);
        byte[] pcm = new byte[sampleCount * 4];
        for (int index = 0; index < sampleCount; index++) {
            double phase = 2.0D * Math.PI * frequency * index / SAMPLE_RATE;
            short sample = (short) Math.round(Math.sin(phase) * Short.MAX_VALUE * Math.min(0.25F, volume));
            int offset = index * 4;
            pcm[offset] = (byte) (sample & 0xFF);
            pcm[offset + 1] = (byte) ((sample >>> 8) & 0xFF);
            pcm[offset + 2] = (byte) (sample & 0xFF);
            pcm[offset + 3] = (byte) ((sample >>> 8) & 0xFF);
        }
        return pcm;
    }
}

package dev.minevoice.neoforge.client.audio;

import dev.minevoice.neoforge.client.ClientAudioSettings;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class JavaSoundAudioDeviceTester {
    private static final int SAMPLE_RATE = 48_000;
    private static final int INPUT_TEST_BUFFER_BYTES = 1_920;
    private static final long INPUT_TEST_DURATION_MILLIS = 10_000L;

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

    public static InputTestSession startInputLevelTest(
            ClientAudioSettings settings,
            Consumer<InputTestUpdate> updateConsumer,
            Runnable completionCallback
    ) {
        InputLevelTest test = new InputLevelTest(settings, updateConsumer, completionCallback);
        test.start();
        return test;
    }

    public record InputTestUpdate(boolean active, float level, String statusKey) {
    }

    public interface InputTestSession {
        boolean running();

        void stop();
    }

    private static SourceDataLine openOutput(ClientAudioSettings settings) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, ClientVoiceAudioPipeline.PLAYBACK_FORMAT);
        SourceDataLine line = (SourceDataLine) JavaSoundDeviceSelector.getLine(settings.outputDevice(), info);
        try {
            line.open(ClientVoiceAudioPipeline.PLAYBACK_FORMAT);
            return line;
        } catch (Exception exception) {
            line.close();
            throw new IllegalStateException("failed to open output test line", exception);
        }
    }

    private static TargetDataLine openInput(ClientAudioSettings settings) {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, ClientVoiceAudioPipeline.CAPTURE_FORMAT);
        TargetDataLine line = (TargetDataLine) JavaSoundDeviceSelector.getLine(settings.microphoneDevice(), info);
        try {
            line.open(ClientVoiceAudioPipeline.CAPTURE_FORMAT);
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

    private static final class InputLevelTest implements InputTestSession {
        private final ClientAudioSettings settings;
        private final Consumer<InputTestUpdate> updateConsumer;
        private final Runnable completionCallback;
        private final AtomicBoolean running = new AtomicBoolean();
        private volatile TargetDataLine line;

        private InputLevelTest(
                ClientAudioSettings settings,
                Consumer<InputTestUpdate> updateConsumer,
                Runnable completionCallback
        ) {
            this.settings = settings;
            this.updateConsumer = updateConsumer;
            this.completionCallback = completionCallback;
        }

        private void start() {
            if (!running.compareAndSet(false, true)) {
                return;
            }
            Thread thread = new Thread(this::run, "minevoice-input-device-test");
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public boolean running() {
            return running.get();
        }

        @Override
        public void stop() {
            if (!running.compareAndSet(true, false)) {
                return;
            }
            TargetDataLine currentLine = line;
            if (currentLine != null) {
                currentLine.stop();
                currentLine.close();
            }
        }

        private void run() {
            String completionKey = "screen.minevoice.device_test_input_finished";
            updateConsumer.accept(new InputTestUpdate(true, 0.0F, "screen.minevoice.device_test_input_starting"));
            try (TargetDataLine inputLine = openInput(settings)) {
                line = inputLine;
                if (!running.get()) {
                    completionKey = "screen.minevoice.device_test_input_stopped";
                    return;
                }
                inputLine.start();
                byte[] buffer = new byte[INPUT_TEST_BUFFER_BYTES];
                long deadline = System.currentTimeMillis() + INPUT_TEST_DURATION_MILLIS;
                long nextUpdateAt = 0L;
                while (running.get() && System.currentTimeMillis() < deadline) {
                    int read = inputLine.read(buffer, 0, buffer.length);
                    if (read <= 0) {
                        continue;
                    }
                    long now = System.nanoTime();
                    if (now >= nextUpdateAt) {
                        updateConsumer.accept(new InputTestUpdate(true, peakLevel(buffer, read), "screen.minevoice.device_test_input_listening"));
                        nextUpdateAt = now + 50_000_000L;
                    }
                }
                if (!running.get()) {
                    completionKey = "screen.minevoice.device_test_input_stopped";
                }
            } catch (RuntimeException exception) {
                completionKey = running.get()
                        ? "screen.minevoice.device_test_input_failed"
                        : "screen.minevoice.device_test_input_stopped";
            } finally {
                line = null;
                running.set(false);
                updateConsumer.accept(new InputTestUpdate(false, 0.0F, completionKey));
                completionCallback.run();
            }
        }

        private static float peakLevel(byte[] pcm, int bytes) {
            int peak = 0;
            for (int index = 0; index + 1 < bytes; index += 2) {
                int sample = (pcm[index + 1] << 8) | (pcm[index] & 0xFF);
                peak = Math.max(peak, Math.abs(sample));
            }
            return Math.min(1.0F, peak / 32768.0F);
        }
    }
}

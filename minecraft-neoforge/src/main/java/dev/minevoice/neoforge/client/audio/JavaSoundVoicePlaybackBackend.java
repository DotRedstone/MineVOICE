package dev.minevoice.neoforge.client.audio;

import dev.minevoice.neoforge.client.ClientAudioSettings;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public final class JavaSoundVoicePlaybackBackend implements VoicePlaybackBackend {
    public static final String NAME = "java-sound";

    private final String outputDevice;
    private final DataLine.Info lineInfo;
    private final String lineSignature;
    private final SourceDataLine line;

    private JavaSoundVoicePlaybackBackend(String outputDevice, DataLine.Info lineInfo, String lineSignature, SourceDataLine line) {
        this.outputDevice = outputDevice;
        this.lineInfo = lineInfo;
        this.lineSignature = lineSignature;
        this.line = line;
    }

    public static JavaSoundVoicePlaybackBackend open(ClientAudioSettings settings, AudioFormat format, int bufferBytes) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        String signature = JavaSoundDeviceSelector.lineSignature(settings.outputDevice(), info);
        SourceDataLine line = (SourceDataLine) JavaSoundDeviceSelector.getLine(settings.outputDevice(), info);
        try {
            line.open(format, bufferBytes);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to open output line", exception);
        }
        return new JavaSoundVoicePlaybackBackend(settings.outputDevice(), info, signature, line);
    }

    @Override
    public String backendName() {
        return NAME;
    }

    @Override
    public void start() {
        line.start();
    }

    @Override
    public void writeStereoFrame(byte[] pcm, int offset, int length) {
        line.write(pcm, offset, length);
    }

    @Override
    public boolean matches(ClientAudioSettings settings) {
        return outputDevice.equals(settings.outputDevice())
                && lineSignature.equals(JavaSoundDeviceSelector.lineSignature(settings.outputDevice(), lineInfo));
    }

    @Override
    public void close() {
        line.stop();
        line.close();
    }
}

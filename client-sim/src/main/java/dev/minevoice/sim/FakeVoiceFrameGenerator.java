package dev.minevoice.sim;

import dev.minevoice.common.audio.VoiceAudioFormat;
import dev.minevoice.common.audio.VoiceCodec;
import dev.minevoice.common.audio.VoiceCodecFactory;
import dev.minevoice.common.protocol.VoiceFrame;
import dev.minevoice.common.protocol.VoiceChannel;

import java.time.Instant;
import java.util.UUID;

public final class FakeVoiceFrameGenerator {
    private final VoiceAudioFormat format;
    private final VoiceCodec codec;
    private final int samplesPerFrame;
    private final int pcmBytesPerFrame;

    public FakeVoiceFrameGenerator() {
        this("opus");
    }

    public FakeVoiceFrameGenerator(String codecName) {
        this(codecName, VoiceAudioFormat.narrowbandVoice());
    }

    public FakeVoiceFrameGenerator(String codecName, VoiceAudioFormat format) {
        this.format = format;
        this.codec = VoiceCodecFactory.create(codecName);
        this.samplesPerFrame = format.sampleRate() * format.frameDurationMillis() / 1_000;
        this.pcmBytesPerFrame = samplesPerFrame * format.channels() * Short.BYTES;
    }

    public VoiceFrame generate(UUID playerId, long sequence) {
        return generate(playerId, sequence, VoiceChannel.PROXIMITY);
    }

    public VoiceFrame generate(UUID playerId, long sequence, VoiceChannel channel) {
        byte[] pcm = generatePcm(playerId, sequence);
        return new VoiceFrame(
                playerId,
                sequence,
                Instant.now().toEpochMilli(),
                codec.encode(pcm),
                format.sampleRate(),
                format.channels(),
                channel
        );
    }

    public String codecName() {
        return codec.codecName();
    }

    public int pcmBytesPerFrame() {
        return pcmBytesPerFrame;
    }

    private byte[] generatePcm(UUID playerId, long sequence) {
        byte[] pcm = new byte[pcmBytesPerFrame];
        double frequency = 220.0D + Math.abs(playerId.hashCode() % 320);
        long sampleOffset = sequence * samplesPerFrame;
        for (int sampleIndex = 0; sampleIndex < samplesPerFrame; sampleIndex++) {
            double angle = (sampleOffset + sampleIndex) * 2.0D * Math.PI * frequency / format.sampleRate();
            short sample = (short) Math.round(Math.sin(angle) * 10_000.0D);
            for (int channel = 0; channel < format.channels(); channel++) {
                int offset = (sampleIndex * format.channels() + channel) * Short.BYTES;
                pcm[offset] = (byte) (sample & 0xFF);
                pcm[offset + 1] = (byte) ((sample >>> 8) & 0xFF);
            }
        }
        return pcm;
    }
}

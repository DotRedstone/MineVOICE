package dev.minevoice.common.audio;

import io.github.jaredmdobson.concentus.OpusApplication;
import io.github.jaredmdobson.concentus.OpusDecoder;
import io.github.jaredmdobson.concentus.OpusEncoder;
import io.github.jaredmdobson.concentus.OpusException;

import java.util.Arrays;

public final class OpusVoiceCodec implements VoiceCodec {
    private static final int MAX_PACKET_BYTES = 1_500;

    private final VoiceAudioFormat format;
    private final int samplesPerFrame;
    private final int samplesPerPacket;
    private final OpusEncoder encoder;

    public OpusVoiceCodec(VoiceAudioFormat format) {
        this.format = format;
        this.samplesPerFrame = format.sampleRate() * format.frameDurationMillis() / 1_000;
        this.samplesPerPacket = samplesPerFrame * format.channels();
        try {
            this.encoder = new OpusEncoder(format.sampleRate(), format.channels(), OpusApplication.OPUS_APPLICATION_VOIP);
        } catch (OpusException exception) {
            throw new IllegalStateException("failed to create Opus encoder", exception);
        }
    }

    @Override
    public String codecName() {
        return "opus";
    }

    @Override
    public synchronized byte[] encode(byte[] pcmSamples) {
        short[] pcm = readLittleEndianPcm(pcmSamples, samplesPerPacket);
        byte[] output = new byte[MAX_PACKET_BYTES];
        try {
            int encodedBytes = encoder.encode(pcm, 0, samplesPerFrame, output, 0, output.length);
            return Arrays.copyOf(output, encodedBytes);
        } catch (OpusException exception) {
            throw new IllegalStateException("failed to encode Opus voice frame", exception);
        }
    }

    @Override
    public byte[] decode(byte[] encodedAudio) {
        return createDecoder().decode(encodedAudio);
    }

    @Override
    public VoiceDecoder createDecoder() {
        return new OpusStreamDecoder(format);
    }

    private static short[] readLittleEndianPcm(byte[] pcmSamples, int sampleCount) {
        short[] pcm = new short[sampleCount];
        int readableSamples = Math.min(sampleCount, pcmSamples.length / Short.BYTES);
        for (int index = 0; index < readableSamples; index++) {
            int offset = index * Short.BYTES;
            pcm[index] = (short) (((pcmSamples[offset + 1] & 0xFF) << 8) | (pcmSamples[offset] & 0xFF));
        }
        return pcm;
    }

    private static byte[] writeLittleEndianPcm(short[] pcm, int sampleCount) {
        byte[] output = new byte[sampleCount * Short.BYTES];
        for (int index = 0; index < sampleCount; index++) {
            short sample = pcm[index];
            int offset = index * Short.BYTES;
            output[offset] = (byte) (sample & 0xFF);
            output[offset + 1] = (byte) ((sample >>> 8) & 0xFF);
        }
        return output;
    }

    private static final class OpusStreamDecoder implements VoiceDecoder {
        private final int channels;
        private final int samplesPerFrame;
        private final int samplesPerPacket;
        private final OpusDecoder decoder;

        private OpusStreamDecoder(VoiceAudioFormat format) {
            this.channels = format.channels();
            this.samplesPerFrame = format.sampleRate() * format.frameDurationMillis() / 1_000;
            this.samplesPerPacket = samplesPerFrame * channels;
            try {
                this.decoder = new OpusDecoder(format.sampleRate(), channels);
            } catch (OpusException exception) {
                throw new IllegalStateException("failed to create Opus decoder", exception);
            }
        }

        @Override
        public synchronized byte[] decode(byte[] encodedAudio) {
            short[] pcm = new short[samplesPerPacket];
            try {
                int decodedSamplesPerChannel = decoder.decode(
                        encodedAudio,
                        0,
                        encodedAudio.length,
                        pcm,
                        0,
                        samplesPerFrame,
                        false
                );
                return writeLittleEndianPcm(pcm, decodedSamplesPerChannel * channels);
            } catch (OpusException exception) {
                throw new IllegalStateException("failed to decode Opus voice frame", exception);
            }
        }
    }
}

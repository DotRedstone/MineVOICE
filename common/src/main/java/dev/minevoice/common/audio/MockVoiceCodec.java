package dev.minevoice.common.audio;

import java.util.Arrays;

public final class MockVoiceCodec implements VoiceCodec {
    private final String codecName;

    public MockVoiceCodec() {
        this("mock-pcm");
    }

    public MockVoiceCodec(String codecName) {
        this.codecName = codecName;
    }

    @Override
    public String codecName() {
        return codecName;
    }

    @Override
    public byte[] encode(byte[] pcmSamples) {
        // TODO(minevoice): replace mock codec with Opus encoder/decoder.
        return Arrays.copyOf(pcmSamples, pcmSamples.length);
    }

    @Override
    public byte[] decode(byte[] encodedAudio) {
        // TODO(minevoice): replace mock codec with Opus encoder/decoder.
        return Arrays.copyOf(encodedAudio, encodedAudio.length);
    }
}

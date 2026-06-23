package dev.minevoice.common.audio;

import java.util.Arrays;

public final class MockVoiceCodec implements VoiceCodec {
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

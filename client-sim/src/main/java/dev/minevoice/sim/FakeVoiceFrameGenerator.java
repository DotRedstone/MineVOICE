package dev.minevoice.sim;

import dev.minevoice.common.protocol.VoiceFrame;
import dev.minevoice.common.protocol.VoiceChannel;

import java.time.Instant;
import java.util.UUID;

public final class FakeVoiceFrameGenerator {
    private final int frameBytes;
    private final int sampleRate;
    private final int channels;

    public FakeVoiceFrameGenerator() {
        this(160, 48_000, 1);
    }

    public FakeVoiceFrameGenerator(int frameBytes, int sampleRate, int channels) {
        this.frameBytes = frameBytes;
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    public VoiceFrame generate(UUID playerId, long sequence) {
        return generate(playerId, sequence, VoiceChannel.PROXIMITY);
    }

    public VoiceFrame generate(UUID playerId, long sequence, VoiceChannel channel) {
        byte[] fakeAudio = new byte[frameBytes];
        for (int index = 0; index < fakeAudio.length; index++) {
            fakeAudio[index] = (byte) ((sequence + index) & 0x7F);
        }
        return new VoiceFrame(playerId, sequence, Instant.now().toEpochMilli(), fakeAudio, sampleRate, channels, channel);
    }
}

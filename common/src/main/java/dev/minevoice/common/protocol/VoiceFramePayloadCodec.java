package dev.minevoice.common.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public final class VoiceFramePayloadCodec {
    private static final int HEADER_BYTES = Long.BYTES
            + Long.BYTES
            + Long.BYTES
            + Long.BYTES
            + Integer.BYTES
            + Integer.BYTES
            + Integer.BYTES
            + Integer.BYTES;

    private VoiceFramePayloadCodec() {
    }

    public static byte[] encode(VoiceFrame frame) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(output);
            data.writeLong(frame.senderPlayerId().getMostSignificantBits());
            data.writeLong(frame.senderPlayerId().getLeastSignificantBits());
            data.writeLong(frame.sequence());
            data.writeLong(frame.timestampMillis());
            data.writeInt(frame.sampleRate());
            data.writeInt(frame.channels());
            data.writeInt(frame.channel().ordinal());
            data.writeInt(frame.encodedAudio().length);
            data.write(frame.encodedAudio());
            data.flush();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to encode voice frame payload", exception);
        }
    }

    public static VoiceFrame decode(byte[] payload) {
        if (payload.length < HEADER_BYTES) {
            throw new IllegalArgumentException("voice frame payload is shorter than header");
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        UUID sender = new UUID(buffer.getLong(), buffer.getLong());
        long sequence = buffer.getLong();
        long timestampMillis = buffer.getLong();
        int sampleRate = buffer.getInt();
        int channels = buffer.getInt();
        int channelOrdinal = buffer.getInt();
        VoiceChannel[] voiceChannels = VoiceChannel.values();
        if (channelOrdinal < 0 || channelOrdinal >= voiceChannels.length) {
            throw new IllegalArgumentException("invalid voice frame channel: " + channelOrdinal);
        }
        int audioLength = buffer.getInt();
        if (audioLength < 0 || audioLength > buffer.remaining()) {
            throw new IllegalArgumentException("invalid voice frame audio length: " + audioLength);
        }
        byte[] audio = new byte[audioLength];
        buffer.get(audio);
        return new VoiceFrame(sender, sequence, timestampMillis, audio, sampleRate, channels, voiceChannels[channelOrdinal]);
    }
}

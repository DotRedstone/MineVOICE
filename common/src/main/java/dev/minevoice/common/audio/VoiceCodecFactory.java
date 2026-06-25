package dev.minevoice.common.audio;

public final class VoiceCodecFactory {
    private VoiceCodecFactory() {
    }

    public static VoiceCodec create(String configuredCodec) {
        return new MockVoiceCodec(effectiveCodecName(configuredCodec));
    }

    public static String effectiveCodecName(String configuredCodec) {
        if ("opus".equalsIgnoreCase(configuredCodec)) {
            return "mock-pcm-opus-placeholder";
        }
        return "mock-pcm";
    }
}

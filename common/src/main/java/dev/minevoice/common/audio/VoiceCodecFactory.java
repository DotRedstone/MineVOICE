package dev.minevoice.common.audio;

public final class VoiceCodecFactory {
    private VoiceCodecFactory() {
    }

    public static VoiceCodec create(String configuredCodec) {
        String codecName = normalizeCodecName(configuredCodec);
        if ("opus".equals(codecName)) {
            try {
                return new OpusVoiceCodec(VoiceAudioFormat.narrowbandVoice());
            } catch (RuntimeException | LinkageError exception) {
                return new MockVoiceCodec("mock-pcm-fallback");
            }
        }
        return new MockVoiceCodec();
    }

    public static String effectiveCodecName(String configuredCodec) {
        return create(configuredCodec).codecName();
    }

    public static String normalizeCodecName(String configuredCodec) {
        if (configuredCodec == null || configuredCodec.isBlank()) {
            return "opus";
        }
        String normalized = configuredCodec.trim().toLowerCase();
        if ("opus".equals(normalized)) {
            return "opus";
        }
        return "mock";
    }
}

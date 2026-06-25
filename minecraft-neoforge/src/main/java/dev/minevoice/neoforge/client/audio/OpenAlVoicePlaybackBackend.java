package dev.minevoice.neoforge.client.audio;

import dev.minevoice.neoforge.client.ClientAudioSettings;

/**
 * Placeholder for the OpenAL source backend. Kept reflection-based so the mod still
 * builds and runs when the LWJGL OpenAL classes are unavailable in a dev runtime.
 */
public final class OpenAlVoicePlaybackBackend implements VoicePlaybackBackend {
    public static final String NAME = "openal";

    private OpenAlVoicePlaybackBackend() {
    }

    public static boolean available() {
        return classPresent("org.lwjgl.openal.AL10") && classPresent("org.lwjgl.openal.ALC10");
    }

    public static OpenAlVoicePlaybackBackend open() {
        if (!available()) {
            throw new UnsupportedOperationException("LWJGL OpenAL classes are unavailable");
        }
        throw new UnsupportedOperationException("OpenAL per-speaker source playback is not implemented yet");
    }

    @Override
    public String backendName() {
        return NAME;
    }

    @Override
    public void start() {
    }

    @Override
    public void writeStereoFrame(byte[] pcm, int offset, int length) {
    }

    @Override
    public boolean matches(ClientAudioSettings settings) {
        return false;
    }

    @Override
    public void close() {
    }

    private static boolean classPresent(String className) {
        try {
            Class.forName(className, false, OpenAlVoicePlaybackBackend.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}

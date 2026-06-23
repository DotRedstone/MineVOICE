package dev.minevoice.neoforge.client.audio;

@FunctionalInterface
public interface VoiceActivityListener {
    void onActivity(float microphoneLevel, boolean transmitting);
}

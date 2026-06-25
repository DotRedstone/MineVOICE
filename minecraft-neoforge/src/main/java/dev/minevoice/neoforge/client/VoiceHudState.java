package dev.minevoice.neoforge.client;

import dev.minevoice.common.protocol.VoiceChannel;

public final class VoiceHudState {
    private volatile VoiceConnectionStatus connectionStatus = VoiceConnectionStatus.DISCONNECTED;
    private volatile boolean pushToTalkDown;
    private volatile boolean groupPushToTalkDown;
    private volatile boolean transmitting;
    private volatile float microphoneLevel;
    private volatile VoiceChannel activeChannel = VoiceChannel.PROXIMITY;

    public VoiceConnectionStatus connectionStatus() {
        return connectionStatus;
    }

    public boolean pushToTalkDown() {
        return pushToTalkDown;
    }

    public boolean groupPushToTalkDown() {
        return groupPushToTalkDown;
    }

    public boolean transmitting() {
        return transmitting;
    }

    public float microphoneLevel() {
        return microphoneLevel;
    }

    public VoiceChannel activeChannel() {
        return activeChannel;
    }

    public void setConnectionStatus(VoiceConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public void setPushToTalkDown(boolean pushToTalkDown) {
        this.pushToTalkDown = pushToTalkDown;
        if (pushToTalkDown) {
            activeChannel = VoiceChannel.PROXIMITY;
        }
    }

    public void setGroupPushToTalkDown(boolean groupPushToTalkDown) {
        this.groupPushToTalkDown = groupPushToTalkDown;
        if (groupPushToTalkDown) {
            activeChannel = VoiceChannel.GROUP;
        } else if (!pushToTalkDown) {
            activeChannel = VoiceChannel.PROXIMITY;
        }
    }

    public void setMicrophoneActivity(float microphoneLevel, boolean transmitting) {
        this.microphoneLevel = Math.max(0.0F, Math.min(1.0F, microphoneLevel));
        this.transmitting = transmitting;
    }

    public void setActiveChannel(VoiceChannel activeChannel) {
        this.activeChannel = activeChannel;
    }
}

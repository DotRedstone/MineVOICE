package dev.minevoice.common.protocol;

public enum VoicePacketType {
    HELLO,
    AUTH,
    AUTH_OK,
    AUTH_FAILED,
    VOICE_FRAME,
    SERVER_STATE,
    PLAYER_POSITION,
    PING,
    PONG,
    DISCONNECT,
    ERROR
}

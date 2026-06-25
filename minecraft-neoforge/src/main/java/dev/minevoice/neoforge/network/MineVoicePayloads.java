package dev.minevoice.neoforge.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.server.level.ServerPlayer;
import dev.minevoice.neoforge.MineVoiceMod;

public final class MineVoicePayloads {
    public static final String VOICE_SERVER_INFO = "minevoice:voice_server_info";
    public static final String PLAYER_POSITION = "minevoice:player_position";
    public static final String VOICE_ROSTER = "minevoice:voice_roster";

    private MineVoicePayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                VoiceServerInfoPayload.TYPE,
                VoiceServerInfoPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleVoiceServerInfo(payload))
        );
        registrar.playToClient(
                VoiceRosterPayload.TYPE,
                VoiceRosterPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleVoiceRoster(payload))
        );
        registrar.playToServer(
                VoiceGroupActionPayload.TYPE,
                VoiceGroupActionPayload.STREAM_CODEC,
                (payload, context) -> MineVoiceMod.handleGroupAction((ServerPlayer) context.player(), payload)
        );
        registrar.playToServer(
                VoicePlayerStatusPayload.TYPE,
                VoicePlayerStatusPayload.STREAM_CODEC,
                (payload, context) -> MineVoiceMod.handlePlayerStatus((ServerPlayer) context.player(), payload)
        );
        registrar.playToServer(
                VoicePeerMutePayload.TYPE,
                VoicePeerMutePayload.STREAM_CODEC,
                (payload, context) -> MineVoiceMod.handlePeerMute((ServerPlayer) context.player(), payload)
        );
    }

    private static void handleVoiceServerInfo(VoiceServerInfoPayload payload) {
        try {
            Class<?> bootstrap = Class.forName("dev.minevoice.neoforge.client.MineVoiceClientBootstrap");
            bootstrap.getMethod("handleVoiceServerInfo", VoiceServerInfoPayload.class).invoke(null, payload);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("failed to handle MineVOICE voice server info payload", exception);
        }
    }

    private static void handleVoiceRoster(VoiceRosterPayload payload) {
        try {
            Class<?> bootstrap = Class.forName("dev.minevoice.neoforge.client.MineVoiceClientBootstrap");
            bootstrap.getMethod("handleVoiceRoster", VoiceRosterPayload.class).invoke(null, payload);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("failed to handle MineVOICE voice roster payload", exception);
        }
    }
}

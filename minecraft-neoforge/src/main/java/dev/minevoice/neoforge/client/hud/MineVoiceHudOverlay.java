package dev.minevoice.neoforge.client.hud;

import dev.minevoice.neoforge.client.ClientAudioSettings;
import dev.minevoice.neoforge.client.VoiceConnectionStatus;
import dev.minevoice.neoforge.client.VoiceHudState;
import dev.minevoice.neoforge.client.VoicePlayerDirectory;
import dev.minevoice.neoforge.client.VoiceSpeakerTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class MineVoiceHudOverlay {
    // Keep the local voice state as a single unobtrusive status icon, matching the
    // convention players already know from established voice-chat mods.
    private static final int LEFT = 16;
    private static final int ICON_SIZE = 16;
    private static final ResourceLocation MICROPHONE_ICON = ResourceLocation.fromNamespaceAndPath(
            "minevoice", "microphone_status"
    );
    private static final ResourceLocation MICROPHONE_MUTED_ICON = ResourceLocation.fromNamespaceAndPath(
            "minevoice", "microphone_muted"
    );
    private static final ResourceLocation SPEAKER_MUTED_ICON = ResourceLocation.fromNamespaceAndPath(
            "minevoice", "speaker_muted"
    );

    private MineVoiceHudOverlay() {
    }

    public static void render(
            RenderGuiEvent.Post event,
            VoiceHudState state,
            ClientAudioSettings settings,
            VoicePlayerDirectory directory,
            VoiceSpeakerTracker speakers
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int top = graphics.guiHeight() - ICON_SIZE - 16;
        ResourceLocation icon = currentHudIcon(state, settings);
        if (icon != null) {
            graphics.blitSprite(icon, LEFT, top, ICON_SIZE, ICON_SIZE);
        }
        VoiceParticipantOverlay.render(graphics, directory, speakers, top);
    }

    private static ResourceLocation currentHudIcon(VoiceHudState state, ClientAudioSettings settings) {
        if (state.connectionStatus() == VoiceConnectionStatus.AUTH_FAILED
                || state.connectionStatus() == VoiceConnectionStatus.ERROR) {
            return MICROPHONE_MUTED_ICON;
        }
        if (settings.deafened()) {
            return SPEAKER_MUTED_ICON;
        }
        if (settings.muted()) {
            return MICROPHONE_MUTED_ICON;
        }
        return state.transmitting() ? MICROPHONE_ICON : null;
    }
}

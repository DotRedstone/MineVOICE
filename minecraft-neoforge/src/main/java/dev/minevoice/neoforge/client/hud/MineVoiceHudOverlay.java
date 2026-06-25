package dev.minevoice.neoforge.client.hud;

import dev.minevoice.neoforge.client.ClientAudioSettings;
import dev.minevoice.neoforge.client.VoiceConnectionStatus;
import dev.minevoice.neoforge.client.VoiceHudState;
import dev.minevoice.neoforge.client.VoicePlayerDirectory;
import dev.minevoice.neoforge.client.VoiceSpeakerTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class MineVoiceHudOverlay {
    private static final int LEFT = 16;
    private static final int BOTTOM = 16;
    private static final ResourceLocation MICROPHONE_ICON = ResourceLocation.fromNamespaceAndPath(
            "minevoice", "microphone_status"
    );
    private static final ResourceLocation MICROPHONE_MUTED_ICON = ResourceLocation.fromNamespaceAndPath(
            "minevoice", "microphone_muted"
    );
    private static final ResourceLocation SPEAKER_ICON = ResourceLocation.fromNamespaceAndPath(
            "minevoice", "speaker_status"
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
        if (minecraft.player == null || minecraft.options.hideGui || !settings.hudEnabled()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int iconSize = settings.hudIconSize();
        int top = graphics.guiHeight() - iconSize - BOTTOM;
        graphics.blitSprite(currentHudIcon(state, settings), LEFT, top, iconSize, iconSize);
        if (state.groupPushToTalkDown()) {
            renderGroupTalkBadge(graphics, minecraft.font, LEFT + iconSize + 4, top, iconSize);
        }
        VoiceParticipantOverlay.render(graphics, settings, directory, speakers, LEFT, top, iconSize);
    }

    private static void renderGroupTalkBadge(GuiGraphics graphics, Font font, int left, int top, int iconSize) {
        int width = Math.max(16, iconSize);
        MineVoiceHudStyle.renderSlot(graphics, left, top, width, true);
        graphics.drawCenteredString(font, "G", left + width / 2, top + (iconSize - 8) / 2, MineVoiceHudStyle.TEXT);
    }

    private static ResourceLocation currentHudIcon(VoiceHudState state, ClientAudioSettings settings) {
        if (settings.deafened()) {
            return SPEAKER_MUTED_ICON;
        }
        if (settings.muted()
                || state.connectionStatus() == VoiceConnectionStatus.AUTH_FAILED
                || state.connectionStatus() == VoiceConnectionStatus.ERROR) {
            return MICROPHONE_MUTED_ICON;
        }
        if (state.transmitting()) {
            return MICROPHONE_ICON;
        }
        return SPEAKER_ICON;
    }
}

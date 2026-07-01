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
                setIconColor(graphics, state, settings, directory, speakers);
        graphics.blitSprite(currentHudIcon(state, settings), LEFT, top, iconSize, iconSize);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        ProximityEdgeIndicators.onRenderGuiPost(event);
    }



    
    private static void setIconColor(GuiGraphics graphics, VoiceHudState state, ClientAudioSettings settings, VoicePlayerDirectory directory, VoiceSpeakerTracker speakers) {
        java.util.UUID myId = Minecraft.getInstance().player.getUUID();
        dev.minevoice.neoforge.network.VoiceRosterEntry myEntry = directory.get(myId);
        java.util.UUID myGroupId = myEntry != null ? myEntry.groupId() : null;

        if (state.transmitting()) {
            if (state.groupPushToTalkDown()) {
                int color = (myEntry != null && myGroupId != null) ? myEntry.groupColor() : settings.groupMemberColor();
                setColorFromInt(graphics, color);
            } else {
                graphics.setColor(0.7f, 0.7f, 0.7f, 1.0f);
            }
            return;
        }

        java.util.List<java.util.UUID> active = speakers.activeSpeakers(10);
        if (active.isEmpty()) {
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }

        boolean teamSpeaking = false;
        int teamColor = 0;

        for (java.util.UUID playerId : active) {
            dev.minevoice.neoforge.network.VoiceRosterEntry entry = directory.get(playerId);
            if (entry != null && myGroupId != null && myGroupId.equals(entry.groupId())) {
                teamSpeaking = true;
                teamColor = entry.groupColor();
                break;
            }
        }
        
        if (teamSpeaking) {
            setColorFromInt(graphics, teamColor);
        } else {
            graphics.setColor(0.7f, 0.7f, 0.7f, 1.0f);
        }
    }

    private static void setColorFromInt(GuiGraphics graphics, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        graphics.setColor(r, g, b, 1.0f);
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

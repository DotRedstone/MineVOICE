package dev.minevoice.neoforge.client.hud;

import dev.minevoice.neoforge.client.VoicePlayerDirectory;
import dev.minevoice.neoforge.client.VoiceSpeakerTracker;
import dev.minevoice.neoforge.network.VoiceRosterEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public final class VoiceParticipantOverlay {
    private static final int SPEAKER_WIDTH = 96;
    private static final int ROW_HEIGHT = 20;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SPEAKING = 0xFF55FF55;

    private VoiceParticipantOverlay() {
    }

    public static void render(GuiGraphics graphics, VoicePlayerDirectory directory, VoiceSpeakerTracker speakers, int microphoneTop) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        renderSpeakers(graphics, minecraft, directory, speakers);
        renderGroup(graphics, minecraft, directory, speakers, microphoneTop);
    }

    private static void renderSpeakers(
            GuiGraphics graphics,
            Minecraft minecraft,
            VoicePlayerDirectory directory,
            VoiceSpeakerTracker speakers
    ) {
        List<UUID> activeSpeakers = speakers.activeSpeakers(4);
        if (activeSpeakers.isEmpty()) {
            return;
        }
        int left = graphics.guiWidth() - SPEAKER_WIDTH - 12;
        int top = graphics.guiHeight() - activeSpeakers.size() * ROW_HEIGHT - 18;
        for (int index = 0; index < activeSpeakers.size(); index++) {
            UUID playerId = activeSpeakers.get(index);
            VoiceRosterEntry entry = directory.get(playerId);
            int rowTop = top + index * ROW_HEIGHT;
            drawFace(graphics, minecraft, playerId, left, rowTop + 2, 16);
            String name = entry == null ? playerId.toString().substring(0, 8) : entry.playerName();
            graphics.drawString(minecraft.font, name, left + 21, rowTop + 6, TEXT, true);
            graphics.fill(left + SPEAKER_WIDTH - 4, rowTop + 8, left + SPEAKER_WIDTH, rowTop + 12, SPEAKING);
        }
    }

    private static void renderGroup(
            GuiGraphics graphics,
            Minecraft minecraft,
            VoicePlayerDirectory directory,
            VoiceSpeakerTracker speakers,
            int microphoneTop
    ) {
        VoiceRosterEntry self = directory.get(minecraft.player.getUUID());
        if (self == null || self.groupId() == null) {
            return;
        }
        List<VoiceRosterEntry> members = directory.groupMembers(self.groupId());
        if (members.isEmpty()) {
            return;
        }
        int top = microphoneTop - 24;
        int left = 16;
        for (int index = 0; index < Math.min(members.size(), 7); index++) {
            VoiceRosterEntry member = members.get(index);
            int x = left + index * 24;
            if (speakers.isSpeaking(member.playerId())) {
                graphics.fill(x + 2, top + 18, x + 18, top + 20, SPEAKING);
            }
            drawFace(graphics, minecraft, member.playerId(), x + 2, top + 2, 16);
            if (member.muted()) {
                graphics.fill(x + 14, top + 2, x + 18, top + 6, 0xFFE05A5A);
            }
        }
    }

    public static void drawFace(GuiGraphics graphics, Minecraft minecraft, UUID playerId, int x, int y, int size) {
        ResourceLocation texture = DefaultPlayerSkin.get(playerId).texture();
        if (minecraft.getConnection() != null) {
            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(playerId);
            if (playerInfo != null) {
                texture = playerInfo.getSkin().texture();
            }
        }
        graphics.blit(texture, x, y, size, size, 8.0F, 8.0F, 8, 8, 64, 64);
        graphics.blit(texture, x, y, size, size, 40.0F, 8.0F, 8, 8, 64, 64);
    }
}

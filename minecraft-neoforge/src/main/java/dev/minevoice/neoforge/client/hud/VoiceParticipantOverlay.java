package dev.minevoice.neoforge.client.hud;

import dev.minevoice.neoforge.client.ClientAudioSettings;
import dev.minevoice.neoforge.client.HudAvatarAnchor;
import dev.minevoice.neoforge.client.VoicePlayerDirectory;
import dev.minevoice.neoforge.client.VoiceSpeakerTracker;
import dev.minevoice.neoforge.network.VoiceRosterEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public final class VoiceParticipantOverlay {
    private static final int SCREEN_MARGIN = 12;
    private static final int SPEAKER_ROW_WIDTH = 104;
    private static final ResourceLocation MICROPHONE_MUTED_ICON = ResourceLocation.fromNamespaceAndPath(
            "minevoice", "microphone_muted"
    );

    private VoiceParticipantOverlay() {
    }

    public static void render(
            GuiGraphics graphics,
            ClientAudioSettings settings,
            VoicePlayerDirectory directory,
            VoiceSpeakerTracker speakers,
            int microphoneLeft,
            int microphoneTop,
            int slotSize
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (settings.speakerHudEnabled()) {
            renderSpeakers(graphics, minecraft, settings, directory, speakers, slotSize);
        }
        if (settings.groupHudEnabled()) {
            renderGroup(graphics, minecraft, directory, speakers, microphoneLeft, microphoneTop, slotSize);
        }
    }

    private static void renderSpeakers(
            GuiGraphics graphics,
            Minecraft minecraft,
            ClientAudioSettings settings,
            VoicePlayerDirectory directory,
            VoiceSpeakerTracker speakers,
            int slotSize
    ) {
        List<UUID> activeSpeakers = speakers.activeSpeakers(4);
        if (activeSpeakers.isEmpty()) {
            return;
        }
        int rowHeight = slotSize;
        int totalHeight = activeSpeakers.size() * rowHeight + Math.max(0, activeSpeakers.size() - 1) * 2;
        int left = speakerLeft(graphics, settings.hudAvatarAnchor());
        int top = speakerTop(graphics, settings.hudAvatarAnchor(), totalHeight);
        int iconSize = Math.max(8, slotSize - MineVoiceHudStyle.SLOT_PADDING * 2);
        for (int index = 0; index < activeSpeakers.size(); index++) {
            UUID playerId = activeSpeakers.get(index);
            VoiceRosterEntry entry = directory.get(playerId);
            int rowTop = top + index * (rowHeight + 2);
            MineVoiceHudStyle.renderRow(graphics, left, rowTop, SPEAKER_ROW_WIDTH, rowHeight, true);
            drawFace(graphics, minecraft, playerId, left + MineVoiceHudStyle.SLOT_PADDING, rowTop + MineVoiceHudStyle.SLOT_PADDING, iconSize);
            String name = entry == null ? playerId.toString().substring(0, 8) : entry.playerName();
            graphics.drawString(
                    minecraft.font,
                    clippedToWidth(minecraft, name, SPEAKER_ROW_WIDTH - slotSize - 12),
                    left + slotSize + 4,
                    rowTop + (rowHeight - 8) / 2,
                    MineVoiceHudStyle.TEXT,
                    true
            );
            MineVoiceHudStyle.renderBar(graphics, left + slotSize + 4, rowTop + rowHeight - 4, SPEAKER_ROW_WIDTH - slotSize - 10, 2, 1.0F, MineVoiceHudStyle.SPEAKING);
        }
    }

    private static void renderGroup(
            GuiGraphics graphics,
            Minecraft minecraft,
            VoicePlayerDirectory directory,
            VoiceSpeakerTracker speakers,
            int microphoneLeft,
            int microphoneTop,
            int slotSize
    ) {
        VoiceRosterEntry self = directory.get(minecraft.player.getUUID());
        if (self == null || self.groupId() == null) {
            return;
        }
        List<VoiceRosterEntry> members = directory.groupMembers(self.groupId());
        if (members.isEmpty()) {
            return;
        }
        int iconSize = Math.max(8, slotSize - MineVoiceHudStyle.SLOT_PADDING * 2);
        int maxMembers = Math.min(members.size(), 7);
        int totalWidth = maxMembers * slotSize + Math.max(0, maxMembers - 1) * MineVoiceHudStyle.GAP;
        int left = Math.max(SCREEN_MARGIN, Math.min(microphoneLeft, graphics.guiWidth() - totalWidth - SCREEN_MARGIN));
        int top = microphoneTop - slotSize - MineVoiceHudStyle.GAP;
        if (top < SCREEN_MARGIN) {
            top = microphoneTop + slotSize + MineVoiceHudStyle.GAP;
        }
        for (int index = 0; index < maxMembers; index++) {
            VoiceRosterEntry member = members.get(index);
            boolean speaking = speakers.isSpeaking(member.playerId());
            int x = left + index * (slotSize + MineVoiceHudStyle.GAP);
            MineVoiceHudStyle.renderSlot(graphics, x, top, slotSize, speaking);
            drawFace(graphics, minecraft, member.playerId(), x + MineVoiceHudStyle.SLOT_PADDING, top + MineVoiceHudStyle.SLOT_PADDING, iconSize);
            if (member.muted()) {
                int badgeSize = Math.max(8, iconSize / 2 + 2);
                graphics.blitSprite(MICROPHONE_MUTED_ICON, x + slotSize - badgeSize - 1, top + slotSize - badgeSize - 1, badgeSize, badgeSize);
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
        PlayerFaceRenderer.draw(graphics, texture, x, y, size);
    }

    private static int speakerLeft(GuiGraphics graphics, HudAvatarAnchor anchor) {
        return switch (anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> SCREEN_MARGIN;
            case TOP_RIGHT, BOTTOM_RIGHT -> graphics.guiWidth() - SPEAKER_ROW_WIDTH - SCREEN_MARGIN;
        };
    }

    private static int speakerTop(GuiGraphics graphics, HudAvatarAnchor anchor, int totalHeight) {
        return switch (anchor) {
            case TOP_LEFT, TOP_RIGHT -> SCREEN_MARGIN;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> graphics.guiHeight() - totalHeight - SCREEN_MARGIN;
        };
    }

    private static String clippedToWidth(Minecraft minecraft, String value, int maxWidth) {
        String clipped = value;
        while (minecraft.font.width(clipped) > maxWidth && clipped.length() > 1) {
            clipped = clipped.substring(0, clipped.length() - 2) + ".";
        }
        return clipped;
    }
}

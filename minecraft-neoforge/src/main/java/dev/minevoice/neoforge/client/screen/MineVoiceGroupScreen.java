package dev.minevoice.neoforge.client.screen;

import dev.minevoice.neoforge.client.MineVoiceClientBootstrap;
import dev.minevoice.neoforge.client.VoiceGroupSummary;
import dev.minevoice.neoforge.client.VoiceSpeakerTracker;
import dev.minevoice.neoforge.client.hud.VoiceParticipantOverlay;
import dev.minevoice.neoforge.network.VoiceRosterEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class MineVoiceGroupScreen extends Screen {
    private static final int PANEL_WIDTH = 236;
    private static final int MIN_PANEL_HEIGHT = 116;
    private static final int ROW_HEIGHT = 22;
    private static final int TEXT = 0xFF404040;
    private static final int SUBTLE_TEXT = 0xFF606060;
    private static final int SPEAKING = 0xFF55FF55;

    private final Screen parent;
    private EditBox groupName;
    private UUID renderedGroupId;

    public MineVoiceGroupScreen(Screen parent) {
        super(Component.translatable("screen.minevoice.group.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int panelHeight = panelHeight();
        VoiceRosterEntry self = selfEntry();
        renderedGroupId = self == null ? null : self.groupId();
        if (self != null && self.groupId() != null) {
            addButton(Component.translatable("screen.minevoice.group.leave"),
                    left + PANEL_WIDTH - 68, top + panelHeight - 25, 62, 19,
                    button -> MineVoiceClientBootstrap.leaveGroup());
            return;
        }

        groupName = new EditBox(font, left + 6, top + 28, 154, 20, Component.translatable("screen.minevoice.group.name"));
        groupName.setMaxLength(32);
        groupName.setHint(Component.translatable("screen.minevoice.group.name"));
        addRenderableWidget(groupName);
        addButton(Component.translatable("screen.minevoice.group.create"),
                left + 164, top + 28, 66, 20, button -> MineVoiceClientBootstrap.createGroup(groupName.getValue()));

        List<VoiceGroupSummary> groups = MineVoiceClientBootstrap.voiceDirectory().groups();
        for (int index = 0; index < Math.min(groups.size(), 5); index++) {
            VoiceGroupSummary group = groups.get(index);
            addButton(Component.translatable("screen.minevoice.group.join"),
                    left + PANEL_WIDTH - 68, top + 54 + index * ROW_HEIGHT + 1, 62, 19,
                    button -> MineVoiceClientBootstrap.joinGroup(group.groupId()));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        Minecraft minecraft = Minecraft.getInstance();
        Component title = Component.translatable("screen.minevoice.group.title");
        graphics.drawCenteredString(minecraft.font, title, left + PANEL_WIDTH / 2, top + 7, TEXT);

        VoiceRosterEntry self = selfEntry();
        if (self != null && self.groupId() != null) {
            renderCurrentGroup(graphics, minecraft, self, left, top);
        } else {
            renderJoinableGroups(graphics, minecraft, left, top);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        MineVoicePanelStyle.render(graphics, panelLeft(), panelTop(), PANEL_WIDTH, panelHeight());
    }

    @Override
    public void tick() {
        VoiceRosterEntry self = selfEntry();
        UUID currentGroupId = self == null ? null : self.groupId();
        if (!Objects.equals(renderedGroupId, currentGroupId)) {
            rebuildWidgets();
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private void addButton(Component message, int x, int y, int width, int height, Button.OnPress onPress) {
        addRenderableWidget(Button.builder(message, onPress).bounds(x, y, width, height).build());
    }

    private void renderCurrentGroup(GuiGraphics graphics, Minecraft minecraft, VoiceRosterEntry self, int left, int top) {
        Component group = Component.translatable("screen.minevoice.group.current", self.groupName());
        graphics.drawString(minecraft.font, group, left + 6, top + 31, SUBTLE_TEXT);
        List<VoiceRosterEntry> members = MineVoiceClientBootstrap.voiceDirectory().groupMembers(self.groupId());
        VoiceSpeakerTracker speakers = MineVoiceClientBootstrap.speakerTracker();
        for (int index = 0; index < Math.min(members.size(), 5); index++) {
            VoiceRosterEntry member = members.get(index);
            int rowTop = top + 47 + index * ROW_HEIGHT;
            boolean speaking = speakers.isSpeaking(member.playerId());
            VoiceParticipantOverlay.drawFace(graphics, minecraft, member.playerId(), left + 6, rowTop + 2, 16);
            graphics.drawString(minecraft.font, member.playerName(), left + 27, rowTop + 6, TEXT, true);
            if (member.muted()) {
                graphics.fill(left + PANEL_WIDTH - 12, rowTop + 8, left + PANEL_WIDTH - 8, rowTop + 12, 0xFFE05A5A);
            } else if (speaking) {
                graphics.fill(left + 6, rowTop + ROW_HEIGHT - 2, left + PANEL_WIDTH - 6, rowTop + ROW_HEIGHT, SPEAKING);
            }
        }
    }

    private void renderJoinableGroups(GuiGraphics graphics, Minecraft minecraft, int left, int top) {
        List<VoiceGroupSummary> groups = MineVoiceClientBootstrap.voiceDirectory().groups();
        if (groups.isEmpty()) {
            graphics.drawString(minecraft.font, Component.translatable("screen.minevoice.group.empty"), left + 6, top + 58, SUBTLE_TEXT);
            return;
        }
        for (int index = 0; index < Math.min(groups.size(), 5); index++) {
            VoiceGroupSummary group = groups.get(index);
            int rowTop = top + 54 + index * ROW_HEIGHT;
            graphics.drawString(minecraft.font, group.groupName(), left + 6, rowTop + 6, TEXT, true);
            graphics.drawString(minecraft.font, Integer.toString(group.memberCount()), left + PANEL_WIDTH - 78, rowTop + 6, SUBTLE_TEXT, true);
        }
    }

    private VoiceRosterEntry selfEntry() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? null : MineVoiceClientBootstrap.voiceDirectory().get(minecraft.player.getUUID());
    }

    private int panelLeft() {
        return width / 2 - PANEL_WIDTH / 2;
    }

    private int panelTop() {
        return Math.max(4, height / 2 - panelHeight() / 2);
    }

    private int panelHeight() {
        VoiceRosterEntry self = selfEntry();
        if (self != null && self.groupId() != null) {
            int members = Math.min(MineVoiceClientBootstrap.voiceDirectory().groupMembers(self.groupId()).size(), 5);
            return Math.max(MIN_PANEL_HEIGHT, 72 + members * ROW_HEIGHT);
        }
        int groups = Math.min(MineVoiceClientBootstrap.voiceDirectory().groups().size(), 5);
        return Math.max(MIN_PANEL_HEIGHT, 76 + groups * ROW_HEIGHT);
    }
}

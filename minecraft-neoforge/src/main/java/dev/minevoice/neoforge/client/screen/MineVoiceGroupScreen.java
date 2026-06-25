package dev.minevoice.neoforge.client.screen;

import dev.minevoice.neoforge.client.ClientAudioSettings;
import dev.minevoice.neoforge.client.MineVoiceClientBootstrap;
import dev.minevoice.neoforge.client.VoiceGroupSummary;
import dev.minevoice.neoforge.client.hud.VoiceParticipantOverlay;
import dev.minevoice.neoforge.network.VoiceRosterEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class MineVoiceGroupScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 292;
    private static final int PANEL_MAX_HEIGHT = 250;
    private static final int PANEL_MIN_WIDTH = 250;
    private static final int PANEL_MIN_HEIGHT = 190;
    private static final int FRAME_TOP_OFFSET = 50;
    private static final int ROW_TOP_OFFSET = 82;
    private static final int ROW_HEIGHT = 36;
    private static final int PLAYER_CONTROLS_WIDTH = 84;
    private static final int PLAYER_SLIDER_WIDTH = 58;
    private static final int PLAYER_MUTE_SIZE = 20;
    private static final int SEARCH_ICON_LEFT_OFFSET = 10;
    private static final int SEARCH_BOX_LEFT_OFFSET = 28;
    private static final int SEARCH_BOX_TOP_OFFSET = 10;
    private static final int SEARCH_BOX_HEIGHT = 15;
    private static final int GROUP_JOINABLE_LABEL_OFFSET = 55;
    private static final int GROUP_LIST_OFFSET = 66;
    private static final int TEXT = 0xFF404040;
    private static final int LIST_TEXT = 0xFFFFFFFF;
    private static final int SUBTLE_TEXT = 0xA0FFFFFF;
    private static final int SPEAKING = 0xFF55AA55;
    private static final ResourceLocation SEARCH_ICON = ResourceLocation.withDefaultNamespace("icon/search");
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

    private final Screen parent;
    private ChannelTab selectedTab = ChannelTab.PUBLIC;
    private EditBox searchBox;
    private EditBox groupName;
    private EditBox createPassword;
    private EditBox joinPassword;
    private UUID renderedGroupId;
    private String renderedSearch = "";
    private int listScroll;
    private boolean restoreSearchFocus;

    public MineVoiceGroupScreen(Screen parent) {
        super(Component.translatable("screen.minevoice.group.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        int left = panelLeft();
        int top = panelTop();
        int contentLeft = left + 6;
        int contentWidth = panelWidth() - 12;
        VoiceRosterEntry self = selfEntry();
        renderedGroupId = self == null ? null : self.groupId();

        int tabGap = 3;
        int tabWidth = (contentWidth - tabGap) / 2;
        Button publicTab = addButton(Component.translatable("screen.minevoice.group.tab.public"),
                contentLeft, top + 25, tabWidth, 18,
                button -> switchTab(ChannelTab.PUBLIC));
        publicTab.active = selectedTab != ChannelTab.PUBLIC;
        Button groupTab = addButton(Component.translatable("screen.minevoice.group.tab.group"),
                contentLeft + tabWidth + tabGap, top + 25, tabWidth, 18,
                button -> switchTab(ChannelTab.GROUP));
        groupTab.active = selectedTab != ChannelTab.GROUP;

        int searchX = searchLeft(left);
        int searchY = top + FRAME_TOP_OFFSET + SEARCH_BOX_TOP_OFFSET;
        searchBox = new EditBox(font, searchX, searchY, searchWidth(), SEARCH_BOX_HEIGHT, Component.translatable("screen.minevoice.group.search"));
        searchBox.setMaxLength(32);
        searchBox.setTextColor(0xFFFFFFFF);
        searchBox.setHint(Component.translatable("screen.minevoice.group.search").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        searchBox.setValue(renderedSearch);
        searchBox.setResponder(this::updateSearch);
        addRenderableWidget(searchBox);
        if (restoreSearchFocus) {
            searchBox.setFocused(true);
            setInitialFocus(searchBox);
            restoreSearchFocus = false;
        }

        if (selectedTab == ChannelTab.PUBLIC) {
            initPublicRows(left, top + ROW_TOP_OFFSET);
        } else {
            initGroupTab(left, top + ROW_TOP_OFFSET, self);
        }

        addButton(Component.translatable("gui.back"), contentLeft, top + panelHeight() - 25, 74, 19,
                button -> Minecraft.getInstance().setScreen(parent));
    }

    private void initPublicRows(int left, int rowTop) {
        List<VoiceRosterEntry> players = filteredPlayers(false);
        int visibleRows = rowsPerPage(rowTop);
        listScroll = clampScroll(listScroll, players.size(), visibleRows);
        for (int index = 0; index < Math.min(players.size() - listScroll, visibleRows); index++) {
            addPlayerControls(players.get(listScroll + index), left, rowTop + index * ROW_HEIGHT);
        }
    }

    private void initGroupTab(int left, int rowTop, VoiceRosterEntry self) {
        if (self != null && self.groupId() != null) {
            List<VoiceRosterEntry> members = filteredGroupMembers(self.groupId());
            int visibleRows = rowsPerPage(rowTop + 19);
            listScroll = clampScroll(listScroll, members.size(), visibleRows);
            for (int index = 0; index < Math.min(members.size() - listScroll, visibleRows); index++) {
                addPlayerControls(members.get(listScroll + index), left, rowTop + 19 + index * ROW_HEIGHT);
            }
            addButton(Component.translatable("screen.minevoice.group.leave"),
                    left + panelWidth() - 80, panelTop() + panelHeight() - 25, 74, 19,
                    button -> MineVoiceClientBootstrap.leaveGroup());
            return;
        }

        int formLeft = searchLeft(left);
        int formWidth = searchWidth();
        int createButtonWidth = 54;
        int fieldGap = 6;
        int fieldWidth = (formWidth - createButtonWidth - fieldGap * 2) / 2;
        int createButtonX = formLeft + formWidth - createButtonWidth;
        int passwordX = formLeft + fieldWidth + fieldGap;

        int firstRowTop = rowTop + 2;
        int fieldHeight = 20;
        groupName = new EditBox(font, formLeft, firstRowTop, fieldWidth, fieldHeight, Component.translatable("screen.minevoice.group.name"));
        groupName.setMaxLength(32);
        groupName.setHint(Component.translatable("screen.minevoice.group.name"));
        addRenderableWidget(groupName);

        createPassword = new EditBox(font, passwordX, firstRowTop, fieldWidth, fieldHeight, Component.translatable("screen.minevoice.group.password"));
        createPassword.setMaxLength(64);
        createPassword.setHint(Component.translatable("screen.minevoice.group.password_optional"));
        addRenderableWidget(createPassword);

        addButton(Component.translatable("screen.minevoice.group.create"),
                createButtonX, firstRowTop, createButtonWidth, fieldHeight,
                button -> MineVoiceClientBootstrap.createGroup(groupName.getValue(), createPassword.getValue()));

        joinPassword = new EditBox(font, formLeft, rowTop + 28, formWidth, fieldHeight, Component.translatable("screen.minevoice.group.join_password"));
        joinPassword.setMaxLength(64);
        joinPassword.setHint(Component.translatable("screen.minevoice.group.join_password"));
        addRenderableWidget(joinPassword);

        List<VoiceGroupSummary> groups = filteredGroups();
        int visibleRows = Math.min(2, rowsPerPage(rowTop + GROUP_LIST_OFFSET));
        listScroll = clampScroll(listScroll, groups.size(), visibleRows);
        for (int index = 0; index < Math.min(groups.size() - listScroll, visibleRows); index++) {
            VoiceGroupSummary group = groups.get(listScroll + index);
            int y = rowTop + GROUP_LIST_OFFSET + index * ROW_HEIGHT;
            addButton(Component.translatable("screen.minevoice.group.join"),
                    searchRight(left) - 54, y + 8, 54, 18,
                    button -> MineVoiceClientBootstrap.joinGroup(group.groupId(), joinPassword.getValue()));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        int left = panelLeft();
        int top = panelTop();
        Minecraft minecraft = Minecraft.getInstance();
        MineVoicePanelStyle.render(graphics, left, top, panelWidth(), panelHeight());
        drawCenteredNoShadow(graphics, minecraft, Component.translatable("screen.minevoice.group.title"), left + panelWidth() / 2, top + 7, TEXT);
        renderSocialFrame(graphics, left, top);
        if (selectedTab == ChannelTab.PUBLIC) {
            renderPublicTab(graphics, minecraft, left, top + ROW_TOP_OFFSET);
        } else {
            renderGroupTab(graphics, minecraft, left, top + ROW_TOP_OFFSET);
        }
    }

    @Override
    public void tick() {
        super.tick();
        VoiceRosterEntry self = selfEntry();
        UUID currentGroupId = self == null ? null : self.groupId();
        if (!Objects.equals(renderedGroupId, currentGroupId)) {
            rebuildWidgets();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null
                && searchBox.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            return true;
        }
        if (searchBox == null || !searchBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
                scrollRows(visibleScrollableRows());
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
                scrollRows(-visibleScrollableRows());
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_HOME) {
                setScroll(0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_END) {
                setScroll(scrollableRows());
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOverScrollableList(mouseX, mouseY)) {
            scrollRows(-(int) Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private void renderPublicTab(GuiGraphics graphics, Minecraft minecraft, int left, int rowTop) {
        List<VoiceRosterEntry> players = filteredPlayers(false);
        if (players.isEmpty()) {
            graphics.drawCenteredString(
                    minecraft.font,
                    Component.translatable("screen.minevoice.group.no_search_results"),
                    left + panelWidth() / 2,
                    rowTop + 34,
                    SUBTLE_TEXT
            );
            return;
        }
        int visibleRows = rowsPerPage(rowTop);
        for (int index = 0; index < Math.min(players.size() - listScroll, visibleRows); index++) {
            renderPlayerRow(graphics, minecraft, players.get(listScroll + index), left, rowTop + index * ROW_HEIGHT);
        }
        renderScrollbar(graphics, left, rowTop, players.size(), visibleRows);
    }

    private void renderGroupTab(GuiGraphics graphics, Minecraft minecraft, int left, int rowTop) {
        VoiceRosterEntry self = selfEntry();
        if (self != null && self.groupId() != null) {
            graphics.drawString(minecraft.font, Component.translatable("screen.minevoice.group.current", self.groupName()), left + 10, rowTop + 2, SUBTLE_TEXT);
            List<VoiceRosterEntry> members = filteredGroupMembers(self.groupId());
            if (members.isEmpty()) {
                graphics.drawCenteredString(
                        minecraft.font,
                        Component.translatable("screen.minevoice.group.no_search_results"),
                        left + panelWidth() / 2,
                        rowTop + 48,
                        SUBTLE_TEXT
                );
                return;
            }
            int visibleRows = rowsPerPage(rowTop + 19);
            for (int index = 0; index < Math.min(members.size() - listScroll, visibleRows); index++) {
                renderPlayerRow(graphics, minecraft, members.get(listScroll + index), left, rowTop + 19 + index * ROW_HEIGHT);
            }
            renderScrollbar(graphics, left, rowTop + 19, members.size(), visibleRows);
            return;
        }

        List<VoiceGroupSummary> groups = filteredGroups();
        graphics.drawString(minecraft.font, Component.translatable("screen.minevoice.group.joinable"), searchLeft(left), rowTop + GROUP_JOINABLE_LABEL_OFFSET, SUBTLE_TEXT);
        if (groups.isEmpty()) {
            graphics.drawCenteredString(
                    minecraft.font,
                    Component.translatable("screen.minevoice.group.empty"),
                    left + panelWidth() / 2,
                    rowTop + 100,
                    SUBTLE_TEXT
            );
            return;
        }
        int visibleRows = Math.min(2, rowsPerPage(rowTop + GROUP_LIST_OFFSET));
        for (int index = 0; index < Math.min(groups.size() - listScroll, visibleRows); index++) {
            VoiceGroupSummary group = groups.get(listScroll + index);
            int y = rowTop + GROUP_LIST_OFFSET + index * ROW_HEIGHT;
            MineVoicePanelStyle.renderSocialRow(graphics, searchLeft(left), y, searchWidth(), ROW_HEIGHT, false);
            String lock = group.passwordProtected() ? " *" : "";
            String groupNameText = clippedToWidth(minecraft, group.groupName() + lock, panelWidth() - 105);
            graphics.drawString(minecraft.font, Component.literal(groupNameText), searchLeft(left) + 8, y + 13, LIST_TEXT, false);
            graphics.drawString(minecraft.font, Integer.toString(group.memberCount()), searchRight(left) - 70, y + 13, SUBTLE_TEXT, false);
        }
        renderScrollbar(graphics, left, rowTop + GROUP_LIST_OFFSET, groups.size(), visibleRows);
    }

    private void renderPlayerRow(GuiGraphics graphics, Minecraft minecraft, VoiceRosterEntry entry, int left, int y) {
        boolean speaking = MineVoiceClientBootstrap.speakerTracker().isSpeaking(entry.playerId());
        int rowLeft = listContentLeft(left);
        int rowWidth = listContentWidth();
        boolean self = isSelf(entry.playerId());
        MineVoicePanelStyle.renderSocialRow(graphics, rowLeft, y, rowWidth, ROW_HEIGHT, speaking);
        VoiceParticipantOverlay.drawFace(graphics, minecraft, entry.playerId(), rowLeft + 4, y + 6, 24);
        int nameX = rowLeft + 36;
        int controlsLeft = searchRight(left) - PLAYER_CONTROLS_WIDTH;
        int nameRight = self ? left + panelWidth() - 52 : controlsLeft - 18;
        String displayName = clippedToWidth(minecraft, entry.playerName(), Math.max(52, nameRight - nameX));
        graphics.drawString(minecraft.font, displayName, nameX, y + 13, LIST_TEXT, false);
        ResourceLocation statusIcon = playerStatusIcon(entry, speaking);
        int iconLimit = self ? left + panelWidth() - 58 : controlsLeft - 14;
        int iconX = Math.min(nameX + minecraft.font.width(displayName) + 4, iconLimit);
        graphics.blitSprite(statusIcon, iconX, y + 12, 12, 12);
        if (self) {
            graphics.drawString(minecraft.font, Component.translatable("screen.minevoice.group.self"), searchRight(left) - 37, y + 13, SUBTLE_TEXT, false);
        } else if (speaking) {
            graphics.fill(rowLeft, y + ROW_HEIGHT - 3, rowLeft + rowWidth, y + ROW_HEIGHT, SPEAKING);
        }
    }

    private void addPlayerControls(VoiceRosterEntry entry, int left, int y) {
        if (isSelf(entry.playerId())) {
            return;
        }
        int controlsLeft = searchRight(left) - PLAYER_CONTROLS_WIDTH;
        addRenderableWidget(new PlayerVolumeSlider(controlsLeft, y + 9, PLAYER_SLIDER_WIDTH, 18, entry.playerId()));
        addRenderableWidget(playerMuteButton(entry.playerId(), controlsLeft + PLAYER_SLIDER_WIDTH + 6, y + 8));
    }

    private Component playerMuteMessage(UUID playerId) {
        return Component.translatable(MineVoiceClientBootstrap.playerMuted(playerId)
                ? "screen.minevoice.group.unmute_player"
                : "screen.minevoice.group.mute_player");
    }

    private SpriteIconButton playerMuteButton(UUID playerId, int x, int y) {
        ResourceLocation icon = MineVoiceClientBootstrap.playerMuted(playerId) ? SPEAKER_MUTED_ICON : SPEAKER_ICON;
        SpriteIconButton button = SpriteIconButton.builder(Component.empty(), press -> {
                    MineVoiceClientBootstrap.togglePlayerMuted(playerId);
                    rebuildWidgets();
                }, true)
                .size(PLAYER_MUTE_SIZE, PLAYER_MUTE_SIZE)
                .sprite(icon, 16, 16)
                .build();
        button.setPosition(x, y);
        button.setTooltip(Tooltip.create(playerMuteMessage(playerId)));
        return button;
    }

    private void switchTab(ChannelTab tab) {
        selectedTab = tab;
        renderedSearch = "";
        listScroll = 0;
        rebuildWidgets();
    }

    private void updateSearch(String value) {
        if (!value.equals(renderedSearch)) {
            renderedSearch = value;
            listScroll = 0;
            restoreSearchFocus = true;
            rebuildWidgets();
        }
    }

    private List<VoiceRosterEntry> filteredPlayers(boolean groupOnly) {
        String query = searchText().toLowerCase(Locale.ROOT);
        return MineVoiceClientBootstrap.voiceDirectory().entries().stream()
                .filter(entry -> !groupOnly || entry.groupId() != null)
                .filter(entry -> query.isBlank() || entry.playerName().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private List<VoiceRosterEntry> filteredGroupMembers(UUID groupId) {
        String query = searchText().toLowerCase(Locale.ROOT);
        return MineVoiceClientBootstrap.voiceDirectory().groupMembers(groupId).stream()
                .filter(entry -> query.isBlank() || entry.playerName().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private List<VoiceGroupSummary> filteredGroups() {
        String query = searchText().toLowerCase(Locale.ROOT);
        return MineVoiceClientBootstrap.voiceDirectory().groups().stream()
                .filter(group -> query.isBlank() || group.groupName().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private String searchText() {
        return renderedSearch;
    }

    private VoiceRosterEntry selfEntry() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? null : MineVoiceClientBootstrap.voiceDirectory().get(minecraft.player.getUUID());
    }

    private boolean isSelf(UUID playerId) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.getUUID().equals(playerId);
    }

    private ResourceLocation playerStatusIcon(VoiceRosterEntry entry, boolean speaking) {
        ClientAudioSettings settings = MineVoiceClientBootstrap.settings();
        if (settings.deafened()) {
            return SPEAKER_MUTED_ICON;
        }
        if (isSelf(entry.playerId()) && settings.muted()) {
            return MICROPHONE_MUTED_ICON;
        }
        if (entry.muted()) {
            return MICROPHONE_MUTED_ICON;
        }
        if (!isSelf(entry.playerId()) && MineVoiceClientBootstrap.playerMuted(entry.playerId())) {
            return SPEAKER_MUTED_ICON;
        }
        return speaking ? MICROPHONE_ICON : SPEAKER_ICON;
    }

    private void renderSocialFrame(GuiGraphics graphics, int left, int top) {
        int frameTop = top + FRAME_TOP_OFFSET;
        int frameHeight = panelHeight() - FRAME_TOP_OFFSET - 30;
        MineVoicePanelStyle.renderSocialList(graphics, socialFrameLeft(left), frameTop, socialFrameWidth(), frameHeight);
        int searchY = frameTop + SEARCH_BOX_TOP_OFFSET;
        graphics.blitSprite(SEARCH_ICON, socialFrameLeft(left) + SEARCH_ICON_LEFT_OFFSET, searchY + 2, 12, 12);
    }

    private void renderScrollbar(GuiGraphics graphics, int left, int rowTop, int totalRows, int visibleRows) {
        if (totalRows <= visibleRows) {
            return;
        }
        int trackLeft = listContentRight(left) - 8;
        int trackTop = rowTop + 1;
        int trackHeight = visibleRows * ROW_HEIGHT - 2;
        int thumbHeight = Math.max(16, trackHeight * visibleRows / totalRows);
        int maxScroll = Math.max(1, totalRows - visibleRows);
        int thumbTop = trackTop + (trackHeight - thumbHeight) * listScroll / maxScroll;
        graphics.fill(trackLeft, trackTop, trackLeft + 6, trackTop + trackHeight, 0xFF202020);
        graphics.fill(trackLeft + 1, thumbTop, trackLeft + 5, thumbTop + thumbHeight, 0xFFCFCFCF);
    }

    private int scrollableRows() {
        VoiceRosterEntry self = selfEntry();
        if (selectedTab == ChannelTab.PUBLIC) {
            return filteredPlayers(false).size();
        }
        if (self != null && self.groupId() != null) {
            return filteredGroupMembers(self.groupId()).size();
        }
        return filteredGroups().size();
    }

    private int visibleScrollableRows() {
        VoiceRosterEntry self = selfEntry();
        int top = panelTop() + ROW_TOP_OFFSET;
        if (selectedTab == ChannelTab.GROUP && self != null && self.groupId() != null) {
            return rowsPerPage(top + 19);
        }
        if (selectedTab == ChannelTab.GROUP) {
            return Math.min(2, rowsPerPage(top + GROUP_LIST_OFFSET));
        }
        return rowsPerPage(top);
    }

    private void scrollRows(int rows) {
        setScroll(listScroll + rows);
    }

    private void setScroll(int scroll) {
        int nextScroll = clampScroll(scroll, scrollableRows(), visibleScrollableRows());
        if (nextScroll != listScroll) {
            listScroll = nextScroll;
            rebuildWidgets();
        }
    }

    private boolean isMouseOverScrollableList(double mouseX, double mouseY) {
        int top = scrollListTop();
        int height = Math.max(ROW_HEIGHT, visibleScrollableRows() * ROW_HEIGHT);
        int left = panelLeft();
        return mouseX >= listContentLeft(left)
                && mouseX <= listContentRight(left)
                && mouseY >= top
                && mouseY <= top + height;
    }

    private int scrollListTop() {
        VoiceRosterEntry self = selfEntry();
        int top = panelTop() + ROW_TOP_OFFSET;
        if (selectedTab == ChannelTab.GROUP && self != null && self.groupId() != null) {
            return top + 19;
        }
        if (selectedTab == ChannelTab.GROUP) {
            return top + GROUP_LIST_OFFSET;
        }
        return top;
    }

    private int clampScroll(int scroll, int totalRows, int visibleRows) {
        return Math.max(0, Math.min(Math.max(0, totalRows - visibleRows), scroll));
    }

    private int rowsPerPage(int rowTop) {
        return Math.max(1, (panelTop() + panelHeight() - 34 - rowTop) / ROW_HEIGHT);
    }

    private int panelWidth() {
        return Math.min(PANEL_MAX_WIDTH, Math.max(PANEL_MIN_WIDTH, width - 12));
    }

    private int panelHeight() {
        return Math.min(PANEL_MAX_HEIGHT, Math.max(PANEL_MIN_HEIGHT, height - 8));
    }

    private int panelLeft() {
        return width / 2 - panelWidth() / 2;
    }

    private int panelTop() {
        return Math.max(4, height / 2 - panelHeight() / 2);
    }

    private int socialFrameLeft(int left) {
        return left + 6;
    }

    private int socialFrameWidth() {
        return panelWidth() - 12;
    }

    private int searchLeft(int left) {
        return socialFrameLeft(left) + SEARCH_BOX_LEFT_OFFSET;
    }

    private int searchWidth() {
        return socialFrameWidth() - SEARCH_BOX_LEFT_OFFSET - 8;
    }

    private int searchRight(int left) {
        return searchLeft(left) + searchWidth();
    }

    private int listContentLeft(int left) {
        return socialFrameLeft(left) + 6;
    }

    private int listContentRight(int left) {
        return socialFrameLeft(left) + socialFrameWidth() - 6;
    }

    private int listContentWidth() {
        return socialFrameWidth() - 12;
    }

    private Button addButton(Component message, int x, int y, int width, int height, Button.OnPress onPress) {
        Button button = Button.builder(message, onPress).bounds(x, y, width, height).build();
        addRenderableWidget(button);
        return button;
    }

    private static void drawCenteredNoShadow(GuiGraphics graphics, Minecraft minecraft, Component text, int centerX, int y, int color) {
        graphics.drawString(minecraft.font, text, centerX - minecraft.font.width(text) / 2, y, color, false);
    }

    private static String clippedToWidth(Minecraft minecraft, String value, int maxWidth) {
        if (minecraft.font.width(value) <= maxWidth) {
            return value;
        }
        String clipped = value;
        while (minecraft.font.width(clipped + "...") > maxWidth && clipped.length() > 1) {
            clipped = clipped.substring(0, clipped.length() - 1);
        }
        return clipped + "...";
    }

    private enum ChannelTab {
        PUBLIC,
        GROUP
    }

    private static final class PlayerVolumeSlider extends AbstractSliderButton {
        private final UUID playerId;

        private PlayerVolumeSlider(int x, int y, int width, int height, UUID playerId) {
            super(x, y, width, height, Component.empty(), MineVoiceClientBootstrap.playerVolume(playerId) / 2.0F);
            this.playerId = playerId;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(Math.round(value * 200.0D) + "%"));
        }

        @Override
        protected void applyValue() {
            MineVoiceClientBootstrap.setPlayerVolume(playerId, (float) value * 2.0F);
        }
    }
}

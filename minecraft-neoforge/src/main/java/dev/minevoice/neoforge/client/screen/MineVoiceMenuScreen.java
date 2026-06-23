package dev.minevoice.neoforge.client.screen;

import dev.minevoice.neoforge.client.ClientAudioSettings;
import dev.minevoice.neoforge.client.MineVoiceClientBootstrap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MineVoiceMenuScreen extends Screen {
    private static final int PANEL_WIDTH = 195;
    private static final int PANEL_HEIGHT = 76;
    private static final int EDGE = 6;
    private static final int ACTION_WIDTH = 75;
    private static final int ACTION_HEIGHT = 20;
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
    public MineVoiceMenuScreen(Screen parent) {
        super(Component.translatable("screen.minevoice.menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();

        addRenderableWidget(Button.builder(Component.translatable("screen.minevoice.action.settings"),
                        button -> Minecraft.getInstance().setScreen(new MineVoiceSettingsScreen(this, MineVoiceClientBootstrap.uiController())))
                .bounds(left + EDGE, top + 21, ACTION_WIDTH, ACTION_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.minevoice.action.group"),
                        button -> Minecraft.getInstance().setScreen(new MineVoiceGroupScreen(this)))
                .bounds(left + PANEL_WIDTH - EDGE - ACTION_WIDTH, top + 21, ACTION_WIDTH, ACTION_HEIGHT)
                .build());

        ClientAudioSettings settings = MineVoiceClientBootstrap.settings();
        addRenderableWidget(iconButton(
                settings.muted() ? MICROPHONE_MUTED_ICON : MICROPHONE_ICON,
                Component.translatable(settings.muted() ? "screen.minevoice.menu.unmute" : "screen.minevoice.menu.mute"),
                left + EDGE,
                top + PANEL_HEIGHT - EDGE - ACTION_HEIGHT,
                button -> {
                    MineVoiceClientBootstrap.toggleMuted();
                    rebuildWidgets();
                }
        ));
        addRenderableWidget(iconButton(
                settings.deafened() ? SPEAKER_MUTED_ICON : SPEAKER_ICON,
                Component.translatable(settings.deafened() ? "screen.minevoice.menu.undeafen" : "screen.minevoice.menu.deafen"),
                left + EDGE + ACTION_HEIGHT + 3,
                top + PANEL_HEIGHT - EDGE - ACTION_HEIGHT,
                button -> {
                    MineVoiceClientBootstrap.toggleDeafened();
                    rebuildWidgets();
                }
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        Minecraft minecraft = Minecraft.getInstance();
        Component title = Component.literal("MineVOICE");
        graphics.drawCenteredString(minecraft.font, title, left + PANEL_WIDTH / 2, top + 7, 0xFF404040);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        MineVoicePanelStyle.render(graphics, panelLeft(), panelTop(), PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private SpriteIconButton iconButton(
            ResourceLocation icon,
            Component tooltip,
            int x,
            int y,
            Button.OnPress onPress
    ) {
        SpriteIconButton button = SpriteIconButton.builder(Component.empty(), onPress, true)
                .size(ACTION_HEIGHT, ACTION_HEIGHT)
                .sprite(icon, 16, 16)
                .build();
        button.setPosition(x, y);
        button.setTooltip(Tooltip.create(tooltip));
        return button;
    }

    private int panelLeft() {
        return width / 2 - PANEL_WIDTH / 2;
    }

    private int panelTop() {
        return height / 2 - PANEL_HEIGHT / 2;
    }
}

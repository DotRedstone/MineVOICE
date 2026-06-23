package dev.minevoice.neoforge.client.screen;

import dev.minevoice.neoforge.client.AudioDevice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Selects one audio device without making the settings page a device dropdown.
 */
public final class MineVoiceDeviceSelectScreen extends Screen {
    private static final int PANEL_WIDTH = 310;
    private static final int PANEL_HEIGHT = 204;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;
    private static final int ROWS_PER_PAGE = 6;

    private final Screen parent;
    private final List<AudioDevice> devices;
    private final String selectedId;
    private final Consumer<String> onSelected;
    private int page;

    public MineVoiceDeviceSelectScreen(
            Screen parent,
            Component title,
            List<AudioDevice> devices,
            String selectedId,
            Consumer<String> onSelected
    ) {
        super(title);
        this.parent = parent;
        this.devices = devices;
        this.selectedId = selectedId;
        this.onSelected = onSelected;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int firstDevice = page * ROWS_PER_PAGE;
        int lastDevice = Math.min(devices.size(), firstDevice + ROWS_PER_PAGE);

        for (int deviceIndex = firstDevice; deviceIndex < lastDevice; deviceIndex++) {
            AudioDevice device = devices.get(deviceIndex);
            Button button = Button.builder(displayName(device), ignored -> select(device))
                    .bounds(left + 10, top + 31 + (deviceIndex - firstDevice) * (ROW_HEIGHT + ROW_GAP), PANEL_WIDTH - 20, ROW_HEIGHT)
                    .build();
            button.active = !device.id().equals(selectedId);
            addRenderableWidget(button);
        }

        int footerTop = top + PANEL_HEIGHT - 28;
        int pageCount = pageCount();
        if (pageCount > 1) {
            addRenderableWidget(Button.builder(Component.translatable("screen.minevoice.device.previous"), ignored -> {
                        page = Math.max(0, page - 1);
                        rebuildWidgets();
                    }).bounds(left + 10, footerTop, 72, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.minevoice.device.next"), ignored -> {
                        page = Math.min(pageCount - 1, page + 1);
                        rebuildWidgets();
                    }).bounds(left + 88, footerTop, 72, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), ignored -> onClose())
                .bounds(left + PANEL_WIDTH - 82, footerTop, 72, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        graphics.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 10, 0xFF404040);
        if (pageCount() > 1) {
            graphics.drawString(font, Component.translatable("screen.minevoice.device.page", page + 1, pageCount()),
                    left + 168, top + PANEL_HEIGHT - 22, 0xFF606060);
        }
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

    private void select(AudioDevice device) {
        onSelected.accept(device.id());
        Minecraft.getInstance().setScreen(parent);
    }

    private Component displayName(AudioDevice device) {
        return device.systemDefault()
                ? Component.translatable("screen.minevoice.system_default")
                : Component.literal(device.displayName());
    }

    private int pageCount() {
        return Math.max(1, (devices.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
    }

    private int panelLeft() {
        return width / 2 - PANEL_WIDTH / 2;
    }

    private int panelTop() {
        return Math.max(4, height / 2 - PANEL_HEIGHT / 2);
    }
}

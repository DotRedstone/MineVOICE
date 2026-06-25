package dev.minevoice.neoforge.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared vanilla-style container framing for MineVOICE screens.
 */
public final class MineVoicePanelStyle {
    private static final ResourceLocation INVENTORY_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/inventory.png"
    );
    private static final int INVENTORY_WIDTH = 176;
    private static final int INVENTORY_HEIGHT = 166;
    private static final int EDGE_SIZE = 4;
    private static final int FILL = 0xFFC6C6C6;

    private MineVoicePanelStyle() {
    }

    public static void render(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width < EDGE_SIZE * 2 || height < EDGE_SIZE * 2) {
            return;
        }
        int innerWidth = width - EDGE_SIZE * 2;
        int innerHeight = height - EDGE_SIZE * 2;
        graphics.fill(x + EDGE_SIZE, y + EDGE_SIZE, x + EDGE_SIZE + innerWidth, y + EDGE_SIZE + innerHeight, FILL);

        blit(graphics, x, y, 0, 0, EDGE_SIZE, EDGE_SIZE);
        blit(graphics, x + width - EDGE_SIZE, y, INVENTORY_WIDTH - EDGE_SIZE, 0, EDGE_SIZE, EDGE_SIZE);
        blit(graphics, x, y + height - EDGE_SIZE, 0, INVENTORY_HEIGHT - EDGE_SIZE, EDGE_SIZE, EDGE_SIZE);
        blit(graphics, x + width - EDGE_SIZE, y + height - EDGE_SIZE, INVENTORY_WIDTH - EDGE_SIZE, INVENTORY_HEIGHT - EDGE_SIZE, EDGE_SIZE, EDGE_SIZE);

        for (int offset = 0; offset < innerWidth; offset++) {
            blit(graphics, x + EDGE_SIZE + offset, y, 10, 0, 1, EDGE_SIZE);
            blit(graphics, x + EDGE_SIZE + offset, y + height - EDGE_SIZE, 10, INVENTORY_HEIGHT - EDGE_SIZE, 1, EDGE_SIZE);
        }
        for (int offset = 0; offset < innerHeight; offset++) {
            blit(graphics, x, y + EDGE_SIZE + offset, 0, 10, EDGE_SIZE, 1);
            blit(graphics, x + width - EDGE_SIZE, y + EDGE_SIZE + offset, INVENTORY_WIDTH - EDGE_SIZE, 10, EDGE_SIZE, 1);
        }
    }

    public static void renderInset(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF8A8A8A);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFB6B6B6);
        graphics.fill(x, y, x + width, y + 1, 0xFF404040);
        graphics.fill(x, y, x + 1, y + height, 0xFF404040);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);
    }

    public static void renderListRow(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF1E1E1E);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFC0C0C0);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFFD0D0D0);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF8A8A8A);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, 0xFF8A8A8A);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFFFFFFFF);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFFFFFFFF);
    }

    public static void renderSocialList(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFE6E6E6);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF1E1E1E);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xFF383838);
    }

    public static void renderSocialRow(GuiGraphics graphics, int x, int y, int width, int height, boolean active) {
        graphics.fill(x, y, x + width, y + height, active ? 0xFF4F604F : 0xFF4A4A4A);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF3A3A3A);
    }

    public static void renderSocialSearch(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF000000);
    }

    private static void blit(GuiGraphics graphics, int x, int y, int sourceX, int sourceY, int width, int height) {
        graphics.blit(INVENTORY_TEXTURE, x, y, sourceX, sourceY, width, height);
    }
}

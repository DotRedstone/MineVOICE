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

    private static void blit(GuiGraphics graphics, int x, int y, int sourceX, int sourceY, int width, int height) {
        graphics.blit(INVENTORY_TEXTURE, x, y, sourceX, sourceY, width, height);
    }
}

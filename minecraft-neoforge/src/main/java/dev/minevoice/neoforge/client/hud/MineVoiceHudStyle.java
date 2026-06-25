package dev.minevoice.neoforge.client.hud;

import net.minecraft.client.gui.GuiGraphics;

public final class MineVoiceHudStyle {
    public static final int MIN_ICON_SIZE = 12;
    public static final int MAX_ICON_SIZE = 24;
    public static final int SLOT_PADDING = 2;
    public static final int GAP = 4;
    public static final int BACKGROUND = 0x8C101820;
    public static final int BACKGROUND_ACTIVE = 0xAA15251D;
    public static final int BORDER = 0xCC59636D;
    public static final int BORDER_ACTIVE = 0xFF63D597;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int SUBTLE_TEXT = 0xFFC9D1D9;
    public static final int DANGER = 0xFFE05A5A;
    public static final int SPEAKING = 0xFF63D597;

    private MineVoiceHudStyle() {
    }

    public static int clampIconSize(int size) {
        return Math.max(MIN_ICON_SIZE, Math.min(MAX_ICON_SIZE, size));
    }

    public static int nextIconSize(int size) {
        int current = clampIconSize(size);
        if (current < 16) {
            return 16;
        }
        if (current < 20) {
            return 20;
        }
        if (current < 24) {
            return 24;
        }
        return 12;
    }

    public static int slotSize(int iconSize) {
        return clampIconSize(iconSize) + SLOT_PADDING * 2;
    }

    public static void renderSlot(GuiGraphics graphics, int x, int y, int size, boolean active) {
        graphics.fill(x, y, x + size, y + size, active ? BACKGROUND_ACTIVE : BACKGROUND);
        int border = active ? BORDER_ACTIVE : BORDER;
        graphics.fill(x, y, x + size, y + 1, border);
        graphics.fill(x, y + size - 1, x + size, y + size, 0xCC05080A);
        graphics.fill(x, y, x + 1, y + size, border);
        graphics.fill(x + size - 1, y, x + size, y + size, 0xCC05080A);
    }

    public static void renderBar(GuiGraphics graphics, int x, int y, int width, int height, float level, int color) {
        graphics.fill(x, y, x + width, y + height, 0x8010151A);
        int filledWidth = Math.round(width * Math.max(0.0F, Math.min(1.0F, level)));
        if (filledWidth > 0) {
            graphics.fill(x, y, x + filledWidth, y + height, color);
        }
    }

    public static void renderRow(GuiGraphics graphics, int x, int y, int width, int height, boolean active) {
        graphics.fill(x, y, x + width, y + height, active ? 0x9915261D : 0x78101820);
        graphics.fill(x, y, x + width, y + 1, active ? BORDER_ACTIVE : 0x8859636D);
        graphics.fill(x, y + height - 1, x + width, y + height, 0x8805080A);
    }
}

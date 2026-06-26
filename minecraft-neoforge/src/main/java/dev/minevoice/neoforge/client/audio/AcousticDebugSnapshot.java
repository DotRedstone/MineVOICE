package dev.minevoice.neoforge.client.audio;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Immutable world-space rays captured on the client tick and rendered later on the level stage.
 */
public record AcousticDebugSnapshot(boolean enabled, List<Line> lines) {
    public static final AcousticDebugSnapshot DISABLED = new AcousticDebugSnapshot(false, List.of());

    public AcousticDebugSnapshot {
        lines = List.copyOf(lines);
    }

    public record Line(Vec3 from, Vec3 to, int red, int green, int blue, int alpha) {
        public Line {
            red = clampColor(red);
            green = clampColor(green);
            blue = clampColor(blue);
            alpha = clampColor(alpha);
        }

        private static int clampColor(int value) {
            return Math.max(0, Math.min(255, value));
        }
    }
}

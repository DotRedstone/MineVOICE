package dev.minevoice.neoforge.client.audio;

import net.neoforged.fml.ModList;

public final class SoundPhysicsCompat {
    private static final String[] MOD_IDS = {
            "sound_physics_remastered",
            "soundphysics",
            "sound_physics"
    };

    private SoundPhysicsCompat() {
    }

    public static boolean installed() {
        ModList modList = ModList.get();
        for (String modId : MOD_IDS) {
            if (modList.isLoaded(modId)) {
                return true;
            }
        }
        return false;
    }

    public static String backendName() {
        return installed() ? "sound-physics-detected" : "minevoice";
    }
}

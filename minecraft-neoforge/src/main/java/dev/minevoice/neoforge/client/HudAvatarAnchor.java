package dev.minevoice.neoforge.client;

public enum HudAvatarAnchor {
    TOP_LEFT("screen.minevoice.hud_avatar_anchor.top_left"),
    TOP_RIGHT("screen.minevoice.hud_avatar_anchor.top_right"),
    BOTTOM_LEFT("screen.minevoice.hud_avatar_anchor.bottom_left"),
    BOTTOM_RIGHT("screen.minevoice.hud_avatar_anchor.bottom_right");

    private final String translationKey;

    HudAvatarAnchor(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public HudAvatarAnchor next() {
        HudAvatarAnchor[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}

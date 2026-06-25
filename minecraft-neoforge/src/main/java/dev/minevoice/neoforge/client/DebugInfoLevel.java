package dev.minevoice.neoforge.client;

public enum DebugInfoLevel {
    OFF("screen.minevoice.debug_level.off"),
    BASIC("screen.minevoice.debug_level.basic"),
    VERBOSE("screen.minevoice.debug_level.verbose");

    private final String translationKey;

    DebugInfoLevel(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public DebugInfoLevel next() {
        DebugInfoLevel[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}

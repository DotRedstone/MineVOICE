package dev.minevoice.common.util;

import java.time.Instant;

public final class MineVoiceLogger {
    private final String name;
    private final boolean debugEnabled;

    public MineVoiceLogger(String name, boolean debugEnabled) {
        this.name = name;
        this.debugEnabled = debugEnabled;
    }

    public void info(String message) {
        log("INFO", message);
    }

    public void debug(String message) {
        if (debugEnabled) {
            log("DEBUG", message);
        }
    }

    public void warn(String message) {
        log("WARN", message);
    }

    private void log(String level, String message) {
        System.out.printf("%s [%s] [%s] %s%n", Instant.now(), level, name, message);
    }
}

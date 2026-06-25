package dev.minevoice.neoforge.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

public final class FilePlayerVolumeStore {
    private static final float DEFAULT_VOLUME = 1.0F;
    private static final float MAX_VOLUME = 2.0F;

    private final Path path;
    private final Map<UUID, Float> volumes = new LinkedHashMap<>();
    private final Set<UUID> mutedPlayers = new LinkedHashSet<>();
    private boolean loaded;

    public FilePlayerVolumeStore(Path path) {
        this.path = path;
    }

    public synchronized float volume(UUID playerId) {
        ensureLoaded();
        return volumes.getOrDefault(playerId, DEFAULT_VOLUME);
    }

    public synchronized void setVolume(UUID playerId, float volume) {
        ensureLoaded();
        float clamped = clamp(volume);
        if (Math.abs(clamped - DEFAULT_VOLUME) < 0.001F) {
            volumes.remove(playerId);
        } else {
            volumes.put(playerId, clamped);
        }
        save();
    }

    public synchronized boolean muted(UUID playerId) {
        ensureLoaded();
        return mutedPlayers.contains(playerId);
    }

    public synchronized void setMuted(UUID playerId, boolean muted) {
        ensureLoaded();
        if (muted) {
            mutedPlayers.add(playerId);
        } else {
            mutedPlayers.remove(playerId);
        }
        save();
    }

    public synchronized Set<UUID> mutedPlayers() {
        ensureLoaded();
        return Set.copyOf(mutedPlayers);
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.exists(path)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException exception) {
            return;
        }
        for (String key : properties.stringPropertyNames()) {
            try {
                if (key.startsWith("volume.")) {
                    UUID playerId = UUID.fromString(key.substring("volume.".length()));
                    volumes.put(playerId, clamp(Float.parseFloat(properties.getProperty(key))));
                } else if (key.startsWith("muted.")) {
                    UUID playerId = UUID.fromString(key.substring("muted.".length()));
                    if (Boolean.parseBoolean(properties.getProperty(key))) {
                        mutedPlayers.add(playerId);
                    }
                } else {
                    UUID playerId = UUID.fromString(key);
                    volumes.put(playerId, clamp(Float.parseFloat(properties.getProperty(key))));
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore stale or manually edited entries.
            }
        }
    }

    private void save() {
        Properties properties = new Properties();
        for (Map.Entry<UUID, Float> entry : volumes.entrySet()) {
            properties.setProperty("volume." + entry.getKey(), Float.toString(entry.getValue()));
        }
        for (UUID playerId : mutedPlayers) {
            properties.setProperty("muted." + playerId, Boolean.TRUE.toString());
        }
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream output = Files.newOutputStream(path)) {
                properties.store(output, "MineVOICE per-player audio preferences. volume=1.0 is default.");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to save MineVOICE player volumes", exception);
        }
    }

    private static float clamp(float volume) {
        return Math.max(0.0F, Math.min(MAX_VOLUME, volume));
    }
}

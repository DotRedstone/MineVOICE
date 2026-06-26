package dev.minevoice.neoforge.client.audio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Loads the user-editable acoustic material table without exposing file I/O to the audio thread.
 */
public final class AcousticMaterialConfig {
    public static final String FILE_NAME = "minevoice-acoustics.properties";
    private static final long FILE_CHECK_INTERVAL_MILLIS = 1_000L;

    private final Path path;
    private volatile Snapshot snapshot = Snapshot.defaults(0L);
    private volatile long nextFileCheckMillis;
    private volatile FileTime lastModified = FileTime.fromMillis(Long.MIN_VALUE);

    public AcousticMaterialConfig(Path path) {
        this.path = path;
    }

    public Snapshot snapshot() {
        long now = System.currentTimeMillis();
        if (now < nextFileCheckMillis) {
            return snapshot;
        }
        synchronized (this) {
            if (now < nextFileCheckMillis) {
                return snapshot;
            }
            nextFileCheckMillis = now + FILE_CHECK_INTERVAL_MILLIS;
            reloadIfNeeded();
            return snapshot;
        }
    }

    private void reloadIfNeeded() {
        try {
            if (!Files.exists(path)) {
                writeDefaults();
            }
            FileTime modified = Files.getLastModifiedTime(path);
            if (modified.equals(lastModified)) {
                return;
            }
            snapshot = load(Files.newInputStream(path), snapshot.revision() + 1L);
            lastModified = modified;
        } catch (IOException exception) {
            snapshot = Snapshot.defaults(snapshot.revision() + 1L);
        }
    }

    private void writeDefaults() throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Properties properties = new Properties();
        properties.setProperty("enabled", "true");
        properties.setProperty("sourceRefreshIntervalMs", "100");
        properties.setProperty("environmentRefreshIntervalMs", "400");
        properties.setProperty("probeDistance", "18.0");
        properties.setProperty("probeCount", "256");
        properties.setProperty("maxOcclusionSamples", "96");
        properties.setProperty("reflectionStrength", "0.85");
        properties.setProperty("debugRenderRays", "false");
        for (Map.Entry<String, Material> entry : Snapshot.defaultMaterials().entrySet()) {
            properties.setProperty("material." + entry.getKey(), entry.getValue().asPropertyValue());
        }
        properties.setProperty("block.minecraft.obsidian", "stone");
        properties.setProperty("block.minecraft.glass", "glass");
        properties.setProperty("block.minecraft.water", "water");
        try (OutputStream output = Files.newOutputStream(path)) {
            properties.store(output, "MineVOICE acoustic material table: transmissionGain,highFrequencyGain,reflectivity");
        }
    }

    static Snapshot load(InputStream input, long revision) throws IOException {
        Properties properties = new Properties();
        try (input) {
            properties.load(input);
        }
        Map<String, Material> materials = new LinkedHashMap<>(Snapshot.defaultMaterials());
        Map<String, String> blockOverrides = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("material.")) {
                String materialName = normalizeName(key.substring("material.".length()));
                Material fallback = materials.getOrDefault(materialName, Material.DEFAULT);
                materials.put(materialName, Material.parse(properties.getProperty(key), fallback));
            }
        }
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("block.")) {
                String blockId = normalizeBlockId(key.substring("block.".length()));
                String materialName = normalizeName(properties.getProperty(key));
                if (!blockId.isEmpty() && materials.containsKey(materialName)) {
                    blockOverrides.put(blockId, materialName);
                }
            }
        }
        return new Snapshot(
                booleanProperty(properties, "enabled", true),
                clampLong(longProperty(properties, "sourceRefreshIntervalMs", 100L), 50L, 1_000L),
                clampLong(longProperty(properties, "environmentRefreshIntervalMs", 400L), 100L, 5_000L),
                clampDouble(doubleProperty(properties, "probeDistance", 18.0D), 4.0D, 48.0D),
                (int) clampLong(longProperty(properties, "probeCount", 64L), 4L, 512L),
                (int) clampLong(longProperty(properties, "maxOcclusionSamples", 96L), 16L, 256L),
                clampFloat(floatProperty(properties, "reflectionStrength", 0.85F), 0.0F, 2.0F),
                booleanProperty(properties, "debugRenderRays", false),
                Map.copyOf(materials),
                Map.copyOf(blockOverrides),
                revision
        );
    }

    private static boolean booleanProperty(Properties properties, String key, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)));
    }

    private static long longProperty(Properties properties, String key, long fallback) {
        try {
            return Long.parseLong(properties.getProperty(key, Long.toString(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static double doubleProperty(Properties properties, String key, double fallback) {
        try {
            return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static float floatProperty(Properties properties, String key, float fallback) {
        try {
            return Float.parseFloat(properties.getProperty(key, Float.toString(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeBlockId(String value) {
        String normalized = normalizeName(value);
        if (normalized.contains(":")) {
            return normalized;
        }
        int separator = normalized.indexOf('.');
        return separator > 0 && separator < normalized.length() - 1
                ? normalized.substring(0, separator) + ":" + normalized.substring(separator + 1)
                : normalized;
    }

    public record Snapshot(
            boolean enabled,
            long sourceRefreshIntervalMillis,
            long environmentRefreshIntervalMillis,
            double probeDistance,
            int probeCount,
            int maxOcclusionSamples,
            float reflectionStrength,
            boolean debugRenderRays,
            Map<String, Material> materials,
            Map<String, String> blockOverrides,
            long revision
    ) {
        private static final Map<String, Material> DEFAULT_MATERIALS = Map.ofEntries(
                Map.entry("default", Material.DEFAULT),
                Map.entry("stone", new Material(0.72F, 0.82F, 0.82F)),
                Map.entry("wood", new Material(0.62F, 0.55F, 0.58F)),
                Map.entry("metal", new Material(0.82F, 0.94F, 0.93F)),
                Map.entry("glass", new Material(0.76F, 0.88F, 0.72F)),
                Map.entry("wool", new Material(0.18F, 0.12F, 0.08F)),
                Map.entry("soil", new Material(0.38F, 0.28F, 0.22F)),
                Map.entry("snow", new Material(0.24F, 0.10F, 0.12F)),
                Map.entry("water", new Material(0.30F, 0.08F, 0.10F))
        );

        public static Snapshot defaults(long revision) {
            return new Snapshot(true, 100L, 400L, 18.0D, 12, 96, 0.85F, false, DEFAULT_MATERIALS, Map.of(), revision);
        }

        static Map<String, Material> defaultMaterials() {
            return DEFAULT_MATERIALS;
        }

        public Material materialFor(String blockId, String fallbackMaterial) {
            String materialName = blockOverrides.getOrDefault(blockId, fallbackMaterial);
            return materials.getOrDefault(materialName, materials.getOrDefault("default", Material.DEFAULT));
        }
    }

    public record Material(float transmissionGain, float highFrequencyGain, float reflectivity) {
        static final Material DEFAULT = new Material(0.58F, 0.62F, 0.36F);

        static Material parse(String value, Material fallback) {
            if (value == null || value.isBlank()) {
                return fallback;
            }
            String[] parts = value.split(",");
            if (parts.length != 3) {
                return fallback;
            }
            try {
                return new Material(
                        clampFloat(Float.parseFloat(parts[0].trim()), 0.02F, 1.0F),
                        clampFloat(Float.parseFloat(parts[1].trim()), 0.02F, 1.0F),
                        clampFloat(Float.parseFloat(parts[2].trim()), 0.0F, 1.0F)
                );
            } catch (NumberFormatException exception) {
                return fallback;
            }
        }

        String asPropertyValue() {
            return String.format(Locale.ROOT, "%.2f,%.2f,%.2f", transmissionGain, highFrequencyGain, reflectivity);
        }

        private static float clampFloat(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}

package dev.minevoice.standalone.metrics;

public record ServerRuntimeStats(int activeSessions, BandwidthStats bandwidthStats) {
    public static String currentVersion() {
        return "0.1.0-SNAPSHOT";
    }
}

package dev.minevoice.common.auth;

public record TokenValidationResult(boolean valid, String reason) {
    public static TokenValidationResult accepted() {
        return new TokenValidationResult(true, "accepted");
    }

    public static TokenValidationResult rejected(String reason) {
        return new TokenValidationResult(false, reason);
    }
}

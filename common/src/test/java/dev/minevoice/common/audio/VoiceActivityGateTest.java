package dev.minevoice.common.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceActivityGateTest {
    @Test
    void doesNotOpenBelowNoiseFloor() {
        VoiceActivityGate gate = new VoiceActivityGate(0.02F, 0.08F, 2);

        assertFalse(gate.update(0.01F, 0.0F));
        assertFalse(gate.open());
    }

    @Test
    void keepsOpenBelowThresholdUntilHangoverExpires() {
        VoiceActivityGate gate = new VoiceActivityGate(0.02F, 0.08F, 2);

        assertTrue(gate.update(0.40F, 0.35F));
        assertTrue(gate.update(0.10F, 0.35F));
        assertTrue(gate.update(0.10F, 0.35F));
        assertFalse(gate.update(0.10F, 0.35F));
    }

    @Test
    void staysOpenInsideHysteresisWindow() {
        VoiceActivityGate gate = new VoiceActivityGate(0.02F, 0.08F, 0);

        assertTrue(gate.update(0.40F, 0.35F));
        assertTrue(gate.update(0.30F, 0.35F));
        assertFalse(gate.update(0.20F, 0.35F));
    }
}

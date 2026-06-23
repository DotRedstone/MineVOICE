package dev.minevoice.common.spatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DistanceCullingTest {
    @Test
    void computesFadeRange() {
        DistanceCulling culling = new DistanceCulling(10.0D, 20.0D);

        assertTrue(culling.isAudible(20.0D));
        assertFalse(culling.isAudible(20.1D));
        assertEquals(1.0F, culling.volumeAt(5.0D));
        assertEquals(0.5F, culling.volumeAt(15.0D));
        assertEquals(0.0F, culling.volumeAt(20.0D));
    }

    @Test
    void rejectsInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> new DistanceCulling(30.0D, 20.0D));
    }
}

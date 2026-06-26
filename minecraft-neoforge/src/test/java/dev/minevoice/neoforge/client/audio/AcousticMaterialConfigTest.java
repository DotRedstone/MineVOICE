package dev.minevoice.neoforge.client.audio;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AcousticMaterialConfigTest {
    @Test
    void parsesCustomMaterialBeforeBlockOverrideRegardlessOfPropertyOrder() throws Exception {
        String properties = """
                block.example.foam=custom_foam
                material.custom_foam=0.15,0.10,0.05
                probeCount=99
                reflectionStrength=-1
                debugRenderRays=true
                """;

        AcousticMaterialConfig.Snapshot snapshot = AcousticMaterialConfig.load(
                new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8)),
                7L
        );

        AcousticMaterialConfig.Material foam = snapshot.materialFor("example:foam", "stone");
        assertEquals(0.15F, foam.transmissionGain());
        assertEquals(0.10F, foam.highFrequencyGain());
        assertEquals(0.05F, foam.reflectivity());
        assertEquals(99, snapshot.probeCount());
        assertEquals(0.0F, snapshot.reflectionStrength());
        org.junit.jupiter.api.Assertions.assertTrue(snapshot.debugRenderRays());
        assertEquals(7L, snapshot.revision());
    }

    @Test
    void keepsFallbackForMalformedMaterialValues() throws Exception {
        String properties = "material.stone=not,a,material\nenabled=false\n";

        AcousticMaterialConfig.Snapshot snapshot = AcousticMaterialConfig.load(
                new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8)),
                1L
        );

        AcousticMaterialConfig.Material stone = snapshot.materialFor("minecraft:stone", "stone");
        assertEquals(0.72F, stone.transmissionGain());
        assertFalse(snapshot.enabled());
    }
}

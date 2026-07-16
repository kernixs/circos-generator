package org.mpg.circos;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CircosApplicationTest {
    @Test
    void validatesAndNormalizesPlot() {
        try (InputStream input = getClass().getResourceAsStream("/examples/gains-and-losses.json")) {
            var plot = new CircosApplication().validate(input);
            assertEquals("GRCh37", plot.assemblyId());
            assertEquals("18", plot.segments().getFirst().interval().chromosome());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}

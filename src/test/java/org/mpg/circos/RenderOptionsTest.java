package org.mpg.circos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RenderOptionsTest {
    @Test
    void requiresPositiveConfigurableLimit() {
        assertEquals(25, new RenderOptions(25).maximumEventCount());
        assertThrows(IllegalArgumentException.class, () -> new RenderOptions(0));
    }
}

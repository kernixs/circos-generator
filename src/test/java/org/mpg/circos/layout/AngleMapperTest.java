package org.mpg.circos.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AngleMapperTest {
    @Test
    void mapsChromosomeBoundariesExactly() {
        var sector = new SectorGeometry("1", 100, -Math.PI / 2.0, Math.PI / 2.0);
        var mapper = new AngleMapper();
        assertEquals(-Math.PI / 2.0, mapper.mapBoundary(sector, 0), 1e-12);
        assertEquals(0.0, mapper.mapBoundary(sector, 50), 1e-12);
        assertEquals(Math.PI / 2.0, mapper.mapBoundary(sector, 100), 1e-12);
    }
}

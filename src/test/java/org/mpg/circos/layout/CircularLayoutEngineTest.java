package org.mpg.circos.layout;

import org.junit.jupiter.api.Test;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.TestFixtures;
import org.mpg.circos.assembly.ClasspathAssemblyRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircularLayoutEngineTest {
    @Test
    void createsCanonicalClockwiseSectorsAndFiniteEventGeometry() {
        var plot = new CircosApplication().validate(TestFixtures.open("/examples/patient-bcr-abl1.json"));
        var assembly = new ClasspathAssemblyRepository().load(plot.assemblyId());
        var geometry = new CircularLayoutEngine().layout(plot, assembly);

        assertEquals(24, geometry.sectors().size());
        assertEquals("1", geometry.sectors().getFirst().chromosome());
        assertEquals("Y", geometry.sectors().getLast().chromosome());
        assertEquals(-Math.PI / 2.0, geometry.sectors().getFirst().startAngle(), 1e-12);
        assertTrue(geometry.sectors().getFirst().endAngle() > geometry.sectors().getFirst().startAngle());
        assertEquals(1, geometry.segments().size());
        assertEquals(1, geometry.links().size());
        assertTrue(Double.isFinite(geometry.links().getFirst().ribbon().sourceEndpoint().x()));
    }
}

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

    @Test
    void versionTwoUsesIntervalBoundariesAndMidpointAnchors() {
        var plot = new CircosApplication().validate(TestFixtures.open("/examples/v2-interval-links.json"));
        var assembly = new ClasspathAssemblyRepository().load(plot.assemblyId());
        var geometry = new CircularLayoutEngine().layout(plot, assembly);
        var ribbon = geometry.links().getFirst().ribbon();
        var sourceSector = geometry.sectors().stream()
                .filter(value -> value.chromosome().equals("9")).findFirst().orElseThrow();
        var targetSector = geometry.sectors().stream()
                .filter(value -> value.chromosome().equals("22")).findFirst().orElseThrow();
        var mapper = new AngleMapper();

        assertEquals(mapper.mapBoundary(sourceSector, 133500000), ribbon.sourceStartAngle(), 1e-12);
        assertEquals(mapper.mapBoundary(sourceSector, 133700000), ribbon.sourceEndAngle(), 1e-12);
        assertEquals(mapper.mapBoundary(targetSector, 23500000), ribbon.targetStartAngle(), 1e-12);
        assertEquals(mapper.mapBoundary(targetSector, 23700000), ribbon.targetEndAngle(), 1e-12);
        var expectedSource = PolarPoint.from(geometry.centerX(), geometry.centerY(), ribbon.radius(),
                mapper.mapBoundary(sourceSector, 133600000.0));
        assertEquals(expectedSource.x(), ribbon.sourceEndpoint().x(), 1e-12);
        assertEquals(expectedSource.y(), ribbon.sourceEndpoint().y(), 1e-12);
    }
}

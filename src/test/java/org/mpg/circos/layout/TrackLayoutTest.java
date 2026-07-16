package org.mpg.circos.layout;

import org.junit.jupiter.api.Test;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.TestFixtures;
import org.mpg.circos.assembly.ClasspathAssemblyRepository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackLayoutTest {
    @Test
    void conditionalTracksMoveLinkAnchorInward() {
        var application = new CircosApplication();
        var assemblies = new ClasspathAssemblyRepository();
        var engine = new CircularLayoutEngine();
        var empty = application.validate(TestFixtures.open("/examples/empty-categories.json"));
        var cnv = application.validate(TestFixtures.open("/examples/gains-and-losses.json"));
        var emptyGeometry = engine.layout(empty, assemblies.load(empty.assemblyId()));
        var cnvGeometry = engine.layout(cnv, assemblies.load(cnv.assemblyId()));

        assertNull(emptyGeometry.tracks().gainTrack());
        assertNull(emptyGeometry.tracks().lossTrack());
        assertNotNull(cnvGeometry.tracks().gainTrack());
        assertNotNull(cnvGeometry.tracks().lossTrack());
        assertTrue(cnvGeometry.tracks().linkRadius() < emptyGeometry.tracks().linkRadius());
    }
}

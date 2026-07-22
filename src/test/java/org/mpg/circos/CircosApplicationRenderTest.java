package org.mpg.circos;

import org.junit.jupiter.api.Test;
import org.mpg.circos.model.CircosPlot;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CircosApplicationRenderTest {
    @Test
    void rendersT2tChm13v2PlotFromTypedApi() {
        var application = new CircosApplication();
        CircosPlot original = application.validate(TestFixtures.open("/examples/v2-interval-links.json"));
        CircosPlot t2t = new CircosPlot(original.schemaVersion(), original.plotId(), original.label(), original.mode(),
                "T2T-CHM13", original.coordinateConvention(), original.sourceResultIds(), original.segments(),
                original.links());

        var document = application.render(t2t);

        assertTrue(document.xml().contains("data-assembly-id=\"T2T-CHM13\""));
    }

    @Test
    void rendersToDocumentAndCallerOwnedStream() throws Exception {
        var application = new CircosApplication();
        var document = application.render(TestFixtures.open("/examples/patient-bcr-abl1.json"));
        assertTrue(document.xml().startsWith("<?xml"));
        assertTrue(document.xml().contains("class=\"circos-link-ribbon\""));

        var output = new ByteArrayOutputStream();
        application.render(TestFixtures.open("/examples/gains-and-losses.json"), output,
                RenderOptions.defaults());
        output.write(' ');
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("</svg>\n "));
    }
}

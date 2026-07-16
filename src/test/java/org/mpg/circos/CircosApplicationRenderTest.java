package org.mpg.circos;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CircosApplicationRenderTest {
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

package org.mpg.circos.renderer;

import org.junit.jupiter.api.Test;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.model.CircosPlot;
import org.mpg.circos.model.CoordinateConvention;
import org.mpg.circos.model.EventType;
import org.mpg.circos.model.GenomicInterval;
import org.mpg.circos.model.GenomicSegment;
import org.mpg.circos.model.PlotMode;
import org.mpg.circos.model.SchemaVersion;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SvgSecurityTest {
    @Test
    void escapesAllCallerTextWithoutExecutableMarkup() throws Exception {
        String hostile = "</metadata><script>alert(\"x\")</script>&";
        var segment = new GenomicSegment("segment<&\"", "result<&\"", null,
                new GenomicInterval("1", 10, 20), EventType.GAIN, 3, "HIGH<&\"", hostile);
        var plot = new CircosPlot(SchemaVersion.V1_0, "plot<&\"", hostile, PlotMode.PATIENT,
                "GRCh38", CoordinateConvention.ZERO_BASED_HALF_OPEN,
                List.of("result<&\""), List.of(segment), List.of());

        String xml = new CircosApplication().render(plot).xml();
        assertFalse(xml.contains("<script>"));
        assertTrue(xml.contains("&lt;script&gt;"));
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertEquals(0, document.getElementsByTagName("script").getLength());
    }
}

package org.mpg.circos.renderer;

import org.junit.jupiter.api.Test;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.TestFixtures;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticSvgRendererTest {
    @Test
    void emitsStableSemanticPatientStructureAndEndpointMarkers() throws Exception {
        Document document = parse(new CircosApplication()
                .render(TestFixtures.open("/examples/patient-bcr-abl1.json")).xml());
        assertEquals("1.0", document.getDocumentElement().getAttribute("data-contract-version"));
        assertEquals("patient", document.getDocumentElement().getAttribute("data-plot-mode"));
        assertEquals(1, elementsWithClass(document, "circos-canvas"));
        assertEquals(24, document.getElementsByTagName("text").getLength() - 3);
        assertEquals(72, elementsWithClass(document, "track-midline"));
        assertEquals(1, elementsWithClass(document, "circos-segment"));
        assertEquals(1, elementsWithClass(document, "circos-link-ribbon"));
        assertEquals(2, elementsWithClass(document, "circos-link-endpoint"));
        var segment = firstWithClass(document, "circos-segment");
        assertEquals("duplication", segment.getAttribute("data-display-type"));
        assertEquals("[\"BCL2\"]", segment.getAttribute("data-genes"));
        var link = firstWithClass(document, "circos-link");
        assertEquals("[\"ABL1\"]", link.getAttribute("data-source-genes"));
        assertEquals("[\"BCR\"]", link.getAttribute("data-target-genes"));
        assertFalse(document.getDocumentElement().hasAttribute("onclick"));
    }

    @Test
    void emitsCohortCountsAndOmitsPatientLinkResultIdentity() throws Exception {
        Document document = parse(new CircosApplication()
                .render(TestFixtures.open("/examples/cohort-aggregate.json")).xml());
        var links = document.getElementsByTagName("g");
        for (int i = 0; i < links.getLength(); i++) {
            var element = (org.w3c.dom.Element) links.item(i);
            if (!java.util.List.of(element.getAttribute("class").split(" ")).contains("circos-link")) continue;
            assertEquals("6", element.getAttribute("data-aggregate-event-count"));
            assertEquals("3", element.getAttribute("data-aggregate-patient-count"));
            assertEquals("4", element.getAttribute("data-aggregate-sample-count"));
            assertEquals("aggregate-9-22", element.getAttribute("data-aggregate-id"));
            assertEquals("Exact breakpoints", element.getAttribute("data-grouping-description"));
            assertEquals("[{\"label\":\"High\",\"count\":4},{\"label\":\"Medium\",\"count\":2}]",
                    element.getAttribute("data-confidence-distribution"));
            assertFalse(element.hasAttribute("data-source-result-id"));
        }
    }

    @Test
    void positionsLegendInLowerLeftCornerOutsidePlotRings() throws Exception {
        Document document = parse(new CircosApplication()
                .render(TestFixtures.open("/examples/gains-and-losses.json")).xml());
        var groups = document.getElementsByTagName("g");
        for (int i = 0; i < groups.getLength(); i++) {
            var group = (org.w3c.dom.Element) groups.item(i);
            if (!java.util.List.of(group.getAttribute("class").split(" ")).contains("circos-legend")) continue;
            var swatches = group.getElementsByTagName("rect");
            assertEquals(3, swatches.getLength());
            assertEquals("16", ((org.w3c.dom.Element) swatches.item(0)).getAttribute("x"));
            assertTrue(Integer.parseInt(((org.w3c.dom.Element) swatches.item(0)).getAttribute("y")) >= 630);
            return;
        }
        throw new AssertionError("Missing Circos legend");
    }

    private int elementsWithClass(Document document, String className) {
        int count = 0;
        var all = document.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            var element = (org.w3c.dom.Element) all.item(i);
            if (java.util.List.of(element.getAttribute("class").split(" ")).contains(className)) count++;
        }
        return count;
    }

    private org.w3c.dom.Element firstWithClass(Document document, String className) {
        var all = document.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            var element = (org.w3c.dom.Element) all.item(i);
            if (java.util.List.of(element.getAttribute("class").split(" ")).contains(className)) return element;
        }
        throw new AssertionError("Missing element with class " + className);
    }

    private Document parse(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}

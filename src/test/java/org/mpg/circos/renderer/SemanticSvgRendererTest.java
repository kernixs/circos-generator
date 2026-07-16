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
            assertFalse(element.hasAttribute("data-source-result-id"));
        }
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

    private Document parse(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}

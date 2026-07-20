package org.mpg.circos.viewer;

import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.TestFixtures;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircosViewerDomTest {
    @Test
    void hoverShowsTextOnlyTooltipsWithoutChangingSelection() throws Exception {
        try (WebClient browser = browser()) {
            HtmlPage segments = page(browser, "/examples/gains-and-losses.json");
            hover(segments, ".event-gain");
            assertTrue(tooltip(segments).contains(
                    "Event type: Amplification\nGenomic range: chr18:37,912,423–39,587,423"));
            assertTrue(tooltip(segments).contains("Genome build: GRCh37"));
            assertEquals("false", script(segments,
                    "String(document.querySelector('.circos-tooltip').hidden)"));
            assertEquals("0", script(segments, "String(window.selectionDetails.length)"));
            hover(segments, ".event-loss");
            assertTrue(tooltip(segments).contains(
                    "Event type: Deletion\nGenomic range: chr7:16,014,079–18,239,079"));

            HtmlPage links = page(browser, "/examples/crossing-links.json");
            hover(links, ".circos-link");
            assertTrue(tooltip(links).contains(
                    "Event type: Translocation\nBreakpoints: chr9:133,600,000 ↔ chr22:23,600,000"));
            assertTrue(tooltip(links).contains("Genes: ABL1 ↔ BCR\nMethod: Karyotype\nConfidence: HIGH"));
            assertEquals("0", script(links, "String(window.selectionDetails.length)"));
        }
    }

    @Test
    void patientSegmentTooltipsFormatMetadataLengthsAndMissingValues() throws Exception {
        try (WebClient browser = browser()) {
            HtmlPage page = page(browser, "/examples/tooltip-metadata.json");

            hover(page, "[data-segment-id='bp-gain']");
            String gain = tooltip(page);
            assertTrue(gain.contains("Event type: Duplication\nGenomic range: chr1:101–1,099"
                    + "\nInterval length: 999 bp"));
            assertTrue(gain.contains("Copy number: 3"));
            assertTrue(gain.contains("Genes: EGFR <safe> & MET"));
            assertTrue(gain.contains("Method: FISH"));
            assertFalse(gain.contains("Confidence:"));
            assertEquals("0", script(page, "String(document.querySelectorAll('.circos-tooltip script').length)"));

            hover(page, "[data-segment-id='kb-loss']");
            String loss = tooltip(page);
            assertTrue(loss.contains("Event type: Loss\nGenomic range: chr2:2,001–14,500"
                    + "\nInterval length: 12.5 kb"));
            assertFalse(loss.contains("Copy number:"));
            assertFalse(loss.contains("Genes:"));
            assertFalse(loss.contains("Method:"));
            assertFalse(loss.contains("Confidence:"));

            hover(page, "[data-segment-id='mb-loss']");
            String longGenes = tooltip(page);
            assertTrue(longGenes.contains("5.36 Mb"));
            assertTrue(longGenes.contains("Genes: 11 genes"));
            assertTrue(longGenes.contains("Methods: Microarray, WGS"));
            assertTrue(longGenes.contains("Confidence: High"));
        }
    }

    @Test
    void patientLinkTooltipCanonicalizesReversedEndpointsAndGenes() throws Exception {
        try (WebClient browser = browser()) {
            HtmlPage page = page(browser, "/examples/tooltip-metadata.json");
            hover(page, "[data-link-id='reversed-link']");

            String tooltip = tooltip(page);
            assertTrue(tooltip.contains("chr9:133,600,000 ↔ chr22:23,600,000"));
            assertTrue(tooltip.contains("Genes: ABL1 ↔ BCR"));
            assertTrue(tooltip.contains("Method: Karyotype"));
            assertTrue(tooltip.contains("Confidence: High"));
        }
    }

    @Test
    void cohortSegmentAndLinkTooltipsUseSuppliedCountsWithoutAveraging() throws Exception {
        try (WebClient browser = browser()) {
            HtmlPage segments = page(browser, "/examples/cohort-single-result.json");
            hover(segments, "[data-segment-id='cohort-gain-1']");
            String gain = tooltip(segments);
            assertTrue(gain.contains("Events: 1\nSamples: 1\nPatients: 1"));
            assertTrue(gain.contains("Methods: Microarray, WGS"));
            assertTrue(gain.contains("Grouped by: Exact interval"));
            assertTrue(gain.contains("Confidence: High 1"));

            hover(segments, "[data-segment-id='cohort-loss-1']");
            String loss = tooltip(segments);
            assertTrue(loss.contains("Events: 2\nSamples: 2\nPatients: 2"));
            assertTrue(loss.contains("Method: WGS"));
            assertTrue(loss.contains("Confidence: High 1, Low 1"));

            HtmlPage links = page(browser, "/examples/cohort-aggregate.json");
            hover(links, "[data-link-id='aggregate-9-22']");
            String link = tooltip(links);
            assertTrue(link.contains("chr9:66,858,501 ↔ chr22:11,609,501"));
            assertTrue(link.contains("Events: 6\nSamples: 4\nPatients: 3"));
            assertTrue(link.contains("Grouped by: Exact breakpoints"));
            assertFalse(link.toLowerCase().contains("average"));
        }
    }

    @Test
    void clickSelectsOneEventDimsOthersAndTogglesOff() throws Exception {
        try (WebClient browser = browser()) {
            HtmlPage page = page(browser, "/examples/crossing-links.json");

            click(page, ".circos-link");

            assertEquals("1", script(page, "String(document.querySelectorAll('.is-selected').length)"));
            assertEquals("2", script(page, "String(document.querySelectorAll('.circos-event.is-dimmed').length)"));
            assertEquals("true", script(page,
                    "String(document.querySelector('.circos-link').classList.contains('is-selected'))"));
            assertEquals("link-a", script(page, "window.selectionDetails[0].linkIds[0]"));
            assertEquals("2", script(page,
                    "String(document.querySelector('.circos-link.is-selected').querySelectorAll('.circos-link-endpoint').length)"));

            click(page, ".circos-link");

            assertEquals("0", script(page, "String(document.querySelectorAll('.is-selected').length)"));
            assertEquals("0", script(page, "String(document.querySelectorAll('.is-dimmed').length)"));
            assertEquals("0", script(page, "String(window.selectionDetails[1].linkIds.length)"));
        }
    }

    @Test
    void backgroundClickClearsSelection() throws Exception {
        try (WebClient browser = browser()) {
            HtmlPage page = page(browser, "/examples/gains-and-losses.json");
            click(page, ".event-gain");
            click(page, ".circos-canvas");

            assertEquals("0", script(page, "String(document.querySelectorAll('.is-selected').length)"));
            assertEquals("0", script(page, "String(window.selectionDetails[1].segmentIds.length)"));
        }
    }

    @Test
    void enterAndSpaceProvideClickEquivalentSelection() throws Exception {
        try (WebClient browser = browser()) {
            HtmlPage page = page(browser, "/examples/crossing-links.json");

            key(page, ".circos-link", "Enter");
            assertEquals("link-a", script(page, "window.selectionDetails[0].linkIds[0]"));
            assertEquals("true", script(page,
                    "document.querySelector('.circos-link').getAttribute('aria-pressed')"));

            key(page, ".circos-link", " ");
            assertEquals("0", script(page, "String(document.querySelectorAll('.is-selected').length)"));
            assertEquals("0", script(page, "String(window.selectionDetails[1].linkIds.length)"));
        }
    }

    @Test
    void cohortSelectionEmitsAggregateIdWithoutContributorRecords() throws Exception {
        try (WebClient browser = browser()) {
            HtmlPage page = page(browser, "/examples/cohort-aggregate.json");
            click(page, ".circos-link");

            assertEquals("aggregate-9-22", script(page, "window.selectionDetails[0].linkIds[0]"));
            assertEquals("aggregate-9-22", script(page, "window.selectionDetails[0].aggregateIds[0]"));
            assertEquals("0", script(page,
                    "String(window.selectionDetails[0].selectedSourceResultIds.length)"));
            assertFalse(script(page, "JSON.stringify(window.selectionDetails[0])").contains("contributor"));

            HtmlPage segments = page(browser, "/examples/cohort-single-result.json");
            click(segments, "[data-segment-id='cohort-gain-1']");
            assertEquals("cohort-gain-1", script(segments, "window.selectionDetails[0].segmentIds[0]"));
            assertEquals("cohort-gain-1", script(segments, "window.selectionDetails[0].aggregateIds[0]"));
            assertEquals("0", script(segments,
                    "String(window.selectionDetails[0].selectedSourceResultIds.length)"));
        }
    }

    @Test
    void emptyPlotAttachesWithoutInteractiveEvents() throws Exception {
        try (WebClient browser = browser()) {
            HtmlPage page = page(browser, "/examples/empty-categories.json");

            assertEquals("0", script(page, "String(document.querySelectorAll('.circos-event').length)"));
            assertEquals("null", script(page, "String(window.viewerController.selectedId())"));
            click(page, ".circos-canvas");
            assertEquals("0", script(page, "String(window.selectionDetails.length)"));
            assertEquals("true", script(page,
                    "String(document.querySelector('.circos-tooltip').hidden)"));
        }
    }

    private WebClient browser() {
        WebClient browser = new WebClient(BrowserVersion.CHROME);
        browser.getOptions().setCssEnabled(false);
        browser.getOptions().setThrowExceptionOnScriptError(true);
        return browser;
    }

    private HtmlPage page(WebClient browser, String fixture) throws Exception {
        String svg = new CircosApplication().render(TestFixtures.open(fixture)).xml()
                .replaceFirst("<\\?xml version=\"1\\.0\" encoding=\"UTF-8\"\\?>\\R", "");
        String viewer = Files.readString(Path.of("viewer/circos-viewer.js"));
        String html = "<!doctype html><html><body><div id=\"host\">" + svg + "</div><script>"
                + viewer + "</script><script>window.selectionDetails=[];"
                + "var host=document.getElementById('host');"
                + "window.viewerController=CircosViewer.attach(host);"
                + "host.addEventListener('circos-selection-change',function(event){"
                + "window.selectionDetails.push(event.detail);});</script></body></html>";
        return browser.loadHtmlCodeIntoCurrentWindow(html);
    }

    private void hover(HtmlPage page, String selector) {
        script(page, "document.querySelector(" + javascriptString(selector)
                + ").dispatchEvent(new MouseEvent('mouseover',"
                + "{bubbles:true,clientX:20,clientY:30}))");
    }

    private String tooltip(HtmlPage page) {
        return script(page, "document.querySelector('.circos-tooltip').textContent");
    }

    private void click(HtmlPage page, String selector) {
        script(page, "document.querySelector(" + javascriptString(selector)
                + ").dispatchEvent(new MouseEvent('click',"
                + "{bubbles:true}))");
    }

    private void key(HtmlPage page, String selector, String key) {
        script(page, "document.querySelector(" + javascriptString(selector)
                + ").dispatchEvent(new KeyboardEvent('keydown',"
                + "{bubbles:true,key:" + javascriptString(key) + "}))");
    }

    private String script(HtmlPage page, String javascript) {
        Object result = page.executeJavaScript(javascript).getJavaScriptResult();
        return result == null ? "null" : result.toString();
    }

    private String javascriptString(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }
}

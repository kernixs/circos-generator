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
            assertTrue(script(segments, "document.querySelector('.circos-tooltip').textContent")
                    .contains("Gain · chr18:37,912,423–39,587,423"));
            assertFalse(script(segments, "document.querySelector('.circos-tooltip').textContent")
                    .contains("GRCh"));
            assertEquals("false", script(segments,
                    "String(document.querySelector('.circos-tooltip').hidden)"));
            assertEquals("0", script(segments, "String(window.selectionDetails.length)"));
            hover(segments, ".event-loss");
            assertTrue(script(segments, "document.querySelector('.circos-tooltip').textContent")
                    .contains("Loss · chr7:16,014,079–18,239,079"));

            HtmlPage links = page(browser, "/examples/crossing-links.json");
            hover(links, ".circos-link");
            assertTrue(script(links, "document.querySelector('.circos-tooltip').textContent")
                    .contains("Translocation · chr1:1,000,001 ↔ chr22:5,000,001"));
            assertEquals("0", script(links, "String(window.selectionDetails.length)"));
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
            assertEquals("0", script(page,
                    "String(window.selectionDetails[0].selectedSourceResultIds.length)"));
            assertFalse(script(page, "JSON.stringify(window.selectionDetails[0])").contains("contributor"));
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
        script(page, "document.querySelector('" + selector + "').dispatchEvent(new MouseEvent('mouseover',"
                + "{bubbles:true,clientX:20,clientY:30}))");
    }

    private void click(HtmlPage page, String selector) {
        script(page, "document.querySelector('" + selector + "').dispatchEvent(new MouseEvent('click',"
                + "{bubbles:true}))");
    }

    private void key(HtmlPage page, String selector, String key) {
        script(page, "document.querySelector('" + selector + "').dispatchEvent(new KeyboardEvent('keydown',"
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

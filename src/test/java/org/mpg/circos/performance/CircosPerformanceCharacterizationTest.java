package org.mpg.circos.performance;

import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.model.CircosPlot;
import org.mpg.circos.model.CoordinateConvention;
import org.mpg.circos.model.EventType;
import org.mpg.circos.model.GenomicInterval;
import org.mpg.circos.model.GenomicSegment;
import org.mpg.circos.model.PlotMode;
import org.mpg.circos.model.SchemaVersion;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("performance")
class CircosPerformanceCharacterizationTest {
    @Test
    void recordsLargePatientAndCohortGenerationAndViewerMeasurements() throws Exception {
        characterize("patient-500", plot(PlotMode.PATIENT, 500));
        characterize("cohort-10000", plot(PlotMode.COHORT, 10_000));
    }

    private void characterize(String name, CircosPlot plot) throws Exception {
        long generationStart = System.nanoTime();
        String svg = new CircosApplication().render(plot).xml();
        long generationNanos = System.nanoTime() - generationStart;
        int svgBytes = svg.getBytes(StandardCharsets.UTF_8).length;
        assertEquals(plot.segments().size(), occurrences(svg, "class=\"circos-event circos-segment"));

        BrowserMetrics browser = browserMetrics(svg, plot.segments().getFirst().id());
        System.out.printf(Locale.ROOT,
                "PERF %-13s events=%d generationMs=%.3f svgBytes=%d browserLoadMs=%.3f hoverMs=%.3f selectionMs=%.3f%n",
                name, plot.segments().size(), millis(generationNanos), svgBytes,
                millis(browser.loadNanos()), millis(browser.hoverNanos()), millis(browser.selectionNanos()));
    }

    private BrowserMetrics browserMetrics(String svg, String expectedId) throws Exception {
        String viewer = Files.readString(Path.of("viewer/circos-viewer.js"));
        String inlineSvg = svg.replaceFirst("<\\?xml version=\"1\\.0\" encoding=\"UTF-8\"\\?>\\R", "");
        String html = "<!doctype html><html><body><div id=\"host\">" + inlineSvg + "</div><script>"
                + viewer + "</script><script>window.selectionDetails=[];"
                + "var host=document.getElementById('host');CircosViewer.attach(host);"
                + "host.addEventListener('circos-selection-change',function(event){"
                + "window.selectionDetails.push(event.detail);});</script></body></html>";

        try (WebClient browser = new WebClient(BrowserVersion.CHROME)) {
            browser.getOptions().setCssEnabled(false);
            browser.getOptions().setThrowExceptionOnScriptError(true);
            long loadStart = System.nanoTime();
            HtmlPage page = browser.loadHtmlCodeIntoCurrentWindow(html);
            long loadNanos = System.nanoTime() - loadStart;

            long hoverStart = System.nanoTime();
            script(page, "document.querySelector('.circos-event').dispatchEvent("
                    + "new MouseEvent('mouseover',{bubbles:true,clientX:20,clientY:30}))");
            long hoverNanos = System.nanoTime() - hoverStart;
            assertEquals("false", script(page, "String(document.querySelector('.circos-tooltip').hidden)"));
            assertTrue(script(page, "document.querySelector('.circos-tooltip').textContent").contains("chr1:"));

            long selectionStart = System.nanoTime();
            script(page, "document.querySelector('.circos-event').dispatchEvent("
                    + "new MouseEvent('click',{bubbles:true}))");
            long selectionNanos = System.nanoTime() - selectionStart;
            assertEquals("1", script(page, "String(document.querySelectorAll('.is-selected').length)"));
            assertEquals(expectedId, script(page, "window.selectionDetails[0].segmentIds[0]"));
            return new BrowserMetrics(loadNanos, hoverNanos, selectionNanos);
        }
    }

    private CircosPlot plot(PlotMode mode, int eventCount) {
        List<GenomicSegment> segments = IntStream.range(0, eventCount)
                .mapToObj(this::segment)
                .toList();
        return new CircosPlot(SchemaVersion.V1_0, "synthetic-performance-" + mode.value(), null,
                mode, "GRCh37", CoordinateConvention.ZERO_BASED_HALF_OPEN,
                List.of("synthetic-result-1"), segments, List.of());
    }

    private GenomicSegment segment(int index) {
        long start = (index * 20_000L) % 249_000_000L;
        EventType type = index % 2 == 0 ? EventType.GAIN : EventType.LOSS;
        Integer copyNumber = type == EventType.GAIN ? 3 + index % 3 : null;
        return new GenomicSegment("synthetic-segment-" + index, "synthetic-result-1", null,
                new GenomicInterval("1", start, start + 5_000), type, copyNumber, null, null);
    }

    private int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private String script(HtmlPage page, String javascript) {
        Object result = page.executeJavaScript(javascript).getJavaScriptResult();
        return result == null ? "null" : result.toString();
    }

    private double millis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private record BrowserMetrics(long loadNanos, long hoverNanos, long selectionNanos) {}
}

package org.mpg.circos.renderer;

import org.mpg.circos.CircosGenerationException;
import org.mpg.circos.layout.AnnularPath;
import org.mpg.circos.layout.PlotGeometry;
import org.mpg.circos.layout.PolarPoint;
import org.mpg.circos.model.CircosPlot;
import org.mpg.circos.model.EventType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SemanticSvgRenderer {
    private final RenderTheme theme;
    private final SvgElementFactory elements;
    private final SvgIdEncoder ids;

    public SemanticSvgRenderer() {
        this(new CompatibilityTheme(), new SvgElementFactory(), new SvgIdEncoder());
    }

    public SemanticSvgRenderer(RenderTheme theme, SvgElementFactory elements, SvgIdEncoder ids) {
        this.theme = theme;
        this.elements = elements;
        this.ids = ids;
    }

    public SvgDocument render(CircosPlot plot, PlotGeometry geometry) {
        validateSafeIdCollisions(plot);
        String plotToken = ids.encode(plot.plotId());
        Map<String, PlotGeometry.SegmentGeometry> segmentGeometry = new HashMap<>();
        geometry.segments().forEach(value -> segmentGeometry.put(value.id(), value));
        Map<String, PlotGeometry.LinkGeometry> linkGeometry = new HashMap<>();
        geometry.links().forEach(value -> linkGeometry.put(value.id(), value));

        StringBuilder svg = new StringBuilder(32_768);
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" id=\"circos-plot-")
                .append(plotToken).append("\" class=\"circos-plot\" data-contract-version=\"1.0\"")
                .append(attr("data-plot-id", plot.plotId()))
                .append(attr("data-plot-mode", plot.mode().value()))
                .append(attr("data-assembly-id", plot.assemblyId()))
                .append(" viewBox=\"0 0 ").append(elements.format(geometry.viewBoxSize())).append(' ')
                .append(elements.format(geometry.viewBoxSize())).append("\" preserveAspectRatio=\"xMidYMid meet\">\n");
        svg.append("  <metadata id=\"circos-metadata-").append(plotToken).append("\">")
                .append(XmlEscaper.text(metadataJson(plot))).append("</metadata>\n");
        appendDefinitions(svg, plotToken);
        svg.append("  <g id=\"circos-scene-").append(plotToken).append("\" class=\"circos-scene\">\n");
        appendBackground(svg, plotToken, geometry);
        appendChromosomes(svg, plotToken, geometry);
        appendTracks(svg, plotToken, plot, geometry, segmentGeometry);
        appendLinks(svg, plotToken, plot, geometry, linkGeometry);
        appendLabels(svg, plotToken, geometry);
        appendLegend(svg, plotToken);
        svg.append("  </g>\n</svg>\n");
        return new SvgDocument(svg.toString());
    }

    private void appendDefinitions(StringBuilder svg, String plotToken) {
        svg.append("  <defs id=\"circos-defs-").append(plotToken).append("\">\n")
                .append("    <style>")
                .append(".circos-plot{background:#fff;font-family:Arial,sans-serif}.chromosome-sector{stroke:#fff;stroke-width:2}")
                .append(".track-background{fill:").append(theme.trackBackgroundColor()).append(";stroke:#fff;stroke-width:1}")
                .append(".circos-event{cursor:pointer;outline:none}.circos-event:focus{stroke:#111;stroke-width:2}")
                .append(".circos-link-endpoint{opacity:.15}.is-selected{stroke:#111;stroke-width:3}.is-related{stroke:#555;stroke-width:2}")
                .append(".is-dimmed{opacity:.18}.chromosome-label{fill:#111;font-size:14px;text-anchor:middle;dominant-baseline:middle}")
                .append(".legend-label{fill:#111;font-size:11px;dominant-baseline:middle}")
                .append("</style>\n  </defs>\n");
    }

    private void appendBackground(StringBuilder svg, String plotToken, PlotGeometry geometry) {
        svg.append("    <g id=\"circos-background-").append(plotToken).append("\" class=\"circos-background\">\n");
        svg.append("      <rect class=\"circos-canvas\" x=\"0\" y=\"0\" width=\"")
                .append(elements.format(geometry.viewBoxSize())).append("\" height=\"")
                .append(elements.format(geometry.viewBoxSize())).append("\" fill=\"#ffffff\"/>\n");
        svg.append("    </g>\n");
    }

    private void appendChromosomes(StringBuilder svg, String plotToken, PlotGeometry geometry) {
        svg.append("    <g id=\"circos-chromosomes-").append(plotToken).append("\" class=\"circos-chromosomes\">\n");
        for (int i = 0; i < geometry.sectors().size(); i++) {
            var sector = geometry.sectors().get(i);
            AnnularPath path = new AnnularPath(geometry.tracks().chromosomeInnerRadius(),
                    geometry.tracks().chromosomeOuterRadius(), sector.startAngle(), sector.endAngle());
            svg.append("      <path id=\"chromosome-").append(plotToken).append('-')
                    .append(XmlEscaper.attribute(sector.chromosome())).append("\" class=\"chromosome-sector\"")
                    .append(attr("data-chromosome", sector.chromosome()))
                    .append(" fill=\"").append(theme.chromosomeColors().get(i)).append("\" d=\"")
                    .append(elements.annularPath(geometry.centerX(), geometry.centerY(), path)).append("\"/>\n");
        }
        svg.append("    </g>\n");
    }

    private void appendTracks(StringBuilder svg, String plotToken, CircosPlot plot, PlotGeometry geometry,
                              Map<String, PlotGeometry.SegmentGeometry> shapes) {
        svg.append("    <g id=\"circos-tracks-").append(plotToken).append("\" class=\"circos-tracks\">\n")
                .append("      <g id=\"track-backgrounds-").append(plotToken)
                .append("\" class=\"track-backgrounds\">\n");
        appendBaseTrackBackgrounds(svg, geometry);
        svg.append("      </g>\n");
        appendSegmentTrackBackground(svg, "gain", plotToken, geometry, geometry.tracks().gainTrack());
        appendSegmentTrackBackground(svg, "loss", plotToken, geometry, geometry.tracks().lossTrack());
        svg.append("      <g id=\"track-gains-").append(plotToken).append("\" class=\"track track-gains\">\n");
        appendSegments(svg, plotToken, plot, geometry, shapes, EventType.GAIN);
        svg.append("      </g>\n      <g id=\"track-losses-").append(plotToken)
                .append("\" class=\"track track-losses\">\n");
        appendSegments(svg, plotToken, plot, geometry, shapes, EventType.LOSS);
        svg.append("      </g>\n    </g>\n");
    }

    private void appendBaseTrackBackgrounds(StringBuilder svg, PlotGeometry geometry) {
        int trackIndex = 1;
        for (var ring : geometry.tracks().backgroundTracks()) {
            for (var sector : geometry.sectors()) {
                AnnularPath path = new AnnularPath(ring.innerRadius(), ring.outerRadius(),
                        sector.startAngle(), sector.endAngle());
                svg.append("        <path class=\"track-background\" data-background-track=\"")
                        .append(trackIndex).append("\"")
                        .append(attr("data-chromosome", sector.chromosome())).append(" d=\"")
                        .append(elements.annularPath(geometry.centerX(), geometry.centerY(), path)).append("\"/>\n");
                double middleRadius = ring.innerRadius() + (ring.outerRadius() - ring.innerRadius()) / 2.0;
                svg.append("        <path class=\"track-midline\" fill=\"none\" stroke=\"#ffffff\"")
                        .append(" stroke-width=\"1.8\" d=\"")
                        .append(elements.arcPath(geometry.centerX(), geometry.centerY(), middleRadius,
                                sector.startAngle(), sector.endAngle()))
                        .append("\"/>\n");
            }
            trackIndex++;
        }
    }

    private void appendSegmentTrackBackground(StringBuilder svg, String type, String plotToken,
            PlotGeometry geometry, org.mpg.circos.layout.TrackLayout.Ring ring) {
        if (ring == null) return;
        svg.append("      <g id=\"track-").append(type).append("-background-").append(plotToken)
                .append("\" class=\"track-backgrounds\">\n");
        for (var sector : geometry.sectors()) {
            var path = new AnnularPath(ring.innerRadius(), ring.outerRadius(), sector.startAngle(), sector.endAngle());
            svg.append("        <path class=\"track-background\" d=\"")
                    .append(elements.annularPath(geometry.centerX(), geometry.centerY(), path)).append("\"/>\n");
        }
        svg.append("      </g>\n");
    }

    private void appendSegments(StringBuilder svg, String plotToken, CircosPlot plot, PlotGeometry geometry,
                                Map<String, PlotGeometry.SegmentGeometry> shapes, EventType type) {
        for (var segment : plot.segments()) {
            if (segment.eventType() != type) continue;
            PlotGeometry.SegmentGeometry shape = required(shapes, segment.id(), "segment");
            String token = ids.encode(segment.id());
            svg.append("        <g id=\"segment-").append(plotToken).append('-').append(token)
                    .append("\" class=\"circos-event circos-segment event-").append(type.value())
                    .append("\" tabindex=\"0\" role=\"button\"")
                    .append(attr("data-segment-id", segment.id()))
                    .append(optionalAttr("data-event-group-id", segment.eventGroupId()))
                    .append(attr("data-source-result-id", segment.sourceResultId()))
                    .append(attr("data-event-type", type.value()))
                    .append(optionalAttr("data-confidence", segment.confidence()))
                    .append(attr("data-chromosome", segment.interval().chromosome()))
                    .append(attr("data-start", Long.toString(segment.interval().start())))
                    .append(attr("data-end", Long.toString(segment.interval().end())))
                    .append(segment.copyNumber() == null ? "" : attr("data-copy-number", segment.copyNumber().toString()))
                    .append(optionalAttr("data-label", segment.label())).append(">\n")
                    .append("          <path class=\"circos-segment-interval\" fill=\"")
                    .append(type == EventType.GAIN ? theme.gainColor() : theme.lossColor())
                    .append("\" fill-opacity=\"").append(elements.format(theme.eventOpacity())).append("\" d=\"")
                    .append(elements.annularPath(geometry.centerX(), geometry.centerY(), shape.interval()))
                    .append("\"/>\n          <circle class=\"circos-segment-marker\" fill=\"")
                    .append(type == EventType.GAIN ? theme.gainColor() : theme.lossColor())
                    .append("\" fill-opacity=\"").append(elements.format(theme.eventOpacity()))
                    .append("\" cx=\"").append(elements.format(shape.marker().x())).append("\" cy=\"")
                    .append(elements.format(shape.marker().y())).append("\" r=\"3\"/>\n        </g>\n");
        }
    }

    private void appendLinks(StringBuilder svg, String plotToken, CircosPlot plot, PlotGeometry geometry,
                             Map<String, PlotGeometry.LinkGeometry> shapes) {
        svg.append("    <g id=\"circos-links-").append(plotToken).append("\" class=\"circos-links\">\n");
        for (var link : plot.links()) {
            PlotGeometry.LinkGeometry shape = required(shapes, link.id(), "link");
            String token = ids.encode(link.id());
            svg.append("      <g id=\"link-").append(plotToken).append('-').append(token)
                    .append("\" class=\"circos-event circos-link event-translocation\" tabindex=\"0\" role=\"button\"")
                    .append(attr("data-link-id", link.id()))
                    .append(optionalAttr("data-event-group-id", link.eventGroupId()))
                    .append(attr("data-source-segment-id", link.source().segmentId()))
                    .append(attr("data-target-segment-id", link.target().segmentId()))
                    .append(optionalAttr("data-source-result-id", link.sourceResultId()))
                    .append(attr("data-event-type", link.eventType().value()))
                    .append(optionalAttr("data-confidence", link.confidence()))
                    .append(attr("data-source-chromosome", link.source().chromosome()))
                    .append(attr("data-source-position", Long.toString(link.source().position())))
                    .append(attr("data-target-chromosome", link.target().chromosome()))
                    .append(attr("data-target-position", Long.toString(link.target().position())))
                    .append(optionalAttr("data-label", link.label()));
            if (link.aggregate() != null) {
                svg.append(attr("data-aggregate-event-count", Integer.toString(link.aggregate().eventCount())))
                        .append(attr("data-aggregate-patient-count", Integer.toString(link.aggregate().patientCount())))
                        .append(attr("data-aggregate-sample-count", Integer.toString(link.aggregate().sampleCount())));
            }
            svg.append(">\n        <path class=\"circos-link-ribbon\" fill=\"").append(theme.linkColor())
                    .append("\" stroke=\"").append(theme.linkColor()).append("\" fill-opacity=\"")
                    .append(elements.format(theme.linkFillOpacity(shape.eventCount())))
                    .append("\" stroke-opacity=\"").append(elements.format(theme.linkBorderOpacity(shape.eventCount())))
                    .append("\" d=\"").append(elements.ribbonPath(geometry.centerX(), geometry.centerY(), shape.ribbon()))
                    .append("\"/>\n");
            appendEndpoint(svg, "endpoint-source", shape.ribbon().sourceEndpoint());
            appendEndpoint(svg, "endpoint-target", shape.ribbon().targetEndpoint());
            svg.append("      </g>\n");
        }
        svg.append("    </g>\n");
    }

    private void appendEndpoint(StringBuilder svg, String type, PolarPoint point) {
        svg.append("        <circle class=\"circos-link-endpoint ").append(type)
                .append("\" aria-hidden=\"true\" fill=\"").append(theme.linkColor())
                .append("\" cx=\"").append(elements.format(point.x())).append("\" cy=\"")
                .append(elements.format(point.y())).append("\" r=\"3\"/>\n");
    }

    private void appendLabels(StringBuilder svg, String plotToken, PlotGeometry geometry) {
        svg.append("    <g id=\"circos-labels-").append(plotToken).append("\" class=\"circos-labels\">\n");
        for (var sector : geometry.sectors()) {
            double angle = sector.startAngle() + (sector.endAngle() - sector.startAngle()) / 2.0;
            PolarPoint point = PolarPoint.from(geometry.centerX(), geometry.centerY(),
                    geometry.tracks().chromosomeLabelRadius(), angle);
            svg.append("      <text class=\"chromosome-label\"")
                    .append(attr("data-chromosome", sector.chromosome()))
                    .append(" x=\"").append(elements.format(point.x())).append("\" y=\"")
                    .append(elements.format(point.y())).append("\">")
                    .append(XmlEscaper.text(sector.chromosome())).append("</text>\n");
        }
        svg.append("    </g>\n");
    }

    private void appendLegend(StringBuilder svg, String plotToken) {
        svg.append("    <g id=\"circos-legend-").append(plotToken).append("\" class=\"circos-legend\">\n");
        legendItem(svg, 16, 640, theme.gainColor(), "Gain / Duplication / Amplification");
        legendItem(svg, 16, 658, theme.lossColor(), "Loss / Deletion");
        legendItem(svg, 16, 676, theme.linkColor(), "Translocation");
        svg.append("    </g>\n");
    }

    private void legendItem(StringBuilder svg, int x, int y, String color, String label) {
        svg.append("      <rect x=\"").append(x).append("\" y=\"").append(y - 6)
                .append("\" width=\"12\" height=\"12\" fill=\"").append(color)
                .append("\" fill-opacity=\"0.88\"/><text class=\"legend-label\" x=\"")
                .append(x + 18).append("\" y=\"").append(y).append("\">")
                .append(XmlEscaper.text(label)).append("</text>\n");
    }

    private void validateSafeIdCollisions(CircosPlot plot) {
        collisionCheck(plot.segments().stream().map(s -> s.id()).toList(), "segment");
        collisionCheck(plot.links().stream().map(l -> l.id()).toList(), "link");
    }

    private void collisionCheck(java.util.List<String> externalIds, String type) {
        Map<String, String> encoded = new LinkedHashMap<>();
        for (String externalId : externalIds) {
            String token = ids.encode(externalId);
            String previous = encoded.putIfAbsent(token, externalId);
            if (previous != null && !previous.equals(externalId)) {
                throw new CircosGenerationException("Safe SVG ID collision for " + type + " IDs");
            }
        }
    }

    private String metadataJson(CircosPlot plot) {
        StringBuilder json = new StringBuilder("{\"sourceResultIds\":[");
        for (int i = 0; i < plot.sourceResultIds().size(); i++) {
            if (i > 0) json.append(',');
            json.append(jsonString(plot.sourceResultIds().get(i)));
        }
        json.append(']');
        if (plot.label() != null) json.append(",\"label\":").append(jsonString(plot.label()));
        return json.append('}').toString();
    }

    private String jsonString(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        value.codePoints().forEach(codePoint -> {
            switch (codePoint) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (codePoint < 0x20) escaped.append(String.format(java.util.Locale.ROOT, "\\u%04x", codePoint));
                    else escaped.appendCodePoint(codePoint);
                }
            }
        });
        return escaped.append('"').toString();
    }

    private String attr(String name, String value) {
        return " " + name + "=\"" + XmlEscaper.attribute(value) + "\"";
    }

    private String optionalAttr(String name, String value) {
        return value == null ? "" : attr(name, value);
    }

    private <T> T required(Map<String, T> map, String id, String type) {
        T value = map.get(id);
        if (value == null) throw new CircosGenerationException("Missing " + type + " geometry for " + id);
        return value;
    }
}

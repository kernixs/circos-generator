package org.mpg.circos.layout;

import java.util.List;

public record PlotGeometry(
        double viewBoxSize,
        double centerX,
        double centerY,
        List<SectorGeometry> sectors,
        TrackLayout tracks,
        List<SegmentGeometry> segments,
        List<LinkGeometry> links) {

    public PlotGeometry {
        sectors = List.copyOf(sectors);
        segments = List.copyOf(segments);
        links = List.copyOf(links);
    }

    public record SegmentGeometry(String id, AnnularPath interval, PolarPoint marker) {}

    public record LinkGeometry(String id, RibbonGeometry ribbon, int eventCount) {}
}

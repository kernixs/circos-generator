package org.mpg.circos.model;

import java.util.List;
import java.util.Objects;

public record CircosPlot(
        SchemaVersion schemaVersion,
        String plotId,
        String label,
        PlotMode mode,
        String assemblyId,
        CoordinateConvention coordinateConvention,
        List<String> sourceResultIds,
        List<GenomicSegment> segments,
        List<GenomicLink> links) {
    public CircosPlot {
        Objects.requireNonNull(schemaVersion, "schemaVersion");
        Objects.requireNonNull(plotId, "plotId");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(assemblyId, "assemblyId");
        Objects.requireNonNull(coordinateConvention, "coordinateConvention");
        sourceResultIds = List.copyOf(Objects.requireNonNull(sourceResultIds, "sourceResultIds"));
        segments = List.copyOf(Objects.requireNonNull(segments, "segments"));
        links = List.copyOf(Objects.requireNonNull(links, "links"));
    }
}

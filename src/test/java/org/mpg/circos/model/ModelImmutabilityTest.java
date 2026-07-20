package org.mpg.circos.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelImmutabilityTest {
    @Test
    void plotCopiesInputCollections() {
        List<String> sourceIds = new ArrayList<>(List.of("r"));
        var plot = new CircosPlot(SchemaVersion.V1_0, "p", null, PlotMode.PATIENT,
                "GRCh38", CoordinateConvention.ZERO_BASED_HALF_OPEN, sourceIds, List.of(), List.of());
        sourceIds.add("mutated");
        assertThrows(UnsupportedOperationException.class, () -> plot.sourceResultIds().add("x"));
    }

    @Test
    void tooltipMetadataCopiesInputCollections() {
        List<String> genes = new ArrayList<>(List.of("EGFR"));
        var annotations = new SegmentAnnotationMetadata(genes, List.of("WGS"));
        genes.add("MET");
        assertEquals(List.of("EGFR"), annotations.genes());
        assertThrows(UnsupportedOperationException.class, () -> annotations.genes().add("BRAF"));

        List<ConfidenceCount> confidence = new ArrayList<>(List.of(new ConfidenceCount("High", 1)));
        var aggregate = new CohortAggregate(1, 1, 1, "Exact interval", confidence);
        confidence.add(new ConfidenceCount("Low", 1));
        assertEquals(1, aggregate.confidenceDistribution().size());
        assertThrows(UnsupportedOperationException.class,
                () -> aggregate.confidenceDistribution().add(new ConfidenceCount("Medium", 1)));
    }
}

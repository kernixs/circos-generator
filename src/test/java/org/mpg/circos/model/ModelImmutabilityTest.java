package org.mpg.circos.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;

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
    void legacyPointAdapterRetainsProvenanceWithoutChangingPreferredIntervalApi() {
        var legacy = LinkEndpoint.fromLegacyPoint("legacy", "1", 99);
        var interval = new LinkEndpoint("interval", new GenomicInterval("1", 90, 110));

        assertEquals(new GenomicInterval("1", 99, 100), legacy.interval());
        assertEquals(99L, legacy.legacyPosition());
        assertEquals(true, legacy.isLegacyPoint());
        assertEquals(false, interval.isLegacyPoint());
        assertThrows(IllegalStateException.class, interval::position);
    }

    @Test
    void tooltipMetadataCopiesInputCollections() {
        List<String> genes = new ArrayList<>(List.of("EGFR"));
        var metadata = new LinkedHashMap<String, String>();
        metadata.put("z", "last");
        metadata.put("a", "first");
        var annotations = new SegmentAnnotationMetadata(genes, List.of("WGS"), metadata);
        genes.add("MET");
        metadata.put("later", "mutation");
        assertEquals(List.of("EGFR"), annotations.genes());
        assertThrows(UnsupportedOperationException.class, () -> annotations.genes().add("BRAF"));
        assertEquals(List.of("a", "z"), annotations.additionalMetadata().keySet().stream().toList());
        assertThrows(UnsupportedOperationException.class,
                () -> annotations.additionalMetadata().put("x", "value"));

        List<ConfidenceCount> confidence = new ArrayList<>(List.of(new ConfidenceCount("High", 1)));
        var aggregate = new CohortAggregate(1, 1, 1, "Exact interval", confidence);
        confidence.add(new ConfidenceCount("Low", 1));
        assertEquals(1, aggregate.confidenceDistribution().size());
        assertThrows(UnsupportedOperationException.class,
                () -> aggregate.confidenceDistribution().add(new ConfidenceCount("Medium", 1)));
    }
}

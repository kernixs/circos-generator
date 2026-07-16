package org.mpg.circos.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelImmutabilityTest {
    @Test
    void plotCopiesInputCollections() {
        List<String> sourceIds = new ArrayList<>(List.of("r"));
        var plot = new CircosPlot(SchemaVersion.V1_0, "p", null, PlotMode.PATIENT,
                "GRCh38", CoordinateConvention.ZERO_BASED_HALF_OPEN, sourceIds, List.of(), List.of());
        sourceIds.add("mutated");
        assertThrows(UnsupportedOperationException.class, () -> plot.sourceResultIds().add("x"));
    }
}

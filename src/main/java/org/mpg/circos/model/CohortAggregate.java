package org.mpg.circos.model;

import java.util.List;
import java.util.Objects;

public record CohortAggregate(
        int eventCount,
        int patientCount,
        int sampleCount,
        String groupingDescription,
        List<ConfidenceCount> confidenceDistribution) {
    public CohortAggregate {
        confidenceDistribution = List.copyOf(Objects.requireNonNull(confidenceDistribution,
                "confidenceDistribution"));
    }

    public CohortAggregate(int eventCount, int patientCount, int sampleCount) {
        this(eventCount, patientCount, sampleCount, null, List.of());
    }
}

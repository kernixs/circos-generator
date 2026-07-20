package org.mpg.circos.model;

import java.util.Objects;

public record GenomicSegment(
        String id,
        String sourceResultId,
        String eventGroupId,
        GenomicInterval interval,
        EventType eventType,
        Integer copyNumber,
        String confidence,
        String label,
        SegmentDisplayType displayType,
        SegmentAnnotationMetadata annotations,
        CohortAggregate aggregate) {
    public GenomicSegment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sourceResultId, "sourceResultId");
        Objects.requireNonNull(interval, "interval");
        Objects.requireNonNull(eventType, "eventType");
    }

    public GenomicSegment(String id, String sourceResultId, String eventGroupId, GenomicInterval interval,
            EventType eventType, Integer copyNumber, String confidence, String label) {
        this(id, sourceResultId, eventGroupId, interval, eventType, copyNumber, confidence, label,
                null, null, null);
    }
}

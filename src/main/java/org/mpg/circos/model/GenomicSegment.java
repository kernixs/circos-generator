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
        String label) {
    public GenomicSegment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sourceResultId, "sourceResultId");
        Objects.requireNonNull(interval, "interval");
        Objects.requireNonNull(eventType, "eventType");
    }
}

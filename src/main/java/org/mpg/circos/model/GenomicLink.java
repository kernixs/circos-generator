package org.mpg.circos.model;

import java.util.Objects;

public record GenomicLink(
        String id,
        String eventGroupId,
        LinkEndpoint source,
        LinkEndpoint target,
        String sourceResultId,
        EventType eventType,
        String confidence,
        CohortAggregate aggregate,
        String label) {
    public GenomicLink {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(eventType, "eventType");
    }
}

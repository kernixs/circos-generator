package org.mpg.circos.model;

import java.util.Objects;

public record LinkEndpoint(String segmentId, String chromosome, long position) {
    public LinkEndpoint {
        Objects.requireNonNull(segmentId, "segmentId");
        Objects.requireNonNull(chromosome, "chromosome");
    }
}

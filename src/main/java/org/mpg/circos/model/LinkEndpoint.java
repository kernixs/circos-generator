package org.mpg.circos.model;

import java.util.Objects;

/**
 * One end of a genomic link. Interval endpoints are the preferred V2 contract.
 * A legacy point marker is retained only for explicit Version 1 compatibility.
 */
public record LinkEndpoint(String segmentId, GenomicInterval interval, Long legacyPosition) {
    public LinkEndpoint {
        Objects.requireNonNull(segmentId, "segmentId");
        Objects.requireNonNull(interval, "interval");
    }

    public LinkEndpoint(String segmentId, GenomicInterval interval) {
        this(segmentId, interval, null);
    }

    /**
     * Compatibility constructor for existing point-based callers. New code should
     * construct an endpoint with a caller-supplied genomic interval.
     */
    @Deprecated(forRemoval = false)
    public LinkEndpoint(String segmentId, String chromosome, long position) {
        this(segmentId, compatibilityInterval(chromosome, position), position);
    }

    public static LinkEndpoint fromLegacyPoint(String segmentId, String chromosome, long position) {
        return new LinkEndpoint(segmentId, compatibilityInterval(chromosome, position), position);
    }

    public String chromosome() {
        return interval.chromosome();
    }

    public boolean isLegacyPoint() {
        return legacyPosition != null;
    }

    public double midpoint() {
        return interval.start() + (interval.end() - interval.start()) / 2.0;
    }

    /**
     * Returns the original V1 point. This is not defined for interval endpoints.
     */
    @Deprecated(forRemoval = false)
    public long position() {
        if (legacyPosition == null) {
            throw new IllegalStateException("Interval endpoints do not have an exact point position");
        }
        return legacyPosition;
    }

    private static GenomicInterval compatibilityInterval(String chromosome, long position) {
        long end = position == Long.MAX_VALUE ? Long.MAX_VALUE : position + 1;
        return new GenomicInterval(chromosome, position, end);
    }
}

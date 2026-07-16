package org.mpg.circos.model;

import java.util.Objects;

public record GenomicInterval(String chromosome, long start, long end) {
    public GenomicInterval {
        Objects.requireNonNull(chromosome, "chromosome");
    }
}

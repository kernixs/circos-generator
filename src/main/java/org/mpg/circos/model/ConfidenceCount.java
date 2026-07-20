package org.mpg.circos.model;

import java.util.Objects;

public record ConfidenceCount(String label, int count) {
    public ConfidenceCount {
        Objects.requireNonNull(label, "label");
    }
}

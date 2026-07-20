package org.mpg.circos.model;

import java.util.List;
import java.util.Objects;

public record SegmentAnnotationMetadata(List<String> genes, List<String> methods) {
    public SegmentAnnotationMetadata {
        genes = List.copyOf(Objects.requireNonNull(genes, "genes"));
        methods = List.copyOf(Objects.requireNonNull(methods, "methods"));
    }
}

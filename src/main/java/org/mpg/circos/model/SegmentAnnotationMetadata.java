package org.mpg.circos.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record SegmentAnnotationMetadata(
        List<String> genes,
        List<String> methods,
        Map<String, String> additionalMetadata) {
    public SegmentAnnotationMetadata {
        genes = List.copyOf(Objects.requireNonNull(genes, "genes"));
        methods = List.copyOf(Objects.requireNonNull(methods, "methods"));
        additionalMetadata = java.util.Collections.unmodifiableMap(new TreeMap<>(Objects.requireNonNull(
                additionalMetadata, "additionalMetadata")));
    }

    public SegmentAnnotationMetadata(List<String> genes, List<String> methods) {
        this(genes, methods, Map.of());
    }
}

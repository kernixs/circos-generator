package org.mpg.circos.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record LinkAnnotationMetadata(
        List<String> sourceGenes,
        List<String> targetGenes,
        List<String> methods,
        Map<String, String> additionalMetadata) {
    public LinkAnnotationMetadata {
        sourceGenes = List.copyOf(Objects.requireNonNull(sourceGenes, "sourceGenes"));
        targetGenes = List.copyOf(Objects.requireNonNull(targetGenes, "targetGenes"));
        methods = List.copyOf(Objects.requireNonNull(methods, "methods"));
        additionalMetadata = java.util.Collections.unmodifiableMap(new TreeMap<>(Objects.requireNonNull(
                additionalMetadata, "additionalMetadata")));
    }

    public LinkAnnotationMetadata(List<String> sourceGenes, List<String> targetGenes, List<String> methods) {
        this(sourceGenes, targetGenes, methods, Map.of());
    }
}

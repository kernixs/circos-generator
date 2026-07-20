package org.mpg.circos.model;

import java.util.List;
import java.util.Objects;

public record LinkAnnotationMetadata(List<String> sourceGenes, List<String> targetGenes, List<String> methods) {
    public LinkAnnotationMetadata {
        sourceGenes = List.copyOf(Objects.requireNonNull(sourceGenes, "sourceGenes"));
        targetGenes = List.copyOf(Objects.requireNonNull(targetGenes, "targetGenes"));
        methods = List.copyOf(Objects.requireNonNull(methods, "methods"));
    }
}

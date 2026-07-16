package org.mpg.circos.assembly;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record GenomeAssembly(AssemblyId id, List<Chromosome> chromosomes) {
    public GenomeAssembly {
        Objects.requireNonNull(id, "id");
        chromosomes = List.copyOf(Objects.requireNonNull(chromosomes, "chromosomes"));
    }

    public Map<String, Chromosome> chromosomesByName() {
        return chromosomes.stream().collect(Collectors.toUnmodifiableMap(Chromosome::name, c -> c));
    }
}

package org.mpg.circos.assembly;

import java.util.List;
import java.util.Objects;

public record Chromosome(String name, long length, List<String> aliases) {
    public Chromosome {
        Objects.requireNonNull(name, "name");
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
    }
}

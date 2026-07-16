package org.mpg.circos.assembly;

import java.util.Locale;

public final class ChromosomeNormalizer {
    public String normalize(String chromosome, GenomeAssembly assembly) {
        if (chromosome == null || chromosome.isBlank()) {
            throw new IllegalArgumentException("chromosome is required");
        }
        String candidate = chromosome.trim();
        if (candidate.regionMatches(true, 0, "chr", 0, 3)) candidate = candidate.substring(3);
        String uppercase = candidate.toUpperCase(Locale.ROOT);
        String normalized;
        if (uppercase.equals("X") || uppercase.equals("Y")) normalized = uppercase;
        else if (candidate.matches("0*[0-9]+")) normalized = candidate.replaceFirst("^0+(?=\\d)", "");
        else normalized = candidate;
        return assembly.chromosomes().stream()
                .filter(c -> c.name().equals(normalized)
                        || c.aliases().stream().anyMatch(a -> a.equalsIgnoreCase(chromosome.trim())))
                .map(Chromosome::name)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported chromosome '" + chromosome + "' for " + assembly.id().canonical()));
    }
}

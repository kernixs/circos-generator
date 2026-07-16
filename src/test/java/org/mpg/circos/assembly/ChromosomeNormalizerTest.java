package org.mpg.circos.assembly;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChromosomeNormalizerTest {
    private final GenomeAssembly assembly = new ClasspathAssemblyRepository().load("GRCh38");
    private final ChromosomeNormalizer normalizer = new ChromosomeNormalizer();

    @Test
    void normalizesChrAliasesAndSexChromosomes() {
        assertEquals("1", normalizer.normalize("chr1", assembly));
        assertEquals("X", normalizer.normalize("chrx", assembly));
        assertEquals("Y", normalizer.normalize("Y", assembly));
    }

    @Test
    void rejectsUnknownChromosome() {
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalize("chrM", assembly));
    }
}

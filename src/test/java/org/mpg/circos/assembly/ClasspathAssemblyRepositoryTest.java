package org.mpg.circos.assembly;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClasspathAssemblyRepositoryTest {
    private final AssemblyRepository repository = new ClasspathAssemblyRepository();

    @Test
    void loadsBothSupportedAssembliesInCanonicalOrder() {
        var grch37 = repository.load("hg19");
        var grch38 = repository.load("GRCh38");
        assertEquals(24, grch37.chromosomes().size());
        assertEquals("1", grch37.chromosomes().getFirst().name());
        assertEquals("Y", grch38.chromosomes().getLast().name());
        assertEquals(248956422L, grch38.chromosomes().getFirst().length());
    }

    @Test
    void rejectsUnsupportedAssembly() {
        assertThrows(IllegalArgumentException.class, () -> repository.load("T2T-CHM13"));
    }
}

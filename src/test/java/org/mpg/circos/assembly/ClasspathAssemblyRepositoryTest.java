package org.mpg.circos.assembly;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClasspathAssemblyRepositoryTest {
    private final AssemblyRepository repository = new ClasspathAssemblyRepository();

    @Test
    void loadsSupportedAssembliesInCanonicalOrder() {
        var grch37 = repository.load("hg19");
        var grch38 = repository.load("GRCh38");
        var t2t = repository.load("hs1");
        assertEquals(24, grch37.chromosomes().size());
        assertEquals("1", grch37.chromosomes().getFirst().name());
        assertEquals("Y", grch38.chromosomes().getLast().name());
        assertEquals(248956422L, grch38.chromosomes().getFirst().length());
        assertEquals(AssemblyId.T2T_CHM13, t2t.id());
        assertEquals(24, t2t.chromosomes().size());
        assertEquals(248387328L, t2t.chromosomes().getFirst().length());
        assertEquals(62460029L, t2t.chromosomes().getLast().length());
    }

    @Test
    void acceptsT2tChm13v2Aliases() {
        assertEquals(AssemblyId.T2T_CHM13, repository.load("T2T-CHM13").id());
        assertEquals(AssemblyId.T2T_CHM13, repository.load("CHM13v2.0").id());
        assertEquals(AssemblyId.T2T_CHM13, repository.load("T2T").id());
    }

    @Test
    void rejectsUnsupportedAssembly() {
        assertThrows(IllegalArgumentException.class, () -> repository.load("GRCh36"));
    }
}

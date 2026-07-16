package org.mpg.circos.assembly;

public interface AssemblyRepository {
    GenomeAssembly load(String assemblyId);
}

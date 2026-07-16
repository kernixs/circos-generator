package org.mpg.circos.validation;

import org.mpg.circos.assembly.AssemblyId;
import org.mpg.circos.assembly.GenomeAssembly;
import org.mpg.circos.model.CircosPlot;

import java.util.List;

final class AssemblyValidator {
    List<ValidationError> validate(CircosPlot plot, GenomeAssembly assembly) {
        try {
            AssemblyId id = AssemblyId.from(plot.assemblyId());
            if (id != assembly.id()) {
                return List.of(new ValidationError("ASSEMBLY_MISMATCH", "/assemblyId",
                        "Assembly resource does not match input"));
            }
            return List.of();
        } catch (IllegalArgumentException e) {
            return List.of(new ValidationError("UNKNOWN_ASSEMBLY", "/assemblyId", e.getMessage()));
        }
    }
}

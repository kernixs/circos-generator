package org.mpg.circos.validation;

import org.mpg.circos.assembly.AssemblyRepository;
import org.mpg.circos.assembly.GenomeAssembly;
import org.mpg.circos.model.CircosPlot;

import java.util.ArrayList;
import java.util.List;

public final class DomainValidator {
    private final AssemblyRepository assemblies;
    private final AssemblyValidator assemblyValidator = new AssemblyValidator();
    private final CoordinateValidator coordinateValidator = new CoordinateValidator();
    private final ReferenceValidator referenceValidator = new ReferenceValidator();
    private final BusinessRulesValidator businessRulesValidator = new BusinessRulesValidator();

    public DomainValidator(AssemblyRepository assemblies) { this.assemblies = assemblies; }

    public CircosPlot validateAndNormalize(CircosPlot plot) {
        List<ValidationError> errors = new ArrayList<>();
        GenomeAssembly assembly;
        try {
            assembly = assemblies.load(plot.assemblyId());
        } catch (RuntimeException e) {
            errors.add(new ValidationError("UNKNOWN_ASSEMBLY", "/assemblyId", e.getMessage()));
            throw new ValidationException(errors);
        }
        errors.addAll(assemblyValidator.validate(plot, assembly));
        errors.addAll(coordinateValidator.validate(plot, assembly));
        errors.addAll(referenceValidator.validate(plot));
        errors.addAll(businessRulesValidator.validate(plot));
        if (!errors.isEmpty()) throw new ValidationException(errors);
        return coordinateValidator.normalize(plot, assembly);
    }
}

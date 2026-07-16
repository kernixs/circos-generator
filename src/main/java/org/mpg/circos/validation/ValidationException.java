package org.mpg.circos.validation;

import java.util.List;
import java.util.stream.Collectors;

public final class ValidationException extends RuntimeException {
    private final List<ValidationError> errors;

    public ValidationException(List<ValidationError> errors) {
        super(errors.stream().sorted(ValidationError.ORDER)
                .map(e -> e.code() + " " + e.path() + ": " + e.message())
                .collect(Collectors.joining("; ")));
        this.errors = errors.stream().sorted(ValidationError.ORDER).toList();
    }

    public List<ValidationError> errors() { return errors; }
}

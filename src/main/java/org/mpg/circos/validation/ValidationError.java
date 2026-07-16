package org.mpg.circos.validation;

import java.util.Comparator;

public record ValidationError(String code, String path, String message) {
    public static final Comparator<ValidationError> ORDER = Comparator
            .comparing(ValidationError::path)
            .thenComparing(ValidationError::code)
            .thenComparing(ValidationError::message);
}

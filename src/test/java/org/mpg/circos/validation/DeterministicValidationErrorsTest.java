package org.mpg.circos.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeterministicValidationErrorsTest {
    @Test
    void errorsAreStableAndSorted() {
        var reader = new PlotInputReader();
        ValidationException first = readInvalid(reader);
        ValidationException second = readInvalid(reader);
        assertEquals(first.errors(), second.errors());
        assertEquals(first.errors().stream().sorted(ValidationError.ORDER).toList(), first.errors());
    }

    private ValidationException readInvalid(PlotInputReader reader) {
        try (var input = getClass().getResourceAsStream("/fixtures/invalid/duplicate-id.json")) {
            try { reader.read(input); throw new AssertionError("expected validation failure"); }
            catch (ValidationException e) { return e; }
        } catch (Exception e) { throw new AssertionError(e); }
    }
}

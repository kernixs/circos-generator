package org.mpg.circos.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AssemblyValidatorTest {
    @Test
    void rejectsUnsupportedAssemblyBeforeCoordinateChecks() {
        try (var input = getClass().getResourceAsStream("/fixtures/invalid/unknown-assembly.json")) {
            assertThrows(ValidationException.class, () -> new PlotInputReader().read(input));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}

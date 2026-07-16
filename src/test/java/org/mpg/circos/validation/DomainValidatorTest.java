package org.mpg.circos.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainValidatorTest {
    private final PlotInputReader reader = new PlotInputReader();

    @Test
    void validatesPatientAndCohortRules() {
        assertValid("/examples/patient-bcr-abl1.json");
        assertValid("/examples/cohort-aggregate.json");
        assertInvalid("/fixtures/invalid/patient-with-aggregate.json", "PATIENT_AGGREGATE_FORBIDDEN");
        assertInvalid("/fixtures/invalid/cohort-without-aggregate.json", "COHORT_AGGREGATE_REQUIRED");
    }

    private void assertValid(String resource) {
        try (var input = getClass().getResourceAsStream(resource)) { reader.read(input); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    private void assertInvalid(String resource, String code) {
        try (var input = getClass().getResourceAsStream(resource)) {
            var exception = org.junit.jupiter.api.Assertions.assertThrows(ValidationException.class, () -> reader.read(input));
            assertTrue(exception.errors().stream().anyMatch(error -> error.code().equals(code)));
        } catch (Exception e) { throw new AssertionError(e); }
    }
}

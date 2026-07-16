package org.mpg.circos.validation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.TestFixtures;

class BusinessRulesValidatorTest {
    @Test
    void acceptsCohortWithOneRepresentedSourceResult() {
        assertDoesNotThrow(() -> new CircosApplication()
                .readAndValidate(TestFixtures.open("/examples/cohort-single-result.json")));
    }

    @ParameterizedTest
    @MethodSource("invalidRules")
    void rejectsInvalidRules(String fixture) {
        assertThrows(ValidationException.class, () -> new CircosApplication().readAndValidate(TestFixtures.open(fixture)));
    }

    static Stream<Arguments> invalidRules() {
        return Stream.of(
                Arguments.of("/fixtures/invalid/patient-with-aggregate.json"),
                Arguments.of("/fixtures/invalid/cohort-without-aggregate.json"),
                Arguments.of("/fixtures/invalid/inconsistent-aggregate-counts.json"),
                Arguments.of("/fixtures/invalid/gain-null-copy-number.json"),
                Arguments.of("/fixtures/invalid/gain-copy-number-below-three.json"));
    }
}

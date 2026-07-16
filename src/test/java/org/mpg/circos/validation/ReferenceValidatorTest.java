package org.mpg.circos.validation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.TestFixtures;

class ReferenceValidatorTest {
    @ParameterizedTest
    @MethodSource("invalidReferences")
    void rejectsInvalidReferences(String fixture, String code) {
        var exception = assertThrows(ValidationException.class, () -> new CircosApplication().readAndValidate(TestFixtures.open(fixture)));
        assertTrue(exception.errors().stream().anyMatch(e -> e.code().equals(code)), exception::getMessage);
    }

    static Stream<Arguments> invalidReferences() {
        return Stream.of(
                Arguments.of("/fixtures/invalid/duplicate-segment-id.json", "reference.segment.duplicate"),
                Arguments.of("/fixtures/invalid/unknown-source-result.json", "reference.sourceResult.unresolved"),
                Arguments.of("/fixtures/invalid/unknown-link-source-result.json", "reference.sourceResult.unresolved"));
    }
}

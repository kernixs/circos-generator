package org.mpg.circos.validation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.TestFixtures;

class CoordinateValidatorTest {
    @ParameterizedTest
    @MethodSource("invalidCoordinates")
    void rejectsInvalidCoordinates(String fixture, String code) {
        var exception = assertThrows(ValidationException.class, () -> new CircosApplication().readAndValidate(TestFixtures.open(fixture)));
        assertTrue(exception.errors().stream().anyMatch(e -> e.code().equals(code)), exception::getMessage);
    }

    static Stream<Arguments> invalidCoordinates() {
        return Stream.of(
                Arguments.of("/fixtures/invalid/unknown-chromosome.json", "coordinate.chromosome.unsupported"),
                Arguments.of("/fixtures/invalid/reversed-interval.json", "coordinate.interval.order"),
                Arguments.of("/fixtures/invalid/segment-out-of-bounds.json", "coordinate.interval.bounds"),
                Arguments.of("/fixtures/invalid/link-position-out-of-bounds.json", "coordinate.point.bounds"),
                Arguments.of("/fixtures/invalid/v2-reversed-link-interval.json", "coordinate.interval.order"),
                Arguments.of("/fixtures/invalid/v2-link-interval-out-of-bounds.json", "coordinate.interval.bounds"));
    }

    @Test void acceptsChromosomeStartAndLastBase() throws Exception {
        assertNotNull(new CircosApplication().readAndValidate(TestFixtures.open("/examples/patient-bcr-abl1.json")));
    }
}


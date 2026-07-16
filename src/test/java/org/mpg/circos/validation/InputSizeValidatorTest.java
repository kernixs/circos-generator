package org.mpg.circos.validation;

import org.junit.jupiter.api.Test;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.RenderOptions;
import org.mpg.circos.TestFixtures;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InputSizeValidatorTest {
    @Test
    void acceptsExactLimitAndRejectsWithoutTruncatingAboveIt() {
        var application = new CircosApplication();
        assertDoesNotThrow(() -> application.render(
                TestFixtures.open("/examples/gains-and-losses.json"), new RenderOptions(2)));

        var exception = assertThrows(ValidationException.class, () -> application.render(
                TestFixtures.open("/examples/gains-and-losses.json"), new RenderOptions(1)));
        assertEquals("INPUT_SIZE_LIMIT_EXCEEDED", exception.errors().getFirst().code());
        assertEquals("Plot contains 2 events; configured maximum is 1",
                exception.errors().getFirst().message());
    }
}

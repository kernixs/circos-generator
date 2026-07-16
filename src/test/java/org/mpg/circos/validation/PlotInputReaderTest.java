package org.mpg.circos.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlotInputReaderTest {
    private final PlotInputReader reader = new PlotInputReader();

    @Test
    void preservesExplicitNullLossCopyNumber() {
        try (var input = getClass().getResourceAsStream("/examples/gains-and-losses.json")) {
            var plot = reader.read(input);
            assertEquals(null, plot.segments().get(1).copyNumber());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void rejectsMalformedDomainFixtures() {
        assertInvalid("/fixtures/invalid/gain-copy-number-below-three.json", "GAIN_COPY_NUMBER_INVALID");
        assertInvalid("/fixtures/invalid/negative-coordinate.json", "NEGATIVE_COORDINATE");
        assertInvalid("/fixtures/invalid/reversed-interval.json", "coordinate.interval.order");
    }

    @org.junit.jupiter.api.Test
    void rejectsMalformedJson() {
        try (var input = getClass().getResourceAsStream("/fixtures/invalid/malformed-json.json")) {
            assertThrows(ValidationException.class, () -> reader.read(input));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void assertInvalid(String resource, String code) {
        try (var input = getClass().getResourceAsStream(resource)) {
            var exception = assertThrows(ValidationException.class, () -> reader.read(input));
            if (exception.errors().stream().noneMatch(error -> error.code().equals(code))) {
                throw new AssertionError("Missing error code " + code);
            }
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}

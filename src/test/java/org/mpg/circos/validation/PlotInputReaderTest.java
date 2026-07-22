package org.mpg.circos.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void parsesTypedTooltipMetadata() {
        try (var input = getClass().getResourceAsStream("/examples/cohort-aggregate.json")) {
            var link = reader.read(input).links().get(0);
            assertEquals(List.of("ABL1"), link.annotations().sourceGenes());
            assertEquals(List.of("BCR"), link.annotations().targetGenes());
            assertEquals("Exact breakpoints", link.aggregate().groupingDescription());
            assertEquals("High", link.aggregate().confidenceDistribution().get(0).label());
            assertEquals(4, link.aggregate().confidenceDistribution().get(0).count());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void parsesVersionTwoIntervalEndpointsAndDisplayMetadata() {
        try (var input = getClass().getResourceAsStream("/examples/v2-interval-links.json")) {
            var plot = reader.read(input);
            var endpoint = plot.links().getFirst().source();
            assertEquals(org.mpg.circos.model.SchemaVersion.V2_0, plot.schemaVersion());
            assertEquals(new org.mpg.circos.model.GenomicInterval("9", 133500000, 133700000),
                    endpoint.interval());
            assertFalse(endpoint.isLegacyPoint());
            assertEquals("Confirmed", plot.links().getFirst().annotations()
                    .additionalMetadata().get("Review status"));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void retainsExplicitVersionOnePointCompatibility() {
        try (var input = getClass().getResourceAsStream("/examples/crossing-links.json")) {
            var endpoint = reader.read(input).links().getFirst().source();
            assertEquals(133599999L, endpoint.legacyPosition());
            assertEquals(new org.mpg.circos.model.GenomicInterval("9", 133599999, 133600000),
                    endpoint.interval());
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

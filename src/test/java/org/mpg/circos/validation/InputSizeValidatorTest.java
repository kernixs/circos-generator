package org.mpg.circos.validation;

import org.junit.jupiter.api.Test;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.RenderOptions;
import org.mpg.circos.TestFixtures;
import org.mpg.circos.model.CircosPlot;
import org.mpg.circos.model.CoordinateConvention;
import org.mpg.circos.model.EventType;
import org.mpg.circos.model.GenomicInterval;
import org.mpg.circos.model.GenomicSegment;
import org.mpg.circos.model.PlotMode;
import org.mpg.circos.model.SchemaVersion;

import java.util.List;
import java.util.stream.IntStream;

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

    @Test
    void acceptsTwentyThousandAndDeterministicallyRejectsTwentyThousandOne() {
        List<GenomicSegment> segments = IntStream.range(0, 20_001)
                .mapToObj(this::lossSegment)
                .toList();
        InputSizeValidator validator = new InputSizeValidator();

        assertDoesNotThrow(() -> validator.validate(plot(segments.subList(0, 20_000)),
                RenderOptions.DEFAULT_MAXIMUM_EVENT_COUNT));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> validator.validate(plot(segments), RenderOptions.DEFAULT_MAXIMUM_EVENT_COUNT));
        assertEquals("INPUT_SIZE_LIMIT_EXCEEDED", exception.errors().getFirst().code());
        assertEquals("Plot contains 20001 events; configured maximum is 20000",
                exception.errors().getFirst().message());
    }

    private GenomicSegment lossSegment(int index) {
        long start = index * 10L;
        return new GenomicSegment("segment-" + index, "result-1", null,
                new GenomicInterval("1", start, start + 5), EventType.LOSS, null, null, null);
    }

    private CircosPlot plot(List<GenomicSegment> segments) {
        return new CircosPlot(SchemaVersion.V1_0, "size-boundary", null, PlotMode.PATIENT,
                "GRCh37", CoordinateConvention.ZERO_BASED_HALF_OPEN,
                List.of("result-1"), segments, List.of());
    }
}

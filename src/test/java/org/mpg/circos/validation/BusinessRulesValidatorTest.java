package org.mpg.circos.validation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.TestFixtures;
import org.mpg.circos.model.CircosPlot;
import org.mpg.circos.model.EventType;
import org.mpg.circos.model.GenomicSegment;
import org.mpg.circos.model.SegmentDisplayType;

class BusinessRulesValidatorTest {
    @Test
    void acceptsCohortWithOneRepresentedSourceResult() {
        assertDoesNotThrow(() -> new CircosApplication()
                .readAndValidate(TestFixtures.open("/examples/cohort-single-result.json")));
    }

    @Test
    void rejectsTranslocationSegmentFromDirectJavaApi() {
        CircosApplication application = new CircosApplication();
        CircosPlot valid = application.validate(TestFixtures.open("/examples/gains-and-losses.json"));
        GenomicSegment original = valid.segments().get(0);
        GenomicSegment invalid = new GenomicSegment(original.id(), original.sourceResultId(),
                original.eventGroupId(), original.interval(), EventType.TRANSLOCATION, null,
                original.confidence(), original.label());
        CircosPlot plot = new CircosPlot(valid.schemaVersion(), valid.plotId(), valid.label(), valid.mode(),
                valid.assemblyId(), valid.coordinateConvention(), valid.sourceResultIds(), List.of(invalid),
                valid.links());

        ValidationException exception = assertThrows(ValidationException.class, () -> application.render(plot));

        assertTrue(exception.errors().stream().anyMatch(error ->
                error.code().equals("SEGMENT_EVENT_TYPE_INVALID")
                        && error.path().equals("/segments/0/eventType")));
    }

    @Test
    void rejectsGainOrLossLinkFromDirectJavaApi() {
        CircosApplication application = new CircosApplication();
        CircosPlot valid = application.validate(TestFixtures.open("/examples/v2-interval-links.json"));
        var original = valid.links().getFirst();
        var invalid = new org.mpg.circos.model.GenomicLink(original.id(), original.eventGroupId(),
                original.source(), original.target(), original.sourceResultId(), EventType.GAIN,
                original.confidence(), original.aggregate(), original.label(), original.annotations());
        CircosPlot plot = new CircosPlot(valid.schemaVersion(), valid.plotId(), valid.label(), valid.mode(),
                valid.assemblyId(), valid.coordinateConvention(), valid.sourceResultIds(), valid.segments(),
                List.of(invalid));

        ValidationException exception = assertThrows(ValidationException.class, () -> application.render(plot));

        assertTrue(exception.errors().stream().anyMatch(error ->
                error.code().equals("LINK_EVENT_TYPE_INVALID")
                        && error.path().equals("/links/0/eventType")));
    }

    @Test
    void rejectsDisplayTypeThatDoesNotMatchGeometryCategory() {
        CircosApplication application = new CircosApplication();
        CircosPlot valid = application.validate(TestFixtures.open("/examples/gains-and-losses.json"));
        GenomicSegment original = valid.segments().get(0);
        GenomicSegment invalid = new GenomicSegment(original.id(), original.sourceResultId(),
                original.eventGroupId(), original.interval(), original.eventType(), original.copyNumber(),
                original.confidence(), original.label(), SegmentDisplayType.DELETION,
                original.annotations(), original.aggregate());
        CircosPlot plot = new CircosPlot(valid.schemaVersion(), valid.plotId(), valid.label(), valid.mode(),
                valid.assemblyId(), valid.coordinateConvention(), valid.sourceResultIds(), List.of(invalid),
                valid.links());

        ValidationException exception = assertThrows(ValidationException.class, () -> application.render(plot));

        assertTrue(exception.errors().stream().anyMatch(error ->
                error.code().equals("SEGMENT_DISPLAY_TYPE_INVALID")
                        && error.path().equals("/segments/0/displayType")));
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

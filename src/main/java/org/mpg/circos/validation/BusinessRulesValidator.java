package org.mpg.circos.validation;

import org.mpg.circos.model.CircosPlot;
import org.mpg.circos.model.CoordinateConvention;
import org.mpg.circos.model.EventType;
import org.mpg.circos.model.PlotMode;

import java.util.ArrayList;
import java.util.List;

final class BusinessRulesValidator {
    List<ValidationError> validate(CircosPlot plot) {
        List<ValidationError> errors = new ArrayList<>();
        if (plot.coordinateConvention() != CoordinateConvention.ZERO_BASED_HALF_OPEN) {
            errors.add(new ValidationError("UNSUPPORTED_COORDINATE_CONVENTION", "/coordinateConvention",
                    "Only ZERO_BASED_HALF_OPEN is supported"));
        }
        if (plot.mode() == PlotMode.PATIENT) {
            if (plot.sourceResultIds().size() != 1) errors.add(new ValidationError("PATIENT_SCOPE_INVALID",
                    "/sourceResultIds", "patient mode requires exactly one source result ID"));
            for (int i = 0; i < plot.links().size(); i++) {
                var link = plot.links().get(i);
                if (link.aggregate() != null) errors.add(new ValidationError("PATIENT_AGGREGATE_FORBIDDEN",
                        "/links/" + i + "/aggregate", "patient links cannot contain aggregate metadata"));
                if (link.sourceResultId() == null) errors.add(new ValidationError("PATIENT_SOURCE_RESULT_REQUIRED",
                        "/links/" + i + "/sourceResultId", "patient links require sourceResultId"));
            }
            for (int i = 0; i < plot.segments().size(); i++) {
                if (plot.segments().get(i).aggregate() != null) {
                    errors.add(new ValidationError("PATIENT_AGGREGATE_FORBIDDEN", "/segments/" + i + "/aggregate",
                            "patient segments cannot contain aggregate metadata"));
                }
            }
        } else {
            if (plot.sourceResultIds().isEmpty()) errors.add(new ValidationError("COHORT_SCOPE_INVALID",
                    "/sourceResultIds", "cohort mode requires at least one source result ID"));
            for (int i = 0; i < plot.links().size(); i++) {
                var link = plot.links().get(i);
                if (link.aggregate() == null) errors.add(new ValidationError("COHORT_AGGREGATE_REQUIRED",
                        "/links/" + i + "/aggregate", "cohort links require aggregate metadata"));
                if (link.sourceResultId() != null) errors.add(new ValidationError("COHORT_SOURCE_RESULT_FORBIDDEN",
                        "/links/" + i + "/sourceResultId", "cohort links cannot contain sourceResultId"));
                if (link.aggregate() != null) validateAggregate(link.aggregate(), "/links/" + i + "/aggregate", errors);
            }
        }
        for (int i = 0; i < plot.links().size(); i++) {
            if (plot.links().get(i).eventType() != EventType.TRANSLOCATION) {
                errors.add(new ValidationError("LINK_EVENT_TYPE_INVALID", "/links/" + i + "/eventType",
                        "link eventType must be translocation"));
            }
        }
        for (int i = 0; i < plot.segments().size(); i++) {
            var segment = plot.segments().get(i);
            if (segment.eventType() != EventType.GAIN && segment.eventType() != EventType.LOSS) {
                errors.add(new ValidationError("SEGMENT_EVENT_TYPE_INVALID", "/segments/" + i + "/eventType",
                        "segment eventType must be gain or loss"));
                continue;
            }
            if (segment.eventType() == EventType.GAIN
                    && segment.copyNumber() != null && segment.copyNumber() < 3) {
                errors.add(new ValidationError("GAIN_COPY_NUMBER_INVALID", "/segments/" + i + "/copyNumber",
                        "known absolute gain copyNumber must be at least 3"));
            }
            if (segment.displayType() != null && segment.displayType().geometryType() != segment.eventType()) {
                errors.add(new ValidationError("SEGMENT_DISPLAY_TYPE_INVALID", "/segments/" + i + "/displayType",
                        "displayType must match the segment gain/loss geometry category"));
            }
            if (segment.aggregate() != null) {
                validateAggregate(segment.aggregate(), "/segments/" + i + "/aggregate", errors);
            }
        }
        return errors;
    }

    private void validateAggregate(org.mpg.circos.model.CohortAggregate aggregate, String path,
                                   List<ValidationError> errors) {
        if (aggregate.eventCount() < 1 || aggregate.patientCount() < 1 || aggregate.sampleCount() < 1) {
            errors.add(new ValidationError("AGGREGATE_COUNT_INVALID", path, "aggregate counts must be positive"));
        }
        if (aggregate.patientCount() > aggregate.sampleCount() || aggregate.sampleCount() > aggregate.eventCount()) {
            errors.add(new ValidationError("AGGREGATE_COUNT_INCONSISTENT", path,
                    "counts must satisfy patientCount <= sampleCount <= eventCount"));
        }
        for (int i = 0; i < aggregate.confidenceDistribution().size(); i++) {
            var value = aggregate.confidenceDistribution().get(i);
            if (value.label().isBlank() || value.count() < 1) {
                errors.add(new ValidationError("AGGREGATE_CONFIDENCE_INVALID",
                        path + "/confidenceDistribution/" + i,
                        "confidence distribution labels must be non-blank and counts must be positive"));
            }
        }
    }
}

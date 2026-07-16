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
        for (int i = 0; i < plot.segments().size(); i++) {
            var segment = plot.segments().get(i);
            if (segment.eventType() == EventType.GAIN && (segment.copyNumber() == null || segment.copyNumber() < 3)) {
                errors.add(new ValidationError("GAIN_COPY_NUMBER_INVALID", "/segments/" + i + "/copyNumber",
                        "absolute gain copyNumber must be at least 3"));
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
    }
}

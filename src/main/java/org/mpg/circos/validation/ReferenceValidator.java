package org.mpg.circos.validation;

import org.mpg.circos.model.CircosPlot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ReferenceValidator {
    List<ValidationError> validate(CircosPlot plot) {
        List<ValidationError> errors = new ArrayList<>();
        checkUnique(plot.sourceResultIds(), "/sourceResultIds", "DUPLICATE_SOURCE_RESULT_ID", errors);
        Set<String> sourceIds = new HashSet<>(plot.sourceResultIds());
        Set<String> segmentIds = new HashSet<>();
        Set<String> linkIds = new HashSet<>();
        for (int i = 0; i < plot.segments().size(); i++) {
            var segment = plot.segments().get(i);
            if (!segmentIds.add(segment.id())) errors.add(new ValidationError("reference.segment.duplicate",
                    "/segments/" + i + "/id", "Segment ID is duplicated"));
            if (!sourceIds.contains(segment.sourceResultId())) errors.add(new ValidationError("reference.sourceResult.unresolved",
                    "/segments/" + i + "/sourceResultId", "sourceResultId is not declared at root"));
        }
        for (int i = 0; i < plot.links().size(); i++) {
            var link = plot.links().get(i);
            if (!linkIds.add(link.id())) errors.add(new ValidationError("DUPLICATE_LINK_ID",
                    "/links/" + i + "/id", "Link ID is duplicated"));
            if (link.sourceResultId() != null && !sourceIds.contains(link.sourceResultId())) {
                errors.add(new ValidationError("reference.sourceResult.unresolved",
                        "/links/" + i + "/sourceResultId", "sourceResultId is not declared at root"));
            }
        }
        return errors;
    }

    private void checkUnique(List<String> values, String path, String code, List<ValidationError> errors) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < values.size(); i++) {
            if (!seen.add(values.get(i))) errors.add(new ValidationError(code, path + "/" + i, "Value is duplicated"));
        }
    }
}

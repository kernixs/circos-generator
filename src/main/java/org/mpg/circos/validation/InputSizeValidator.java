package org.mpg.circos.validation;

import org.mpg.circos.model.CircosPlot;

import java.util.List;

public final class InputSizeValidator {
    public void validate(CircosPlot plot, int maximumEventCount) {
        long actual = (long) plot.segments().size() + plot.links().size();
        if (actual > maximumEventCount) {
            throw new ValidationException(List.of(new ValidationError(
                    "INPUT_SIZE_LIMIT_EXCEEDED",
                    "/",
                    "Plot contains " + actual + " events; configured maximum is " + maximumEventCount)));
        }
    }
}

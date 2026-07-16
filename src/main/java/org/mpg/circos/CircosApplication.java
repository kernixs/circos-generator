package org.mpg.circos;

import org.mpg.circos.model.CircosPlot;
import org.mpg.circos.validation.PlotInputReader;

import java.io.InputStream;

public final class CircosApplication {
    private final PlotInputReader inputReader;

    public CircosApplication() { this(new PlotInputReader()); }

    public CircosApplication(PlotInputReader inputReader) { this.inputReader = inputReader; }

    public CircosPlot validate(InputStream input) { return inputReader.read(input); }

    public CircosPlot readAndValidate(InputStream input) { return validate(input); }
}

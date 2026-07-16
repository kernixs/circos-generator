package org.mpg.circos;

import org.mpg.circos.model.CircosPlot;
import org.mpg.circos.assembly.AssemblyRepository;
import org.mpg.circos.assembly.ClasspathAssemblyRepository;
import org.mpg.circos.layout.CircularLayoutEngine;
import org.mpg.circos.renderer.SemanticSvgRenderer;
import org.mpg.circos.renderer.SvgDocument;
import org.mpg.circos.validation.DomainValidator;
import org.mpg.circos.validation.InputSizeValidator;
import org.mpg.circos.validation.PlotInputReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

public final class CircosApplication {
    private final PlotInputReader inputReader;
    private final DomainValidator domainValidator;
    private final AssemblyRepository assemblies;
    private final InputSizeValidator inputSizeValidator;
    private final CircularLayoutEngine layoutEngine;
    private final SemanticSvgRenderer renderer;

    public CircosApplication() {
        this(new PlotInputReader(), new ClasspathAssemblyRepository(), new InputSizeValidator(),
                new CircularLayoutEngine(), new SemanticSvgRenderer());
    }

    public CircosApplication(PlotInputReader inputReader) {
        this(inputReader, new ClasspathAssemblyRepository(), new InputSizeValidator(),
                new CircularLayoutEngine(), new SemanticSvgRenderer());
    }

    public CircosApplication(PlotInputReader inputReader, AssemblyRepository assemblies,
            InputSizeValidator inputSizeValidator, CircularLayoutEngine layoutEngine,
            SemanticSvgRenderer renderer) {
        this.inputReader = inputReader;
        this.assemblies = assemblies;
        this.domainValidator = new DomainValidator(assemblies);
        this.inputSizeValidator = inputSizeValidator;
        this.layoutEngine = layoutEngine;
        this.renderer = renderer;
    }

    public CircosPlot validate(InputStream input) { return inputReader.read(input); }

    public CircosPlot readAndValidate(InputStream input) { return validate(input); }

    public SvgDocument render(InputStream input) {
        return render(validate(input), RenderOptions.defaults());
    }

    public SvgDocument render(InputStream input, RenderOptions options) {
        return render(validate(input), options);
    }

    public SvgDocument render(CircosPlot plot) {
        return render(plot, RenderOptions.defaults());
    }

    public SvgDocument render(CircosPlot plot, RenderOptions options) {
        CircosPlot normalized = domainValidator.validateAndNormalize(plot);
        inputSizeValidator.validate(normalized, options.maximumEventCount());
        var assembly = assemblies.load(normalized.assemblyId());
        return renderer.render(normalized, layoutEngine.layout(normalized, assembly));
    }

    public void render(InputStream input, OutputStream output, RenderOptions options) throws IOException {
        render(input, options).writeTo(output);
    }

    public void render(InputStream input, Writer writer, RenderOptions options) throws IOException {
        render(input, options).writeTo(writer);
    }
}

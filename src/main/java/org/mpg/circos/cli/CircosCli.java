package org.mpg.circos.cli;

import org.mpg.circos.CircosApplication;
import org.mpg.circos.CircosGenerationException;
import org.mpg.circos.model.CircosPlot;
import org.mpg.circos.validation.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CircosCli {
    private CircosCli() {}

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, java.io.PrintStream out, java.io.PrintStream err) {
        Arguments parsed = Arguments.parse(args);
        if (parsed == null) {
            err.println("Usage: CircosCli --input <path|-> [--output <path|->]");
            return 2;
        }
        InputStream input = null;
        OutputStream output = null;
        boolean closeInput = false;
        boolean closeOutput = false;
        try {
            input = "-".equals(parsed.input()) ? System.in : Files.newInputStream(Path.of(parsed.input()));
            closeInput = input != System.in;
            CircosApplication application = new CircosApplication();
            if (parsed.output() == null) {
                CircosPlot plot = application.validate(input);
                out.println("Validated plotId=" + plot.plotId() + " mode=" + plot.mode().value()
                        + " assembly=" + plot.assemblyId());
            } else {
                output = "-".equals(parsed.output()) ? out : Files.newOutputStream(Path.of(parsed.output()));
                closeOutput = output != out;
                application.render(input).writeTo(output);
            }
            return 0;
        } catch (ValidationException e) {
            e.errors().forEach(error -> err.println(error.code() + " " + error.path() + ": " + error.message()));
            return 3;
        } catch (CircosGenerationException e) {
            err.println("Generation failure: " + e.getMessage());
            return 4;
        } catch (IOException e) {
            err.println("Input failure: " + e.getMessage());
            return 5;
        } finally {
            try {
                if (closeOutput && output != null) output.close();
                if (closeInput && input != null) input.close();
            } catch (IOException e) {
                err.println("Close failure: " + e.getMessage());
            }
        }
    }

    private record Arguments(String input, String output) {
        static Arguments parse(String[] args) {
            if (args.length != 2 && args.length != 4) return null;
            String input = null;
            String output = null;
            for (int i = 0; i < args.length; i += 2) {
                if (i + 1 >= args.length) return null;
                if ("--input".equals(args[i]) && input == null) input = args[i + 1];
                else if ("--output".equals(args[i]) && output == null) output = args[i + 1];
                else return null;
            }
            return input == null || input.isBlank() || output != null && output.isBlank()
                    ? null : new Arguments(input, output);
        }
    }
}

package org.mpg.circos.cli;

import org.mpg.circos.CircosApplication;
import org.mpg.circos.model.CircosPlot;
import org.mpg.circos.validation.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CircosCli {
    private CircosCli() {}

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, java.io.PrintStream out, java.io.PrintStream err) {
        if (args.length != 2 || !"--input".equals(args[0])) {
            err.println("Usage: CircosCli --input <path|->");
            return 2;
        }
        try (InputStream input = "-".equals(args[1]) ? System.in : Files.newInputStream(Path.of(args[1]))) {
            CircosPlot plot = new CircosApplication().validate(input);
            out.println("Validated plotId=" + plot.plotId() + " mode=" + plot.mode().value()
                    + " assembly=" + plot.assemblyId());
            return 0;
        } catch (ValidationException e) {
            e.errors().forEach(error -> err.println(error.code() + " " + error.path() + ": " + error.message()));
            return 3;
        } catch (IOException | RuntimeException e) {
            err.println("Input failure: " + e.getMessage());
            return 5;
        }
    }
}

package org.mpg.circos.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircosCliTest {
    @TempDir Path temporaryDirectory;

    @Test
    void validatesFixtureWithoutGeneratingSvg() {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        int status = CircosCli.run(new String[]{"--input", "src/test/resources/examples/empty-categories.json"},
                new PrintStream(out), new PrintStream(err));
        assertEquals(0, status);
        assertTrue(out.toString().contains("Validated plotId=synthetic-empty-1"));
        assertEquals("", err.toString());
    }

    @Test
    void writesSvgToExplicitOutputPath() throws Exception {
        Path output = temporaryDirectory.resolve("patient.svg");
        Files.writeString(output, "previous contents");
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        int status = CircosCli.run(new String[]{"--input", "src/test/resources/examples/patient-bcr-abl1.json",
                        "--output", output.toString()}, new PrintStream(out), new PrintStream(err));
        assertEquals(0, status);
        assertTrue(Files.readString(output).contains("class=\"circos-link-ribbon\""));
        assertEquals("", out.toString());
        assertEquals("", err.toString());
    }

    @Test
    void preservesExistingOutputWhenValidationFails() throws Exception {
        Path output = temporaryDirectory.resolve("existing.svg");
        Files.writeString(output, "previous valid SVG");
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int status = CircosCli.run(new String[]{"--input",
                        "src/test/resources/fixtures/invalid/gain-null-copy-number.json",
                        "--output", output.toString()}, new PrintStream(out), new PrintStream(err));

        assertEquals(3, status);
        assertEquals("previous valid SVG", Files.readString(output));
        assertEquals("", out.toString());
        assertTrue(err.toString().contains("GAIN_COPY_NUMBER_INVALID"));
    }

    @Test
    void writesSvgToStandardOutput() {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        int status = CircosCli.run(new String[]{"--output", "-", "--input",
                        "src/test/resources/examples/gains-and-losses.json"},
                new PrintStream(out), new PrintStream(err));
        assertEquals(0, status);
        assertTrue(out.toString().startsWith("<?xml"));
        assertEquals("", err.toString());
    }
}

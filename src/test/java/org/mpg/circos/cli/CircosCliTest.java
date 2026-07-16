package org.mpg.circos.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircosCliTest {
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
}

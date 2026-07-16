package org.mpg.circos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class CircosGenerationExceptionTest {
    @Test
    void preservesCause() {
        var cause = new IllegalStateException("cause");
        assertSame(cause, new CircosGenerationException("failure", cause).getCause());
    }
}

package org.mpg.circos;

public final class CircosGenerationException extends RuntimeException {
    public CircosGenerationException(String message) {
        super(message);
    }

    public CircosGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

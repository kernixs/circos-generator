package org.mpg.circos;

public record RenderOptions(int maximumEventCount) {
    public static final int DEFAULT_MAXIMUM_EVENT_COUNT = 20_000;

    public RenderOptions {
        if (maximumEventCount < 1) {
            throw new IllegalArgumentException("maximumEventCount must be positive");
        }
    }

    public static RenderOptions defaults() {
        return new RenderOptions(DEFAULT_MAXIMUM_EVENT_COUNT);
    }
}

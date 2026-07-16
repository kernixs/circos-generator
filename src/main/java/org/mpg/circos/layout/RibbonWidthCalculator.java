package org.mpg.circos.layout;

public final class RibbonWidthCalculator {
    public long halfWidthBases(int eventCount) {
        int bounded = Math.min(Math.max(eventCount, 1), 10);
        return 900_000L + bounded * 250_000L;
    }
}

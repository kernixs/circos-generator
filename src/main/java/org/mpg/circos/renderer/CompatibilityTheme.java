package org.mpg.circos.renderer;

import java.util.List;

public final class CompatibilityTheme implements RenderTheme {
    private static final List<String> CHROMOSOME_COLORS = List.of(
            "#ed1c24", "#a93b55", "#6a5f8f", "#2f83b7", "#3aa0a0", "#43a765",
            "#4caf45", "#6d846f", "#98659a", "#b24e8e", "#cf5f4d", "#f07818",
            "#ff8a00", "#ffaa00", "#ffe100", "#f4de1f", "#d9aa22", "#b96b26",
            "#c75c46", "#df6b87", "#ee74b0", "#bd86a8", "#8f8f8f", "#ed1c24");

    @Override public List<String> chromosomeColors() { return CHROMOSOME_COLORS; }
    @Override public String gainColor() { return "#d7191c"; }
    @Override public String lossColor() { return "#2c7bb6"; }
    @Override public String linkColor() { return "#71dfc0"; }
    @Override public String trackBackgroundColor() { return "#eeeeee"; }
    @Override public double eventOpacity() { return 0.88; }

    @Override
    public double linkFillOpacity(int eventCount) {
        int bounded = Math.min(Math.max(eventCount, 1), 10);
        return Math.min(0.95, 0.55 + 0.04 * bounded);
    }

    @Override
    public double linkBorderOpacity(int eventCount) {
        return Math.min(0.98, linkFillOpacity(eventCount) + 0.08);
    }
}

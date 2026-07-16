package org.mpg.circos.layout;

public record LayoutParameters(
        double viewBoxSize,
        double chromosomeInnerRadius,
        double chromosomeOuterRadius,
        double chromosomeLabelRadius,
        double backgroundTrackWidth,
        double backgroundTrackGap,
        double gainInnerRadius,
        double gainOuterRadius,
        double lossInnerRadius,
        double lossOuterRadius) {

    public static LayoutParameters compatibilityDefaults() {
        return new LayoutParameters(684.0, 294.0, 310.0, 323.0,
                10.0, 8.0, 218.0, 234.0, 194.0, 210.0);
    }

    public double center() {
        return viewBoxSize / 2.0;
    }
}

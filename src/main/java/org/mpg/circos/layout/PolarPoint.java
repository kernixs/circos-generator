package org.mpg.circos.layout;

public record PolarPoint(double x, double y) {
    public static PolarPoint from(double centerX, double centerY, double radius, double angle) {
        return new PolarPoint(centerX + radius * Math.cos(angle), centerY + radius * Math.sin(angle));
    }
}

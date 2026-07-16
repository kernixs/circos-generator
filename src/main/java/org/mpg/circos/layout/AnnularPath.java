package org.mpg.circos.layout;

public record AnnularPath(double innerRadius, double outerRadius, double startAngle, double endAngle) {
    public AnnularPath {
        if (!(innerRadius >= 0.0 && outerRadius > innerRadius && endAngle > startAngle)) {
            throw new IllegalArgumentException("Invalid annular geometry");
        }
    }
}

package org.mpg.circos.layout;

public final class AngleMapper {
    public double mapBoundary(SectorGeometry sector, long boundary) {
        if (boundary < 0 || boundary > sector.chromosomeLength()) {
            throw new IllegalArgumentException("Boundary is outside chromosome");
        }
        double fraction = (double) boundary / sector.chromosomeLength();
        return sector.startAngle() + (sector.endAngle() - sector.startAngle()) * fraction;
    }
}

package org.mpg.circos.layout;

public final class AngleMapper {
    public double mapBoundary(SectorGeometry sector, long boundary) {
        return mapBoundary(sector, (double) boundary);
    }

    public double mapBoundary(SectorGeometry sector, double boundary) {
        if (!Double.isFinite(boundary) || boundary < 0 || boundary > sector.chromosomeLength()) {
            throw new IllegalArgumentException("Boundary is outside chromosome");
        }
        double fraction = boundary / sector.chromosomeLength();
        return sector.startAngle() + (sector.endAngle() - sector.startAngle()) * fraction;
    }
}

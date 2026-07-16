package org.mpg.circos.layout;

import java.util.List;

public record TrackLayout(
        double chromosomeInnerRadius,
        double chromosomeOuterRadius,
        double chromosomeLabelRadius,
        List<Ring> backgroundTracks,
        Ring gainTrack,
        Ring lossTrack,
        double linkRadius) {

    public TrackLayout {
        backgroundTracks = List.copyOf(backgroundTracks);
    }

    public record Ring(double innerRadius, double outerRadius) {}
}

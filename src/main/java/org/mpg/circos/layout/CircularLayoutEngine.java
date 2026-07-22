package org.mpg.circos.layout;

import org.mpg.circos.CircosGenerationException;
import org.mpg.circos.assembly.Chromosome;
import org.mpg.circos.assembly.GenomeAssembly;
import org.mpg.circos.model.CircosPlot;
import org.mpg.circos.model.EventType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CircularLayoutEngine {
    private static final double ONE_DEGREE = Math.PI / 180.0;
    private final LayoutParameters parameters;
    private final AngleMapper angleMapper;
    private final RibbonWidthCalculator ribbonWidths;

    public CircularLayoutEngine() {
        this(LayoutParameters.compatibilityDefaults(), new AngleMapper(), new RibbonWidthCalculator());
    }

    public CircularLayoutEngine(LayoutParameters parameters, AngleMapper angleMapper,
                                RibbonWidthCalculator ribbonWidths) {
        this.parameters = parameters;
        this.angleMapper = angleMapper;
        this.ribbonWidths = ribbonWidths;
    }

    public PlotGeometry layout(CircosPlot plot, GenomeAssembly assembly) {
        List<SectorGeometry> sectors = sectors(assembly);
        Map<String, SectorGeometry> byChromosome = new LinkedHashMap<>();
        sectors.forEach(sector -> byChromosome.put(sector.chromosome(), sector));
        TrackLayout tracks = tracks(plot);
        List<PlotGeometry.SegmentGeometry> segments = segmentGeometry(plot, byChromosome, tracks);
        List<PlotGeometry.LinkGeometry> links = linkGeometry(plot, byChromosome, assembly, tracks);
        return new PlotGeometry(parameters.viewBoxSize(), parameters.center(), parameters.center(),
                sectors, tracks, segments, links);
    }

    private List<SectorGeometry> sectors(GenomeAssembly assembly) {
        long totalBases = assembly.chromosomes().stream().mapToLong(Chromosome::length).sum();
        double usable = 2.0 * Math.PI - 28.0 * ONE_DEGREE;
        double cursor = -Math.PI / 2.0;
        List<SectorGeometry> result = new ArrayList<>();
        for (Chromosome chromosome : assembly.chromosomes()) {
            double span = usable * chromosome.length() / totalBases;
            result.add(new SectorGeometry(chromosome.name(), chromosome.length(), cursor, cursor + span));
            cursor += span + ("Y".equals(chromosome.name()) ? 5.0 : 1.0) * ONE_DEGREE;
        }
        return result;
    }

    private TrackLayout tracks(CircosPlot plot) {
        List<TrackLayout.Ring> backgrounds = new ArrayList<>();
        double outer = parameters.chromosomeInnerRadius() - 8.0;
        for (int i = 0; i < 3; i++) {
            backgrounds.add(new TrackLayout.Ring(outer - parameters.backgroundTrackWidth(), outer));
            outer -= parameters.backgroundTrackWidth() + parameters.backgroundTrackGap();
        }
        boolean gains = plot.segments().stream().anyMatch(s -> s.eventType() == EventType.GAIN);
        boolean losses = plot.segments().stream().anyMatch(s -> s.eventType() == EventType.LOSS);
        TrackLayout.Ring gain = gains
                ? new TrackLayout.Ring(parameters.gainInnerRadius(), parameters.gainOuterRadius()) : null;
        TrackLayout.Ring loss = losses
                ? new TrackLayout.Ring(parameters.lossInnerRadius(), parameters.lossOuterRadius()) : null;
        double linkRadius = losses ? parameters.lossInnerRadius() - 8.0
                : gains ? parameters.gainInnerRadius() - 8.0 : outer - 8.0;
        return new TrackLayout(parameters.chromosomeInnerRadius(), parameters.chromosomeOuterRadius(),
                parameters.chromosomeLabelRadius(), backgrounds, gain, loss, linkRadius);
    }

    private List<PlotGeometry.SegmentGeometry> segmentGeometry(CircosPlot plot,
            Map<String, SectorGeometry> sectors, TrackLayout tracks) {
        List<PlotGeometry.SegmentGeometry> result = new ArrayList<>();
        for (var segment : plot.segments()) {
            SectorGeometry sector = requiredSector(sectors, segment.interval().chromosome());
            double start = angleMapper.mapBoundary(sector, segment.interval().start());
            double end = angleMapper.mapBoundary(sector, segment.interval().end());
            TrackLayout.Ring ring = switch (segment.eventType()) {
                case GAIN -> tracks.gainTrack();
                case LOSS -> tracks.lossTrack();
                case TRANSLOCATION -> throw new CircosGenerationException(
                        "Unsupported segment event type: " + segment.eventType().value());
            };
            double outer = ring.outerRadius();
            if (segment.eventType() == EventType.GAIN) {
                if (segment.copyNumber() == null) {
                    outer = ring.innerRadius() + 0.75;
                } else {
                    double value = Math.min(5.8, Math.max(3.2, segment.copyNumber()));
                    outer = ring.innerRadius() + (ring.outerRadius() - ring.innerRadius()) * ((value - 3.0) / 2.8);
                    outer = Math.max(ring.innerRadius() + 0.75, outer);
                }
            }
            AnnularPath interval = new AnnularPath(ring.innerRadius(), outer, start, end);
            double markerRadius = segment.eventType() == EventType.GAIN
                    ? ring.outerRadius() - 1.5 : ring.innerRadius() + 0.72 * (ring.outerRadius() - ring.innerRadius());
            PolarPoint marker = PolarPoint.from(parameters.center(), parameters.center(), markerRadius,
                    start + (end - start) / 2.0);
            result.add(new PlotGeometry.SegmentGeometry(segment.id(), interval, marker));
        }
        return result;
    }

    private List<PlotGeometry.LinkGeometry> linkGeometry(CircosPlot plot,
            Map<String, SectorGeometry> sectors, GenomeAssembly assembly, TrackLayout tracks) {
        Map<String, Long> lengths = new LinkedHashMap<>();
        assembly.chromosomes().forEach(chromosome -> lengths.put(chromosome.name(), chromosome.length()));
        List<PlotGeometry.LinkGeometry> result = new ArrayList<>();
        for (var link : plot.links()) {
            int count = link.aggregate() == null ? 1 : link.aggregate().eventCount();
            SectorGeometry sourceSector = requiredSector(sectors, link.source().chromosome());
            SectorGeometry targetSector = requiredSector(sectors, link.target().chromosome());
            org.mpg.circos.model.GenomicInterval sourceInterval = displayInterval(link.source(),
                    lengths.get(link.source().chromosome()), count);
            org.mpg.circos.model.GenomicInterval targetInterval = displayInterval(link.target(),
                    lengths.get(link.target().chromosome()), count);
            long sourceStart = sourceInterval.start();
            long sourceEnd = sourceInterval.end();
            long targetStart = targetInterval.start();
            long targetEnd = targetInterval.end();
            double sourceAngle = angleMapper.mapBoundary(sourceSector, link.source().midpoint());
            double targetAngle = angleMapper.mapBoundary(targetSector, link.target().midpoint());
            RibbonGeometry ribbon = new RibbonGeometry(tracks.linkRadius(),
                    angleMapper.mapBoundary(sourceSector, sourceStart),
                    angleMapper.mapBoundary(sourceSector, sourceEnd),
                    angleMapper.mapBoundary(targetSector, targetStart),
                    angleMapper.mapBoundary(targetSector, targetEnd),
                    PolarPoint.from(parameters.center(), parameters.center(), tracks.linkRadius(), sourceAngle),
                    PolarPoint.from(parameters.center(), parameters.center(), tracks.linkRadius(), targetAngle));
            result.add(new PlotGeometry.LinkGeometry(link.id(), ribbon, count));
        }
        return result;
    }

    private org.mpg.circos.model.GenomicInterval displayInterval(
            org.mpg.circos.model.LinkEndpoint endpoint, long chromosomeLength, int eventCount) {
        if (!endpoint.isLegacyPoint()) return endpoint.interval();
        long halfWidth = ribbonWidths.halfWidthBases(eventCount);
        long position = endpoint.legacyPosition();
        return new org.mpg.circos.model.GenomicInterval(endpoint.chromosome(),
                Math.max(0L, position - halfWidth), Math.min(chromosomeLength, position + halfWidth));
    }

    private SectorGeometry requiredSector(Map<String, SectorGeometry> sectors, String chromosome) {
        SectorGeometry sector = sectors.get(chromosome);
        if (sector == null) throw new CircosGenerationException("Missing layout sector for chromosome " + chromosome);
        return sector;
    }
}

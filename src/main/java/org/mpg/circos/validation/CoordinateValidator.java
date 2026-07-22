package org.mpg.circos.validation;

import org.mpg.circos.assembly.Chromosome;
import org.mpg.circos.assembly.ChromosomeNormalizer;
import org.mpg.circos.assembly.GenomeAssembly;
import org.mpg.circos.model.CircosPlot;
import org.mpg.circos.model.GenomicInterval;
import org.mpg.circos.model.GenomicLink;
import org.mpg.circos.model.GenomicSegment;
import org.mpg.circos.model.LinkEndpoint;

import java.util.ArrayList;
import java.util.List;

final class CoordinateValidator {
    private final ChromosomeNormalizer normalizer = new ChromosomeNormalizer();

    List<ValidationError> validate(CircosPlot plot, GenomeAssembly assembly) {
        List<ValidationError> errors = new ArrayList<>();
        for (int i = 0; i < plot.segments().size(); i++) {
            GenomicInterval interval = plot.segments().get(i).interval();
            validateInterval(interval, "/segments/" + i + "/interval", assembly, errors);
        }
        for (int i = 0; i < plot.links().size(); i++) {
            GenomicLink link = plot.links().get(i);
            validateEndpoint(link.source(), "/links/" + i + "/source", plot.schemaVersion(), assembly, errors);
            validateEndpoint(link.target(), "/links/" + i + "/target", plot.schemaVersion(), assembly, errors);
        }
        return errors;
    }

    CircosPlot normalize(CircosPlot plot, GenomeAssembly assembly) {
        List<GenomicSegment> segments = plot.segments().stream()
                .map(s -> new GenomicSegment(s.id(), s.sourceResultId(), s.eventGroupId(),
                        new GenomicInterval(normalize(s.interval().chromosome(), assembly),
                                s.interval().start(), s.interval().end()), s.eventType(),
                        s.copyNumber(), s.confidence(), s.label(), s.displayType(), s.annotations(), s.aggregate()))
                .toList();
        List<GenomicLink> links = plot.links().stream()
                .map(l -> new GenomicLink(l.id(), l.eventGroupId(), normalize(l.source(), assembly),
                        normalize(l.target(), assembly), l.sourceResultId(), l.eventType(),
                        l.confidence(), l.aggregate(), l.label(), l.annotations()))
                .toList();
        return new CircosPlot(plot.schemaVersion(), plot.plotId(), plot.label(), plot.mode(),
                assembly.id().canonical(), plot.coordinateConvention(), plot.sourceResultIds(), segments, links);
    }

    private LinkEndpoint normalize(LinkEndpoint endpoint, GenomeAssembly assembly) {
        GenomicInterval interval = endpoint.interval();
        return new LinkEndpoint(endpoint.segmentId(),
                new GenomicInterval(normalize(interval.chromosome(), assembly), interval.start(), interval.end()),
                endpoint.legacyPosition());
    }

    private String normalize(String chromosome, GenomeAssembly assembly) {
        try { return normalizer.normalize(chromosome, assembly); }
        catch (IllegalArgumentException e) { return chromosome; }
    }

    private void validateInterval(GenomicInterval interval, String path, GenomeAssembly assembly,
                                  List<ValidationError> errors) {
        Chromosome chromosome = chromosome(interval.chromosome(), path + "/chromosome", assembly, errors);
        if (chromosome == null) return;
        if (interval.start() < 0) errors.add(new ValidationError("NEGATIVE_COORDINATE", path + "/start", "start must be >= 0"));
        if (interval.end() <= interval.start()) errors.add(new ValidationError("coordinate.interval.order", path,
                "end must be greater than start"));
        if (interval.end() > chromosome.length()) errors.add(new ValidationError("coordinate.interval.bounds", path + "/end",
                "end exceeds chromosome length " + chromosome.length()));
    }

    private void validateEndpoint(LinkEndpoint endpoint, String path,
            org.mpg.circos.model.SchemaVersion version, GenomeAssembly assembly,
            List<ValidationError> errors) {
        if (version == org.mpg.circos.model.SchemaVersion.V1_0) {
            if (!endpoint.isLegacyPoint()) {
                errors.add(new ValidationError("LINK_ENDPOINT_VERSION_INVALID", path,
                        "Version 1 links require compatibility point endpoints"));
                return;
            }
            validatePoint(endpoint, path, assembly, errors);
            return;
        }
        if (endpoint.isLegacyPoint()) {
            errors.add(new ValidationError("LINK_ENDPOINT_VERSION_INVALID", path,
                    "Version 2 links require genomic interval endpoints"));
            return;
        }
        validateInterval(endpoint.interval(), path + "/interval", assembly, errors);
    }

    private void validatePoint(LinkEndpoint endpoint, String path, GenomeAssembly assembly,
                               List<ValidationError> errors) {
        Chromosome chromosome = chromosome(endpoint.chromosome(), path + "/chromosome", assembly, errors);
        if (chromosome == null) return;
        long position = endpoint.legacyPosition();
        if (position < 0 || position >= chromosome.length()) {
            errors.add(new ValidationError("coordinate.point.bounds", path + "/position",
                    "position must be in [0," + chromosome.length() + ")"));
        }
    }

    private Chromosome chromosome(String value, String path, GenomeAssembly assembly,
                                  List<ValidationError> errors) {
        try {
            String canonical = normalizer.normalize(value, assembly);
            return assembly.chromosomes().stream().filter(c -> c.name().equals(canonical)).findFirst().orElseThrow();
        } catch (IllegalArgumentException | java.util.NoSuchElementException e) {
            errors.add(new ValidationError("coordinate.chromosome.unsupported", path, "Unsupported chromosome: " + value));
            return null;
        }
    }
}

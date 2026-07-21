package org.mpg.circos.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mpg.circos.assembly.ClasspathAssemblyRepository;
import org.mpg.circos.model.CircosPlot;

import java.io.IOException;
import java.io.InputStream;

public final class PlotInputReader {
    private final ObjectMapper mapper;
    private final SchemaValidator schemaValidator;
    private final DomainValidator domainValidator;

    public PlotInputReader() {
        this(new ObjectMapper(), new SchemaValidator(), new DomainValidator(new ClasspathAssemblyRepository()));
    }

    public PlotInputReader(ObjectMapper mapper, SchemaValidator schemaValidator, DomainValidator domainValidator) {
        this.mapper = mapper;
        this.schemaValidator = schemaValidator;
        this.domainValidator = domainValidator;
    }

    public CircosPlot read(InputStream input) {
        JsonNode root;
        try {
            root = mapper.readTree(input);
        } catch (IOException | RuntimeException e) {
            throw new ValidationException(java.util.List.of(new ValidationError("JSON_PARSE_ERROR", "/", e.getMessage())));
        }
        if (root == null || !root.isObject()) {
            throw new ValidationException(java.util.List.of(new ValidationError("ROOT_NOT_OBJECT", "/", "root must be an object")));
        }
        var schemaErrors = schemaValidator.validate(root);
        if (!schemaErrors.isEmpty()) throw new ValidationException(schemaErrors);
        return domainValidator.validateAndNormalize(toModel(root));
    }

    private CircosPlot toModel(JsonNode root) {
        var segments = new java.util.ArrayList<org.mpg.circos.model.GenomicSegment>();
        root.withArray("segments").forEach(node -> segments.add(new org.mpg.circos.model.GenomicSegment(
                node.get("id").asText(), node.get("sourceResultId").asText(), textOrNull(node, "eventGroupId"),
                new org.mpg.circos.model.GenomicInterval(node.get("interval").get("chromosome").asText(),
                        node.get("interval").get("start").asLong(), node.get("interval").get("end").asLong()),
                org.mpg.circos.model.EventType.valueOf(node.get("eventType").asText().toUpperCase()),
                node.get("copyNumber").isNull() ? null : node.get("copyNumber").asInt(),
                textOrNull(node, "confidence"), textOrNull(node, "label"), displayType(node),
                segmentAnnotations(node.get("annotations")), aggregate(node.get("aggregate")))));
        var version = org.mpg.circos.model.SchemaVersion.fromValue(root.get("schemaVersion").asText());
        var links = new java.util.ArrayList<org.mpg.circos.model.GenomicLink>();
        root.withArray("links").forEach(node -> links.add(new org.mpg.circos.model.GenomicLink(
                node.get("id").asText(), textOrNull(node, "eventGroupId"), endpoint(node.get("source"), version),
                endpoint(node.get("target"), version), textOrNull(node, "sourceResultId"),
                org.mpg.circos.model.EventType.valueOf(node.get("eventType").asText().toUpperCase()),
                textOrNull(node, "confidence"), aggregate(node.get("aggregate")), textOrNull(node, "label"),
                linkAnnotations(node.get("annotations")))));
        var sourceIds = new java.util.ArrayList<String>();
        root.withArray("sourceResultIds").forEach(node -> sourceIds.add(node.asText()));
        return new CircosPlot(version, root.get("plotId").asText(),
                textOrNull(root, "label"), org.mpg.circos.model.PlotMode.valueOf(root.get("mode").asText().toUpperCase()),
                root.get("assemblyId").asText(), coordinateConvention(root),
                sourceIds, segments, links);
    }

    private org.mpg.circos.model.CoordinateConvention coordinateConvention(JsonNode root) {
        JsonNode value = root.get("coordinateConvention");
        return value == null || value.isNull()
                ? org.mpg.circos.model.CoordinateConvention.ZERO_BASED_HALF_OPEN
                : org.mpg.circos.model.CoordinateConvention.valueOf(value.asText());
    }

    public CircosPlot readAndValidate(InputStream input) { return read(input); }

    private org.mpg.circos.model.LinkEndpoint endpoint(JsonNode node,
            org.mpg.circos.model.SchemaVersion version) {
        if (version == org.mpg.circos.model.SchemaVersion.V1_0) {
            return org.mpg.circos.model.LinkEndpoint.fromLegacyPoint(node.get("segmentId").asText(),
                    node.get("chromosome").asText(), node.get("position").asLong());
        }
        JsonNode interval = node.get("interval");
        return new org.mpg.circos.model.LinkEndpoint(node.get("segmentId").asText(),
                new org.mpg.circos.model.GenomicInterval(interval.get("chromosome").asText(),
                        interval.get("start").asLong(), interval.get("end").asLong()));
    }

    private org.mpg.circos.model.CohortAggregate aggregate(JsonNode node) {
        if (node == null || node.isNull()) return null;
        var confidenceDistribution = new java.util.ArrayList<org.mpg.circos.model.ConfidenceCount>();
        JsonNode distribution = node.get("confidenceDistribution");
        if (distribution != null) distribution.forEach(value -> confidenceDistribution.add(
                new org.mpg.circos.model.ConfidenceCount(value.get("label").asText(), value.get("count").asInt())));
        return new org.mpg.circos.model.CohortAggregate(node.get("eventCount").asInt(),
                node.get("patientCount").asInt(), node.get("sampleCount").asInt(),
                textOrNull(node, "groupingDescription"), confidenceDistribution);
    }

    private org.mpg.circos.model.SegmentDisplayType displayType(JsonNode node) {
        String value = textOrNull(node, "displayType");
        return value == null ? null : org.mpg.circos.model.SegmentDisplayType.valueOf(value.toUpperCase());
    }

    private org.mpg.circos.model.SegmentAnnotationMetadata segmentAnnotations(JsonNode node) {
        if (node == null || node.isNull()) return null;
        return new org.mpg.circos.model.SegmentAnnotationMetadata(textList(node, "genes"),
                textList(node, "methods"), metadata(node, "additionalMetadata"));
    }

    private org.mpg.circos.model.LinkAnnotationMetadata linkAnnotations(JsonNode node) {
        if (node == null || node.isNull()) return null;
        return new org.mpg.circos.model.LinkAnnotationMetadata(textList(node, "sourceGenes"),
                textList(node, "targetGenes"), textList(node, "methods"),
                metadata(node, "additionalMetadata"));
    }

    private java.util.List<String> textList(JsonNode node, String field) {
        var values = new java.util.ArrayList<String>();
        JsonNode array = node.get(field);
        if (array != null) array.forEach(value -> values.add(value.asText()));
        return values;
    }

    private java.util.Map<String, String> metadata(JsonNode node, String field) {
        var values = new java.util.TreeMap<String, String>();
        JsonNode object = node.get(field);
        if (object != null) object.fields().forEachRemaining(entry -> values.put(entry.getKey(), entry.getValue().asText()));
        return values;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}

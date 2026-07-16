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
                textOrNull(node, "confidence"), textOrNull(node, "label"))));
        var links = new java.util.ArrayList<org.mpg.circos.model.GenomicLink>();
        root.withArray("links").forEach(node -> links.add(new org.mpg.circos.model.GenomicLink(
                node.get("id").asText(), textOrNull(node, "eventGroupId"), endpoint(node.get("source")),
                endpoint(node.get("target")), textOrNull(node, "sourceResultId"),
                org.mpg.circos.model.EventType.valueOf(node.get("eventType").asText().toUpperCase()),
                textOrNull(node, "confidence"), aggregate(node.get("aggregate")), textOrNull(node, "label"))));
        var sourceIds = new java.util.ArrayList<String>();
        root.withArray("sourceResultIds").forEach(node -> sourceIds.add(node.asText()));
        return new CircosPlot(org.mpg.circos.model.SchemaVersion.V1_0, root.get("plotId").asText(),
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

    private org.mpg.circos.model.LinkEndpoint endpoint(JsonNode node) {
        return new org.mpg.circos.model.LinkEndpoint(node.get("segmentId").asText(),
                node.get("chromosome").asText(), node.get("position").asLong());
    }

    private org.mpg.circos.model.CohortAggregate aggregate(JsonNode node) {
        if (node == null || node.isNull()) return null;
        return new org.mpg.circos.model.CohortAggregate(node.get("eventCount").asInt(),
                node.get("patientCount").asInt(), node.get("sampleCount").asInt());
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}

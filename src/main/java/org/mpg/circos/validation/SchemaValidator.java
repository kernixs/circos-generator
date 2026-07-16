package org.mpg.circos.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

public final class SchemaValidator {
    private final ObjectMapper mapper;

    public SchemaValidator() { this(new ObjectMapper()); }

    public SchemaValidator(ObjectMapper mapper) { this.mapper = mapper; }

    public List<ValidationError> validate(JsonNode root) {
        String version = root != null && root.path("schemaVersion").isTextual()
                ? root.path("schemaVersion").textValue() : null;
        String resource = SupportedSchemaVersions.schemaResource(version);
        if (resource == null) {
            return List.of(new ValidationError("UNSUPPORTED_SCHEMA_VERSION", "/schemaVersion",
                    "Unsupported schema version: " + version));
        }
        try (InputStream input = SchemaValidator.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing schema resource: " + resource);
            JsonNode schemaNode = mapper.readTree(input);
            JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(schemaNode);
            return schema.validate(root).stream()
                    .map(this::toError)
                    .sorted(ValidationError.ORDER)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load schema: " + resource, e);
        }
    }

    private ValidationError toError(ValidationMessage message) {
        String path = message.getInstanceLocation() == null
                || message.getInstanceLocation().toString().isBlank()
                ? "/" : message.getInstanceLocation().toString();
        String code = message.getCode() == null ? "SCHEMA_VALIDATION" : message.getCode();
        return new ValidationError(code, path, message.getMessage());
    }
}

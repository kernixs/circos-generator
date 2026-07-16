package org.mpg.circos.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaValidatorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final SchemaValidator validator = new SchemaValidator(mapper);

    @Test
    void acceptsSchemaValidPatientPayload() throws Exception {
        var node = mapper.readTree(getClass().getResourceAsStream("/examples/patient-bcr-abl1.json"));
        assertTrue(validator.validate(node).isEmpty());
    }

    @Test
    void reportsUnsupportedVersion() throws Exception {
        var node = mapper.readTree(getClass().getResourceAsStream("/fixtures/invalid/unsupported-schema-version.json"));
        assertFalse(validator.validate(node).isEmpty());
        assertTrue(validator.validate(node).stream().anyMatch(e -> e.code().equals("UNSUPPORTED_SCHEMA_VERSION")));
    }

    @Test
    void rejectsUnknownProperties() throws Exception {
        var node = mapper.readTree(getClass().getResourceAsStream("/fixtures/invalid/extra-root-property.json"));
        assertFalse(validator.validate(node).isEmpty());
    }
}

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
    void acceptsVersionTwoIntervalEndpoints() throws Exception {
        var node = mapper.readTree(getClass().getResourceAsStream("/examples/v2-interval-links.json"));
        assertTrue(validator.validate(node).isEmpty());
    }

    @Test
    void versionTwoRequiresExplicitCoordinateConvention() throws Exception {
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(
                getClass().getResourceAsStream("/examples/v2-interval-links.json"));
        node.remove("coordinateConvention");
        assertFalse(validator.validate(node).isEmpty());
    }

    @Test
    void versionTwoSchemaRejectsNegativeIntervalCoordinates() throws Exception {
        var node = mapper.readTree(getClass().getResourceAsStream("/examples/v2-interval-links.json"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) node.at("/links/0/source/interval"))
                .put("start", -1);
        assertFalse(validator.validate(node).isEmpty());

        node = mapper.readTree(getClass().getResourceAsStream("/examples/v2-interval-links.json"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) node.at("/links/0/source/interval"))
                .put("end", -1);
        assertFalse(validator.validate(node).isEmpty());
    }

    @Test
    void versionTwoRejectsLegacyPointEndpoints() throws Exception {
        var node = mapper.readTree(getClass().getResourceAsStream("/examples/v2-interval-links.json"));
        var source = (com.fasterxml.jackson.databind.node.ObjectNode) node.at("/links/0/source");
        source.remove("interval");
        source.put("chromosome", "9");
        source.put("position", 133599999);
        assertFalse(validator.validate(node).isEmpty());
    }

    @Test
    void versionOneRejectsIntervalEndpoints() throws Exception {
        var node = mapper.readTree(getClass().getResourceAsStream("/examples/crossing-links.json"));
        var source = (com.fasterxml.jackson.databind.node.ObjectNode) node.at("/links/0/source");
        source.remove("chromosome");
        source.remove("position");
        source.set("interval", mapper.readTree("{\"chromosome\":\"9\",\"start\":1,\"end\":2}"));
        assertFalse(validator.validate(node).isEmpty());
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

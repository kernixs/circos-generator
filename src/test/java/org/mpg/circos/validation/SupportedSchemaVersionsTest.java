package org.mpg.circos.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SupportedSchemaVersionsTest {
    @Test
    void dispatchesSupportedVersions() {
        assertNotNull(SupportedSchemaVersions.schemaResource("1.0"));
        assertNotNull(SupportedSchemaVersions.schemaResource("2.0"));
        assertNull(SupportedSchemaVersions.schemaResource("3.0"));
    }
}

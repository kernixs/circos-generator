package org.mpg.circos.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SupportedSchemaVersionsTest {
    @Test
    void dispatchesOnlyVersionOne() {
        assertNotNull(SupportedSchemaVersions.schemaResource("1.0"));
        assertNull(SupportedSchemaVersions.schemaResource("2.0"));
    }
}

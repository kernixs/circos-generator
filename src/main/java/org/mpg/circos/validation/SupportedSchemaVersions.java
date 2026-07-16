package org.mpg.circos.validation;

import java.util.Map;

public final class SupportedSchemaVersions {
    private static final Map<String, String> SCHEMAS = Map.of(
            "1.0", "/schema/circos-plot-1.0.schema.json");

    private SupportedSchemaVersions() {}

    public static String schemaResource(String version) {
        return SCHEMAS.get(version);
    }
}

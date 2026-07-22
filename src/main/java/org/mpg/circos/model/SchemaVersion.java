package org.mpg.circos.model;

public enum SchemaVersion {
    V1_0("1.0"), V2_0("2.0");

    private final String value;

    SchemaVersion(String value) { this.value = value; }

    public String value() { return value; }

    public static SchemaVersion fromValue(String value) {
        for (SchemaVersion version : values()) {
            if (version.value.equals(value)) return version;
        }
        throw new IllegalArgumentException("Unsupported schema version: " + value);
    }
}

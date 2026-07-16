package org.mpg.circos.model;

public enum SchemaVersion {
    V1_0("1.0");

    private final String value;

    SchemaVersion(String value) { this.value = value; }

    public String value() { return value; }
}

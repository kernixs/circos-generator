package org.mpg.circos.model;

public enum CoordinateConvention {
    ZERO_BASED_HALF_OPEN("ZERO_BASED_HALF_OPEN");

    private final String value;

    CoordinateConvention(String value) { this.value = value; }

    public String value() { return value; }
}

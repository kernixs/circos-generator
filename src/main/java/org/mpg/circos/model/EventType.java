package org.mpg.circos.model;

public enum EventType {
    GAIN("gain"), LOSS("loss"), TRANSLOCATION("translocation");

    private final String value;

    EventType(String value) { this.value = value; }

    public String value() { return value; }
}

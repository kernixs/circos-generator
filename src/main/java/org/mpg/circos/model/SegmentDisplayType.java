package org.mpg.circos.model;

public enum SegmentDisplayType {
    GAIN("gain", EventType.GAIN),
    DUPLICATION("duplication", EventType.GAIN),
    AMPLIFICATION("amplification", EventType.GAIN),
    LOSS("loss", EventType.LOSS),
    DELETION("deletion", EventType.LOSS);

    private final String value;
    private final EventType geometryType;

    SegmentDisplayType(String value, EventType geometryType) {
        this.value = value;
        this.geometryType = geometryType;
    }

    public String value() {
        return value;
    }

    public EventType geometryType() {
        return geometryType;
    }
}

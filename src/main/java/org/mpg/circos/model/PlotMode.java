package org.mpg.circos.model;

public enum PlotMode {
    PATIENT("patient"), COHORT("cohort");

    private final String value;

    PlotMode(String value) { this.value = value; }

    public String value() { return value; }
}

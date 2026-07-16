package org.mpg.circos.layout;

public record RibbonGeometry(
        double radius,
        double sourceStartAngle,
        double sourceEndAngle,
        double targetStartAngle,
        double targetEndAngle,
        PolarPoint sourceEndpoint,
        PolarPoint targetEndpoint) {}

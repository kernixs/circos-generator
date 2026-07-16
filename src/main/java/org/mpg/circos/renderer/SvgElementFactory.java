package org.mpg.circos.renderer;

import org.mpg.circos.layout.AnnularPath;
import org.mpg.circos.layout.PolarPoint;
import org.mpg.circos.layout.RibbonGeometry;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SvgElementFactory {
    public String annularPath(double centerX, double centerY, AnnularPath path) {
        PolarPoint outerStart = PolarPoint.from(centerX, centerY, path.outerRadius(), path.startAngle());
        PolarPoint outerEnd = PolarPoint.from(centerX, centerY, path.outerRadius(), path.endAngle());
        PolarPoint innerEnd = PolarPoint.from(centerX, centerY, path.innerRadius(), path.endAngle());
        PolarPoint innerStart = PolarPoint.from(centerX, centerY, path.innerRadius(), path.startAngle());
        int largeArc = path.endAngle() - path.startAngle() > Math.PI ? 1 : 0;
        return "M " + point(outerStart) + " A " + format(path.outerRadius()) + " "
                + format(path.outerRadius()) + " 0 " + largeArc + " 1 " + point(outerEnd)
                + " L " + point(innerEnd) + " A " + format(path.innerRadius()) + " "
                + format(path.innerRadius()) + " 0 " + largeArc + " 0 " + point(innerStart) + " Z";
    }

    public String ribbonPath(double centerX, double centerY, RibbonGeometry ribbon) {
        PolarPoint sourceStart = PolarPoint.from(centerX, centerY, ribbon.radius(), ribbon.sourceStartAngle());
        PolarPoint sourceEnd = PolarPoint.from(centerX, centerY, ribbon.radius(), ribbon.sourceEndAngle());
        PolarPoint targetStart = PolarPoint.from(centerX, centerY, ribbon.radius(), ribbon.targetStartAngle());
        PolarPoint targetEnd = PolarPoint.from(centerX, centerY, ribbon.radius(), ribbon.targetEndAngle());
        double controlRadius = ribbon.radius() * 0.35;
        PolarPoint sourceControlStart = PolarPoint.from(centerX, centerY, controlRadius, ribbon.sourceStartAngle());
        PolarPoint sourceControlEnd = PolarPoint.from(centerX, centerY, controlRadius, ribbon.sourceEndAngle());
        PolarPoint targetControlStart = PolarPoint.from(centerX, centerY, controlRadius, ribbon.targetStartAngle());
        PolarPoint targetControlEnd = PolarPoint.from(centerX, centerY, controlRadius, ribbon.targetEndAngle());
        return "M " + point(sourceStart) + " C " + point(sourceControlStart) + " "
                + point(targetControlStart) + " " + point(targetStart) + " L " + point(targetEnd)
                + " C " + point(targetControlEnd) + " " + point(sourceControlEnd) + " "
                + point(sourceEnd) + " Z";
    }

    public String arcPath(double centerX, double centerY, double radius, double startAngle, double endAngle) {
        PolarPoint start = PolarPoint.from(centerX, centerY, radius, startAngle);
        PolarPoint end = PolarPoint.from(centerX, centerY, radius, endAngle);
        int largeArc = endAngle - startAngle > Math.PI ? 1 : 0;
        return "M " + point(start) + " A " + format(radius) + " " + format(radius)
                + " 0 " + largeArc + " 1 " + point(end);
    }

    public String point(PolarPoint point) {
        return format(point.x()) + " " + format(point.y());
    }

    public String format(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("SVG coordinate must be finite");
        BigDecimal decimal = BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
        return decimal.compareTo(BigDecimal.ZERO) == 0 ? "0" : decimal.toPlainString();
    }
}

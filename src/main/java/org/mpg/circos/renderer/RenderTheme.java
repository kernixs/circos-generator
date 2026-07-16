package org.mpg.circos.renderer;

import java.util.List;

public interface RenderTheme {
    List<String> chromosomeColors();
    String gainColor();
    String lossColor();
    String linkColor();
    String trackBackgroundColor();
    double eventOpacity();
    double linkFillOpacity(int eventCount);
    double linkBorderOpacity(int eventCount);
}

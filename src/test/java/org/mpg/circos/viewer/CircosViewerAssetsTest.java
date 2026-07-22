package org.mpg.circos.viewer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircosViewerAssetsTest {
    @Test
    void exposesPackagedViewerJavascript() {
        String javascript = CircosViewerAssets.javascript();

        assertTrue(javascript.contains("global.CircosViewer"));
        assertTrue(javascript.contains("circos-selection-change"));
        assertEquals(javascript, CircosViewerAssets.javascript());
    }
}

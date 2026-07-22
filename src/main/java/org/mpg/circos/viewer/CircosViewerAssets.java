package org.mpg.circos.viewer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Provides the dependency-free browser viewer packaged with the renderer artifact. */
public final class CircosViewerAssets {
    private static final String JAVASCRIPT_RESOURCE = "circos-viewer.js";

    private CircosViewerAssets() {
    }

    public static String javascript() {
        try (InputStream input = CircosViewerAssets.class.getResourceAsStream(JAVASCRIPT_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing packaged Circos viewer JavaScript");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read packaged Circos viewer JavaScript", e);
        }
    }
}

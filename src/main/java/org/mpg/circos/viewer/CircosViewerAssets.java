package org.mpg.circos.viewer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Provides the dependency-free browser viewer packaged with the renderer artifact. */
public final class CircosViewerAssets {
    private static final String JAVASCRIPT_RESOURCE = "circos-viewer.js";
    private static final String STYLESHEET_RESOURCE = "circos-viewer.css";

    private CircosViewerAssets() {
    }

    public static String javascript() {
        return read(JAVASCRIPT_RESOURCE, "JavaScript");
    }

    public static String stylesheet() {
        return read(STYLESHEET_RESOURCE, "stylesheet");
    }

    private static String read(String resource, String description) {
        try (InputStream input = CircosViewerAssets.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing packaged Circos viewer " + description);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read packaged Circos viewer " + description, e);
        }
    }
}

package org.mpg.circos;

import java.io.InputStream;

public final class TestFixtures {
    private TestFixtures() {}

    public static InputStream open(String path) {
        InputStream input = TestFixtures.class.getResourceAsStream(path);
        if (input == null) throw new IllegalArgumentException("Missing test fixture: " + path);
        return input;
    }
}


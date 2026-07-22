package org.mpg.circos.renderer;

import org.junit.jupiter.api.Test;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.TestFixtures;

import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeterministicSvgTest {
    @Test
    void versionOneOutputIsByteIdenticalAcrossLocaleAndTimeZone() {
        assertDeterministic("/examples/crossing-links.json");
    }

    @Test
    void versionTwoOutputIsByteIdenticalAcrossLocaleAndTimeZone() {
        assertDeterministic("/examples/v2-interval-links.json");
    }

    private void assertDeterministic(String fixture) {
        Locale originalLocale = Locale.getDefault();
        TimeZone originalZone = TimeZone.getDefault();
        try {
            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
            String first = new CircosApplication()
                    .render(TestFixtures.open(fixture)).xml();
            Locale.setDefault(Locale.GERMANY);
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
            String second = new CircosApplication()
                    .render(TestFixtures.open(fixture)).xml();
            assertEquals(first, second);
        } finally {
            Locale.setDefault(originalLocale);
            TimeZone.setDefault(originalZone);
        }
    }
}

package org.mpg.circos.renderer;

import org.junit.jupiter.api.Test;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.TestFixtures;

import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeterministicSvgTest {
    @Test
    void outputIsByteIdenticalAcrossLocaleAndTimeZone() {
        Locale originalLocale = Locale.getDefault();
        TimeZone originalZone = TimeZone.getDefault();
        try {
            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
            String first = new CircosApplication()
                    .render(TestFixtures.open("/examples/crossing-links.json")).xml();
            Locale.setDefault(Locale.GERMANY);
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
            String second = new CircosApplication()
                    .render(TestFixtures.open("/examples/crossing-links.json")).xml();
            assertEquals(first, second);
        } finally {
            Locale.setDefault(originalLocale);
            TimeZone.setDefault(originalZone);
        }
    }
}

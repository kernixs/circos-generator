package org.mpg.circos.validation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mpg.circos.CircosApplication;
import org.mpg.circos.TestFixtures;

class GoldenJsonFixturesTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void everyExampleRemainsValid(String name) throws Exception {
        assertNotNull(new CircosApplication().readAndValidate(TestFixtures.open("/examples/" + name)));
    }

    static Stream<String> examples() {
        return Stream.of("patient-bcr-abl1.json", "gains-and-losses.json", "crossing-links.json",
                "cohort-aggregate.json", "empty-categories.json", "chromosome-aliases.json");
    }
}


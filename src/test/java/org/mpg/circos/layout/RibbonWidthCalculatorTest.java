package org.mpg.circos.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RibbonWidthCalculatorTest {
    @Test
    void clampsWidthCountBetweenOneAndTen() {
        var calculator = new RibbonWidthCalculator();
        assertEquals(1_150_000L, calculator.halfWidthBases(0));
        assertEquals(1_150_000L, calculator.halfWidthBases(1));
        assertEquals(3_400_000L, calculator.halfWidthBases(10));
        assertEquals(3_400_000L, calculator.halfWidthBases(100));
    }
}

package org.mpg.circos.renderer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SvgIdEncoderTest {
    @Test
    void usesFirstNinetySixBitsOfSha256() {
        assertEquals("ba7816bf8f01cfea414140de", new SvgIdEncoder().encode("abc"));
    }
}

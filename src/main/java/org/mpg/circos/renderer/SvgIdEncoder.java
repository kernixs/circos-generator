package org.mpg.circos.renderer;

import org.mpg.circos.CircosGenerationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class SvgIdEncoder {
    public String encode(String externalId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(externalId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 24);
        } catch (NoSuchAlgorithmException e) {
            throw new CircosGenerationException("SHA-256 is unavailable", e);
        }
    }
}

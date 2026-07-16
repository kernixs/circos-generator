package org.mpg.circos.renderer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public record SvgDocument(String xml) {
    public SvgDocument {
        Objects.requireNonNull(xml, "xml");
    }

    public void writeTo(OutputStream output) throws IOException {
        output.write(xml.getBytes(StandardCharsets.UTF_8));
    }

    public void writeTo(Writer writer) throws IOException {
        writer.write(xml);
    }

    @Override public String toString() { return xml; }
}

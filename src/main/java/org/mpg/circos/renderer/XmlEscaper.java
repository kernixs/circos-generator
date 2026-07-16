package org.mpg.circos.renderer;

public final class XmlEscaper {
    private XmlEscaper() {}

    public static String text(String value) {
        if (value == null) return "";
        StringBuilder escaped = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> append(escaped, codePoint, false));
        return escaped.toString();
    }

    public static String attribute(String value) {
        if (value == null) return "";
        StringBuilder escaped = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> append(escaped, codePoint, true));
        return escaped.toString();
    }

    private static void append(StringBuilder out, int codePoint, boolean attribute) {
        switch (codePoint) {
            case '&' -> out.append("&amp;");
            case '<' -> out.append("&lt;");
            case '>' -> out.append("&gt;");
            case '"' -> out.append(attribute ? "&quot;" : "\"");
            case '\'' -> out.append(attribute ? "&apos;" : "'");
            default -> {
                if (isAllowedXml(codePoint)) out.appendCodePoint(codePoint);
                else out.append("&#xfffd;");
            }
        }
    }

    private static boolean isAllowedXml(int codePoint) {
        return codePoint == 0x9 || codePoint == 0xa || codePoint == 0xd
                || codePoint >= 0x20 && codePoint <= 0xd7ff
                || codePoint >= 0xe000 && codePoint <= 0xfffd
                || codePoint >= 0x10000 && codePoint <= 0x10ffff;
    }
}

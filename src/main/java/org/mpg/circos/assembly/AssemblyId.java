package org.mpg.circos.assembly;

import java.util.Locale;

public enum AssemblyId {
    GRCH37("GRCh37", "hg19"),
    GRCH38("GRCh38", "hg38");

    private final String canonical;
    private final String alias;

    AssemblyId(String canonical, String alias) {
        this.canonical = canonical;
        this.alias = alias;
    }

    public String canonical() { return canonical; }

    public static AssemblyId from(String value) {
        if (value == null) throw new IllegalArgumentException("assemblyId is required");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (AssemblyId id : values()) {
            if (id.canonical.toLowerCase(Locale.ROOT).equals(normalized)
                    || id.alias.equals(normalized)) return id;
        }
        throw new IllegalArgumentException("Unsupported assembly: " + value);
    }
}

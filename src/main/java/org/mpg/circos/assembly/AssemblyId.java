package org.mpg.circos.assembly;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public enum AssemblyId {
    GRCH37("GRCh37", "hg19"),
    GRCH38("GRCh38", "hg38"),
    T2T_CHM13("T2T-CHM13", "T2T", "CHM13", "CHM13v2", "CHM13v2.0", "hs1");

    private final String canonical;
    private final Set<String> aliases;

    AssemblyId(String canonical, String... aliases) {
        this.canonical = canonical;
        this.aliases = Arrays.stream(aliases)
                .map(alias -> alias.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public String canonical() { return canonical; }

    public static AssemblyId from(String value) {
        if (value == null) throw new IllegalArgumentException("assemblyId is required");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (AssemblyId id : values()) {
            if (id.canonical.toLowerCase(Locale.ROOT).equals(normalized)
                    || id.aliases.contains(normalized)) return id;
        }
        throw new IllegalArgumentException("Unsupported assembly: " + value);
    }
}

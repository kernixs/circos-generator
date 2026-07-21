# Circos Generator

This repository provides a reusable Java 21 library that renders complete,
semantic Circos plots as SVG, plus a standalone browser viewer that enhances the
same SVG with interaction.

Version 2.0 is the preferred input and SVG contract. It adds interval-aware
translocation endpoints while retaining strict Version 1 point inputs through a
compatibility adapter. Both versions use the Java 21 model, validation,
deterministic layout and semantic SVG renderer, CLI harness, and dependency-free
viewer. The authoritative specification is
[docs/technical-design.md](docs/technical-design.md).

The caller decides which biological records should be visualized and constructs
renderer objects. The renderer supports exactly three visualization categories:
`GAIN` and `LOSS` interval track items, and `TRANSLOCATION` links connecting two
affected genomic intervals. Link attachment markers use interval midpoints by
default as approximate display geometry; they are not confirmed or exact
breakpoints. Database access, classification of source-system aliases, cohort
selection, and application integration remain outside this repository.

Run the validation suite with:

```text
./mvnw test
```

Generate deterministic SVG with the database-independent CLI:

```text
./mvnw -q exec:java \
  -Dexec.mainClass=org.mpg.circos.cli.CircosCli \
  -Dexec.args="--input src/test/resources/examples/v2-interval-links.json --output examples/generated/crossing-links.svg"
```

Use `--output -` to write SVG to standard output. The CLI only reads the
versioned plot contract; it performs no database queries, cohort aggregation,
contributor resolution, or GUI/viewer behavior.

Run the inline viewer examples with:

```text
python3 -m http.server 8000
```

Then open `http://localhost:8000/viewer/`. See [viewer/README.md](viewer/README.md)
for the standalone viewer API and selection callback contract.

The public Java API and neutral host boundary are documented in
[docs/host-integration-contract.md](docs/host-integration-contract.md). This is
an integration contract only; it does not select or implement JDBC, REST,
FileMaker, MPG, or GUI-specific adapters.

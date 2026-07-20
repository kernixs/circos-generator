# Circos Generator

This repository will provide a reusable Java 21 library that renders complete,
semantic Circos plots as SVG, plus a standalone browser viewer that enhances the
same SVG with interaction.

The current baseline contains the Java 21 model, validation, deterministic
layout and semantic SVG renderer, CLI harness, and a dependency-free initial
interactive viewer. Database and application integrations remain outside this
repository. The authoritative specification is
[docs/technical-design.md](docs/technical-design.md).

Run the validation suite with:

```text
./mvnw test
```

Generate deterministic SVG with the database-independent CLI:

```text
./mvnw -q exec:java \
  -Dexec.mainClass=org.mpg.circos.cli.CircosCli \
  -Dexec.args="--input src/test/resources/examples/gains-and-losses.json --output examples/generated/patient-gains-losses.svg"
```

Use `--output -` to write SVG to standard output. The CLI only reads the
versioned plot contract; it performs no database queries, cohort aggregation,
contributor resolution, or GUI/viewer behavior.

Run the three inline viewer examples with:

```text
python3 -m http.server 8000
```

Then open `http://localhost:8000/viewer/`. See [viewer/README.md](viewer/README.md)
for the standalone viewer API and selection callback contract.

The public Java API and neutral host boundary are documented in
[docs/host-integration-contract.md](docs/host-integration-contract.md). This is
an integration contract only; it does not select or implement JDBC, REST,
FileMaker, MPG, or GUI-specific adapters.

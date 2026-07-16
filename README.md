# Circos Generator

This repository will provide a reusable Java 21 library that renders complete,
semantic Circos plots as SVG, plus a standalone browser viewer that enhances the
same SVG with interaction.

Phase 1 now contains the Java 21 model, JSON Schema 1.0, assembly resources,
JSON/domain validation, fixtures, tests, and validation-only CLI. Geometry, SVG,
browser interaction, and integrations remain deferred. The authoritative
specification is [docs/technical-design.md](docs/technical-design.md).

Run the validation suite with:

```text
./mvnw test
```

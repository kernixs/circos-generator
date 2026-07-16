# Technical design: Java semantic Circos renderer

Status: **Proposed for approval** · Contract version: **JSON Schema 1.0** ·
Target runtime: **Java 21** · Build system: **Maven**

This document is the authoritative, standalone specification and implementation
gate for the new Circos component. A developer must be able to implement and
test the repository using this document and repository-owned fixtures/resources
alone; access to the MPG repository, its audit, database, R template, or stored
outputs is not required. This specification records all normative legacy-parity
behavior retained by V1 while establishing safer coordinates, explicit
missing-value behavior, and stable semantic SVG. It does not authorize
implementation.

## 1. Scope and design principles

The repository will contain one rendering implementation. Java will parse and
validate versioned JSON, calculate the circular layout, and emit the complete
SVG. The browser viewer will only attach behavior to that SVG; it will not
recalculate chromosomes, angles, tracks, endpoints, ribbons, or paths.

The rendering core will have no dependency on HTML, JavaScript, HTTP, JDBC,
Swing, JavaFX, H2, PostgreSQL, patient databases, or the MPG application. It
will accept plain domain objects and return an SVG document/string or write it
to a caller-provided `OutputStream`/`Writer`.

Visual compatibility targets are:

- GRCh37/hg19 and GRCh38/hg38; chromosomes 1-22, X, and Y;
- proportional, clockwise sectors starting at 90 degrees;
- one-degree inter-sector gaps and the specified larger closing gap;
- the chromosome colors and labels specified in section 6;
- three gray background tracks and conditional gain/loss tracks;
- the copy-number markers, legend, and SVG styling specified in section 6;
- mint translocation ribbons using the specified width, opacity, and curvature
  calculations; and
- patient inputs and already-aggregated cohort inputs.

The compatibility constants and formulas recorded in section 6 are normative.
They will be represented as named, immutable rendering parameters and locked by
deterministic structural and golden SVG tests maintained in this repository.
Invalid coordinates, semantic identity, missing copy number, escaping, and
explicit cohort aggregation intentionally supersede legacy behavior. If a
future external reference conflicts with this document, this document controls
until it is revised and approved.

Out of scope for the first implementation are databases and adapters, JDBC,
HTTP/REST, Docker, GUI frameworks, patient data, cohort ranking/selection,
reimplementation of `circlize`, and MPG callbacks beyond emitting opaque IDs.
In particular, the renderer will not reproduce the legacy cohort comparator
bug, the 50/100-row link limiting policy, or the GUI's current-page cohort scope.
Those are producer-selection concerns. The renderer validates and draws exactly
the already-selected records it receives in deterministic input order.

## 2. Architecture and dependency direction

The architecture is a directed pipeline with inward dependencies on stable
domain types:

```text
fixture/path -> CLI -> CircosApplication -> parsing/schema validation -> model
                                               |                         |
assembly resources -> assembly lookup ----------+----------- domain validation
                                                                      |
                                                                      v
                                                             geometry -> renderer
                                                                      |
                                                                      v
                                          CLI output path <- semantic SVG
                                                                      |
                                                                      v
                                                               viewer enhancement
```

No package may depend on `viewer/`. In production code, dependencies are:

```text
model <- assembly
model <- validation -> assembly
model <- geometry -> assembly
model <- renderer -> geometry, assembly
renderer, validation, model <- CircosApplication <- cli
```

More specifically:

- **Model** contains immutable, database-independent input records and enums.
  It carries meaning, not render coordinates or DOM concerns. It has no
  dependency on other project packages.
- **Validation** performs supported-version checks and cross-field/domain
  checks after JSON Schema validation. It depends on model and assembly, but
  never repairs invalid input silently.
- **Assembly resources** load immutable chromosome names, aliases, order, and
  lengths from classpath JSON. They normalize accepted input aliases to
  canonical names and reject unknown/ambiguous names.
- **Geometry** transforms validated genomic coordinates into deterministic
  sector, track, marker, and ribbon geometry. It does not serialize XML or know
  about browser interaction.
- **Renderer** turns geometry plus semantic model data into an SVG scene and
  serializes safe XML. It owns visual styles and semantic SVG attributes, but
  not domain validation or layout calculations.
- **Viewer** is static HTML/CSS/JavaScript. It loads or embeds Java-produced SVG
  and attaches listeners/selectors to its semantic contract. It does not import
  or reproduce Java geometry.
- **Examples** are synthetic JSON fixtures and generated demonstration SVGs.
  They are not production resources and contain no patient information.
- **Tests** cover each boundary independently, then exercise the full pipeline
  with deterministic structural fixtures and, after structural parity is
  established, golden SVG snapshots.

JSON parsing and JSON Schema evaluation live at the application boundary, not
inside the domain model. A thin public facade coordinates them so library
callers do not need to assemble the pipeline manually.

The development CLI is an outer adapter in the same Maven module. It depends on
the public `CircosApplication` facade and standard stream/path APIs only. The
facade, model, validation, assembly, geometry, and renderer packages have no
dependency on `cli`; CLI argument handling and process exit codes cannot leak
into the rendering core.

## 3. Proposed repository structure

Base Java package: `org.mpg.circos`.

```text
pom.xml
README.md
docs/
  technical-design.md
  json-contract.md
  semantic-svg-contract.md
  compatibility-notes.md
src/test/resources/examples/
  patient-bcr-abl1.json
  gains-and-losses.json
  crossing-links.json
  cohort-aggregate.json
  empty-categories.json
viewer/
  index.html
  viewer.css
  viewer.js
  README.md
src/main/java/org/mpg/circos/
  CircosApplication.java
  cli/
    CircosCli.java
  model/
    CircosPlot.java
    SchemaVersion.java
    PlotMode.java
    GenomicSegment.java
    GenomicLink.java
    LinkEndpoint.java
    CohortAggregate.java
    EventType.java
    CoordinateConvention.java
    GenomicInterval.java
  validation/
    PlotInputReader.java
    SchemaValidator.java
    SupportedSchemaVersions.java
    AssemblyValidator.java
    CoordinateValidator.java
    ReferenceValidator.java
    BusinessRulesValidator.java
    DomainValidator.java
    ValidationError.java
    ValidationException.java
  assembly/
    GenomeAssembly.java
    Chromosome.java
    AssemblyId.java
    AssemblyRepository.java
    ClasspathAssemblyRepository.java
    ChromosomeNormalizer.java
  geometry/
    CircularLayoutEngine.java
    LayoutParameters.java
    PlotGeometry.java
    SectorGeometry.java
    AnnularPath.java
    RibbonGeometry.java
    PolarPoint.java
    AngleMapper.java
    TrackLayout.java
    RibbonStyleCalculator.java
  renderer/
    SemanticSvgRenderer.java
    SvgDocument.java
    SvgElementFactory.java
    SvgIdEncoder.java
    XmlEscaper.java
    RenderTheme.java
    CompatibilityTheme.java
  layout/
    package-info.java
src/main/resources/
  genomes/
    grch37.chromosomes.json
    grch38.chromosomes.json
  schema/
    circos-plot-1.0.schema.json
src/test/java/org/mpg/circos/
  CircosApplicationTest.java
  cli/
    CircosCliTest.java
  validation/
    SchemaValidatorTest.java
    SupportedSchemaVersionsTest.java
    PlotInputReaderTest.java
    AssemblyValidatorTest.java
    CoordinateValidatorTest.java
    ReferenceValidatorTest.java
    BusinessRulesValidatorTest.java
    DomainValidatorTest.java
    DeterministicValidationErrorsTest.java
    GoldenJsonFixturesTest.java
  assembly/
    ClasspathAssemblyRepositoryTest.java
    ChromosomeNormalizerTest.java
src/test/resources/
  fixtures/invalid/
```

This is one Maven module with one root `pom.xml`, one production JAR, and one
test lifecycle. No aggregator POM, child modules, or Maven multi-module build
will be introduced initially. `viewer/`, `examples/`, and `docs/` are
repository assets, not Maven modules. Phase 1 synthetic fixtures live under
`src/test/resources/examples`; the top-level `examples/` directory is reserved
for later demonstration assets.

`CircosApplication` is the neutral Phase 1 application facade. It accepts an
`InputStream`, performs parsing/schema/domain validation, and returns a validated
`CircosPlot`. SVG output methods are deferred until the renderer phase. The
Maven artifact will be a plain reusable JAR. The viewer remains outside the JAR
and can be served by any static file server during development.

`CircosCli` is the database-independent development harness. Its Phase 1
invocation is:

```text
./mvnw exec:java \
  -Dexec.mainClass=org.mpg.circos.cli.CircosCli \
  -Dexec.args="--input src/test/resources/examples/patient-bcr-abl1.json"
```

It reads one JSON payload from the input path (or `-` for standard input), calls
only the public facade, and validates the payload without writing SVG. It emits
diagnostics to standard error and uses stable Phase 1 process codes: 0 success,
2 command usage, 3 schema/domain validation (including version or assembly
rejection), and 5 input/output failure. Geometry/rendering code and its future
exit code are deferred. It performs no SVG construction, database access,
network access, aggregation, or viewer enhancement.

## 4. Versioned JSON contract (1.0)

The canonical schema resource will use JSON Schema Draft 2020-12 and be
`src/main/resources/schema/circos-plot-1.0.schema.json`; `$id` will identify the
contract independently of a deployment URL. The root uses
`"schemaVersion": "1.0"`, rejects unknown properties by default, and uses
opaque strings for all external identifiers. IDs must be non-empty, have a
maximum length of 256 Unicode code points, exclude control characters, and are
never parsed as database keys. Labels are capped at 512 code points and
confidence categories at 64. `sourceResultIds` has at least one entry.

### 4.1 Classification labels

Every input field belongs to exactly one classification:

- **P** — required for current visual parity
- **I** — preserved for interaction
- **A** — cohort aggregation metadata
- **C** — optional future clone data

“Required” below means required by JSON Schema. A nullable value is still
present and explicit. Fields omitted from a conditional branch are forbidden,
not ignored.

### 4.2 Root object

| Field | Type | Required | Class | Meaning |
|---|---|---:|:---:|---|
| `schemaVersion` | string, const `1.0` | yes | I | Contract dispatch/evolution key |
| `plotId` | opaque string | yes | I | Stable external plot identifier |
| `label` | string | no | I | Optional display/export metadata; escaped but not visibly drawn in V1 |
| `mode` | `patient` or `cohort` | yes | P | Selects link aggregation rules |
| `assemblyId` | `GRCh37`, `hg19`, `GRCh38`, or `hg38` | yes | P | Genome assembly lookup key; normalized to GRCh ID |
| `coordinateConvention` | `ZERO_BASED_HALF_OPEN` | no | P | Optional wire value; omitted input defaults to the Phase 1 enum value |
| `sourceResultIds` | unique opaque string array | yes | I | Source result external IDs represented by the plot |
| `segments` | array of segment objects | yes | P | May be empty |
| `links` | array of link objects | yes | P | May be empty |

`plotId` and segment/link IDs must be unique in their applicable namespace; root
source result IDs must also be unique. Event group IDs are deliberately
repeatable. Source-result references must resolve within the same payload. Link
endpoint segment IDs are preserved external references and need not occur in the
plotted `segments` array. Empty arrays are valid so empty categories and empty
plots are safe.

Clone fields are deliberately absent from the Phase 1 JSON Schema and model.
They will be introduced only with a later approved contract revision.

### 4.3 Segment object

| Field | Type | Required | Class | Meaning |
|---|---|---:|:---:|---|
| `id` | opaque string | yes | I | Segment external ID |
| `sourceResultId` | opaque string | yes | I | Owning source result; must appear at root |
| `eventGroupId` | opaque string or null | yes | I | Groups related visual events when supplied |
| `interval` | genomic interval | yes | P | Canonicalized during validation |
| `eventType` | `gain` or `loss` | yes | P | Track/category behavior; producer normalizes legacy aliases |
| `copyNumber` | integer or null | yes | P | Explicit unknown is `null`; never coerced to zero |
| `confidence` | string or null | yes | I | Source category such as `HIGH`; explicit unknown is `null` |
| `label` | string | no | I | Tooltip/display metadata |

V1 defines copy number as absolute integer copy number. For `gain`,
`copyNumber` must be non-null and at least 3. Values below 3 are rejected with a
domain-validation error; the renderer never reclassifies them or silently
reverses radial bounds. For `loss`, copy number may be null because its visual
appearance does not use the value. Unknown copy number never implies loss or
numeric zero. A future producer using relative/log-ratio copy number requires a
new documented contract convention rather than weakening this rule. Confidence
is a bounded, display-safe
opaque category rather than an invented numeric score; V1 preserves values such
as `HIGH` and `MEDIUM` without assigning them an ordering.

### 4.4 Genomic interval

| Field | Type | Required | Class | Meaning |
|---|---|---:|:---:|---|
| `chromosome` | string | yes | P | Accepts documented aliases; stored/rendered canonically |
| `start` | integer >= 0 | yes | P | Zero-based inclusive start |
| `end` | integer > 0 | yes | P | Zero-based exclusive end |

The reusable interval is embedded as `interval` in a plotted segment. These are
the gain/loss records used for segment tracks; a link endpoint's source segment
does not have to appear here because the current pipeline does not plot its
`TRANS` breakpoint segments as CNV tracks.

### 4.5 Link object

| Field | Type | Required | Class | Meaning |
|---|---|---:|:---:|---|
| `id` | opaque string | yes | I | Link external ID; a cohort uses a new stable aggregate ID, never a representative raw-link ID |
| `eventGroupId` | opaque string or null | yes | I | Groups link with related events when supplied |
| `source` | link endpoint object | yes | P | Source endpoint container |
| `target` | link endpoint object | yes | P | Target endpoint container |
| `sourceResultId` | opaque string | conditional | I | Required in patient mode; forbidden for cohort aggregates |
| `eventType` | `translocation` | yes | P | Version 1.0 link type |
| `confidence` | string or null | yes | I | Explicit unknown is `null`; never overloaded with `COHORT` |
| `aggregate` | cohort aggregate object | conditional | A | Required in cohort mode; forbidden in patient mode |
| `label` | string | no | I | Tooltip/display metadata |

Each link endpoint has this shape:

| Field | Type | Required | Class | Meaning |
|---|---|---:|:---:|---|
| `segmentId` | opaque string | yes | I | Identity of the source/target segment |
| `chromosome` | string | yes | P | Endpoint sector |
| `position` | integer >= 0 | yes | P | Zero-based point coordinate |

Source and target may be on the same chromosome. Endpoint coordinates are
explicit because the V1 compatibility behavior uses the source/target segment
start only, not the whole interval. The producer must ensure that an endpoint ID
and point describe the same source record. V1 intentionally avoids requiring
otherwise unplotted breakpoint segments in `segments` or duplicating their
unused ends.

### 4.6 Cohort aggregate object

| Field | Type | Required | Class | Meaning |
|---|---|---:|:---:|---|
| `eventCount` | integer >= 1 | yes | A | Number of aggregated events |
| `patientCount` | integer >= 1 | yes | A | Number of distinct patients represented |
| `sampleCount` | integer >= 1 | yes | A | Number of distinct samples represented |

Counts are explicit, not encoded as magic IDs, sentinel values, widths, or
missing fields. Given the V1 count definitions, domain validation enforces
`patientCount <= sampleCount <= eventCount`. The renderer consumes
already-aggregated values and does no cohort grouping, ranking, selection, or
link limiting.

### 4.7 Representative payload shape

```json
{
  "schemaVersion": "1.0",
  "plotId": "demo-1",
  "label": "Synthetic BCR::ABL1 example",
  "mode": "patient",
  "assemblyId": "GRCh38",
  "sourceResultIds": ["result-alpha"],
  "segments": [
    {
      "id": "seg-9",
      "sourceResultId": "result-alpha",
      "eventGroupId": "gain-group-1",
      "interval": {"chromosome": "18", "start": 37912422, "end": 39587423},
      "eventType": "gain",
      "copyNumber": 3,
      "confidence": "HIGH"
    },
    {
      "id": "seg-22",
      "sourceResultId": "result-alpha",
      "eventGroupId": "loss-group-1",
      "interval": {"chromosome": "7", "start": 16014078, "end": 18239079},
      "eventType": "loss",
      "copyNumber": 1,
      "confidence": "MEDIUM"
    }
  ],
  "links": [
    {
      "id": "link-1",
      "eventGroupId": "event-1",
      "source": {"segmentId": "breakpoint-9", "chromosome": "9", "position": 133729449},
      "target": {"segmentId": "breakpoint-22", "chromosome": "22", "position": 23372909},
      "sourceResultId": "result-alpha",
      "eventType": "translocation",
      "confidence": "HIGH"
    }
  ]
}
```

The example is synthetic and demonstrates shape only. Its coordinates use the
chosen zero-based contract; a future MPG adapter must establish its actual
source convention rather than infer it from examples.

## 5. Coordinate convention and assembly validation

`CoordinateConvention.ZERO_BASED_HALF_OPEN` is the only supported Phase 1
convention. It uses **zero-based, half-open** coordinates: `[start, end)`. This is
unambiguous, composes cleanly with lengths, and represents a full chromosome as
`[0, chromosomeLength)`.

For every interval or link point, validation will:

1. normalize the chromosome alias (`chr1` and `1` become canonical `1`, `chrX`
   becomes `X`) using the selected assembly;
2. reject chromosomes outside canonical human chromosomes 1-22, X, and Y;
3. require `0 <= start < end <= chromosomeLength` for intervals and
   `0 <= position < chromosomeLength` for points;
4. reject negative, empty, reversed, non-integral, and out-of-bounds intervals;
5. report a structured error containing the JSON path, rejected value, assembly,
   chromosome, and allowed range.

Angle mapping uses boundaries, not inclusive base positions. For a chromosome
sector beginning at angle `a` with angular span `s`, boundary coordinate `p`
maps to `a + direction * s * (p / chromosomeLength)`, where `p` is in
`[0, chromosomeLength]` and `direction` encodes clockwise layout. Consequently,
chromosome start, midpoint, and end map exactly to the sector's start, midpoint,
and end boundaries. SVG's coordinate system and the human-readable angle
convention will be isolated in `AngleMapper` so the clockwise/90-degree behavior
is testable without sign ambiguity.

The future MPG adapter must explicitly convert its source convention. If the
source is one-based closed `[start1, end1]`, it must emit
`start0 = start1 - 1` and `end0 = end1`; a one-based point `position1` becomes
`position0 = position1 - 1`. It must not adjust coordinates that are already
zero-based half-open/zero-based points. The adapter must establish the MPG
source convention explicitly, reject ambiguity, and apply assembly-specific
bounds before serialization. The renderer will independently validate and will
never guess or clamp input coordinates.

## 6. Geometry and compatibility behavior

`CircularLayoutEngine` will operate only after successful validation. It will
compute immutable `PlotGeometry` in canonical chromosome order. Chromosome
angular spans are proportional to assembly lengths after subtracting 28 degrees
of gaps from 360 degrees: one degree after each of chromosomes 1-22 and X, and
five degrees after Y. The first chromosome boundary is at 90 degrees and sectors
advance clockwise.

The compatibility canvas has a square `684 684` view box, corresponding to a
9.5-inch 72-points-per-inch canvas, a white background, and a base point size of
10. `CompatibilityTheme` records zero cell padding and a
radial margin equivalent to `0.002` above and below every track. Its 24 sector
colors, in chromosome order, are:

```text
#ed1c24 #a93b55 #6a5f8f #2f83b7 #3aa0a0 #43a765
#4caf45 #6d846f #98659a #b24e8e #cf5f4d #f07818
#ff8a00 #ffaa00 #ffe100 #f4de1f #d9aa22 #b96b26
#c75c46 #df6b87 #ee74b0 #bd86a8 #8f8f8f #ed1c24
```

The outer chromosome track height is `0.055`, with solid sector rectangles,
white borders equivalent to line width 2, and black labels at normalized
`y=1.55`, bending inside with nice-facing behavior and size 1.0. Java geometry
must reproduce the label orientation explicitly.

Three background tracks always render. Each has normalized y-range `[0,1]`,
height `0.085`, fill `#eeeeee`, a white border, and a white midline at `y=0.5`
with line width 1.8.

The gain track exists only when gain segments exist. It has y-range `[0,6]`,
height `0.095`, gray background, and white border. Domain validation guarantees
absolute integer copy number `v >= 3`. Its red `#d7191c` interval rectangle
uses bottom `y=3.2` and top `y=max(3.2,min(5.8,v))`; this explicit V1 floor
prevents reversed geometry for `v=3`. Its filled-circle marker is at
`y=min(5.7,v+1.2)`, size 0.35. Null and values below 3 fail domain validation.

The loss track exists only when loss segments exist. It has y-range `[0,3]`,
height `0.095`, gray background, and white border. Its blue `#2c7bb6` interval
rectangle spans y `0.15` to `1.05`, and its filled-circle marker is at `y=1.75`,
size 0.35. Loss copy number does not affect geometry, so an explicit null is
safe. By specification, conditional gain/loss tracks change the radial
anchor available to subsequent ribbons.

Segment annular geometry is calculated from normalized interval boundary
angles. Zero-area paths are impossible because empty intervals are rejected.
Marker placement is a pure function, and null is never converted to numeric
zero.

For every link, let `eventCount` be 1 in patient mode and
`aggregate.eventCount` in cohort mode, then:

```text
n = min(max(eventCount, 1), 10)
halfWidthBp = 900000 + n * 250000
fillAlpha = min(0.95, 0.55 + 0.04 * n)
borderAlpha = min(0.98, fillAlpha + 0.08)
```

Each endpoint ribbon interval is
`[max(0, position-halfWidthBp), min(chromosomeLength,
position+halfWidthBp))`. The upper clamp is an intentional V1 safety correction;
it prevents invalid geometry at chromosome ends. The
ribbon fill and border use mint `#71dfc0`; curvature uses `h.ratio=0.65`. Java
calculates both attachment intervals and one closed ribbon path. The geometry
implementation will document the cubic Bezier interpretation used to match
repository-owned SVG fixtures rather than attempting a general `circlize`
reimplementation.

The bottom-left legend is unconditional, even for empty categories. It always
contains “Gain / Duplication / Amplification,” “Loss / Deletion,” and
“Translocation,” using fills `#d7191c`, `#2c7bb6`, and mint `#71dfc0` at alpha
0.88 respectively, with no borders or surrounding box and text size equivalent
to 0.8. The optional input label is metadata and is not drawn as a title in V1.

## 7. Semantic SVG contract

The renderer emits SVG 1.1-compatible XML with a stable `viewBox`; presentation
may use a `<style>` element, but interaction-relevant values live in attributes.
Element ordering is deterministic: definitions, background, chromosomes,
tracks, links, labels, then legend, each in canonical/model order.

### 7.1 Root and group structure

```xml
<svg id="circos-plot-{plot-safe}" class="circos-plot" data-contract-version="1.0"
     data-plot-id="..." data-plot-mode="patient" data-assembly-id="GRCh38">
  <metadata id="circos-metadata-{plot-safe}">...</metadata>
  <defs id="circos-defs-{plot-safe}">...</defs>
  <g id="circos-scene-{plot-safe}" class="circos-scene">
    <g id="circos-background-{plot-safe}" class="circos-background">...</g>
    <g id="circos-chromosomes-{plot-safe}" class="circos-chromosomes">...</g>
    <g id="circos-tracks-{plot-safe}" class="circos-tracks">
      <g id="track-backgrounds-{plot-safe}" class="track-backgrounds">...</g>
      <g id="track-gains-{plot-safe}" class="track track-gains">...</g>
      <g id="track-losses-{plot-safe}" class="track track-losses">...</g>
    </g>
    <g id="circos-links-{plot-safe}" class="circos-links">...</g>
    <g id="circos-labels-{plot-safe}" class="circos-labels">...</g>
    <g id="circos-legend-{plot-safe}" class="circos-legend">...</g>
  </g>
</svg>
```

All groups exist even when empty, giving the viewer stable attachment points.
The stable classes are the attachment API; the plot-safe suffix prevents DOM ID
collisions when several plots share a document. Chromosome sectors use IDs such
as `chromosome-{plot-safe}-1`, class `chromosome-sector`, and
`data-chromosome="1"`. Background tracks use
`track-background-{plot-safe}-1` through `-3`.

### 7.2 Safe IDs and original IDs

DOM IDs never interpolate raw external IDs. `SvgIdEncoder` computes each safe
token as the first 24 lowercase hexadecimal characters (96 bits) of SHA-256 over
the UTF-8 external ID. The plot token namespaces every ID: for example,
`segment-{plot-safe}-{segment-safe}` and `link-{plot-safe}-{link-safe}`.
Collision detection occurs per type per plot and fails generation rather than
changing IDs nondeterministically. This algorithm is part of the 1.0 SVG
contract.

Original opaque IDs remain verbatim only in XML-escaped `data-*` attributes.
They are not used as CSS selectors without browser escaping. Labels and
metadata are XML escaped; no input is inserted as raw markup, CSS, URL, script,
or event-handler content. `.circos-plot > metadata` contains escaped canonical
JSON for root `sourceResultIds` and includes the plot label only when supplied;
it contains no geometry or clinical identity.

### 7.3 Interactive segment elements

Each visible segment is one focusable group:

```xml
<g id="segment-{plot-safe}-{safe}" class="circos-event circos-segment event-gain"
   tabindex="0" role="button"
   data-segment-id="..."
   data-event-group-id="..."
   data-source-result-id="..."
   data-clone-id="..."
   data-event-type="gain"
   data-confidence="HIGH"
   data-chromosome="7"
   data-start="100"
   data-end="200"
   data-copy-number="3">...</g>
```

Required attributes are `data-segment-id`, `data-source-result-id`,
`data-event-type`, `data-chromosome`, `data-start`, and `data-end`.
`data-event-group-id`, `data-confidence`, `data-copy-number`, and
`data-clone-id` are omitted when their model values are null/absent; unknown is
never serialized as an empty string, zero, or `null` text. Numeric formatting is
locale-independent.

### 7.4 Interactive link elements

Each link is one focusable group containing its ribbon path:

```xml
<g id="link-{plot-safe}-{safe}" class="circos-event circos-link event-translocation"
   tabindex="0" role="button"
   data-link-id="..."
   data-event-group-id="..."
   data-source-segment-id="..."
   data-target-segment-id="..."
   data-source-result-id="..."
   data-clone-id="..."
   data-event-type="translocation"
   data-confidence="HIGH"
   data-source-chromosome="9" data-source-position="133729449"
   data-target-chromosome="22" data-target-position="23372909"
   data-aggregate-event-count="..."
   data-aggregate-patient-count="..."
   data-aggregate-sample-count="...">...</g>
```

`data-link-id`, source/target segment IDs, event type, both endpoint
chromosomes, and both point positions are required. Event group, clone, and
confidence are omitted when absent. `data-source-result-id` is required for a
patient link and omitted for a cohort aggregate, whose contributing per-link
result IDs are not present in the V1 contract; plot-level source scope remains
in metadata. All three aggregate attributes are required in cohort mode and
omitted in patient mode. The inner path uses class `circos-link-ribbon`;
interaction attaches to the parent group so future visual layers do not change
selection identity.

Classes describe category, not raw IDs. Viewer state classes are reserved:
`is-hovered`, `is-selected`, `is-related`, `is-dimmed`, `is-filtered`, and
`is-hidden`. The Java renderer will not emit transient state classes.

## 8. Viewer responsibilities and attachment model

The standalone viewer will obtain SVG by loading a Java-generated `.svg` file
or by receiving the same SVG inline from its development harness. Because
cross-document event access differs for `<object>` and inline SVG, the initial
viewer should normalize to an inline DOM without modifying geometry. It will
verify `data-contract-version="1.0"` before attaching.

The viewer will:

- delegate hover/focus and click handling from `.circos-scene` to
  `.circos-event`;
- build escaped text-only tooltips from `data-*` values;
- select by opaque ID and highlight all elements sharing
  `data-event-group-id`;
- dim unrelated events with state classes;
- toggle gain, loss, and translocation classes;
- filter on canonical chromosome endpoint attributes, confidence, event type,
  and clone ID when clone data exists;
- apply zoom/pan as a transform to `#circos-scene` or by changing the root
  `viewBox`, and restore the original transform/viewBox on reset;
- export the current SVG by cloning the root, preserving active presentation
  state, removing viewer-only unsafe/runtime nodes, and serializing it; and
- emit a `circos-selection-change` `CustomEvent` whose `detail` contains only
  opaque `plotId`, `plotSourceResultIds`, `segmentIds`, `linkIds`,
  `eventGroupIds`, `selectedSourceResultIds`, and `cloneIds` for future MPG
  callbacks. A cohort link may have no selected source result IDs; the plot
  scope remains explicit and separate.

Filtering will change classes/ARIA state only. It will never regenerate a path,
recalculate an angle, move an endpoint, or infer cohort aggregation. UI panels
and controls remain outside the SVG so standalone SVG export is still valid.
Confidence filtering is exact categorical matching (case preserved), not a
numeric threshold. All DOM queries are rooted at the plot and support one or
more plot instances.

## 9. Rendering pipeline

The public facade coordinates these stages in order:

1. **JSON parsing** — parse with duplicate-key detection and bounded input
   settings; report malformed JSON without partially constructing a plot.
2. **Schema dispatch and validation** — read `schemaVersion` minimally, reject
   unsupported/missing versions, select the bundled schema, and validate types,
   required fields, enums, formats, and conditional patient/cohort shape.
3. **Domain mapping** — create immutable Java records without coercing strings,
   numbers, nulls, or chromosome names.
4. **Assembly lookup** — load the requested bundled assembly and its canonical
   chromosome order/lengths.
5. **Domain validation** — normalize aliases, check bounds, IDs/references,
   mode-specific aggregate rules, copy-number semantics, and clone references;
   return all actionable errors in deterministic JSON-path order.
6. **Layout calculation** — calculate sectors, gaps, tracks, annular paths,
   endpoints, ribbons, labels, and legend geometry using pure Java geometry.
7. **SVG scene generation** — combine immutable geometry with domain semantics
   and the compatibility theme into an internal SVG element tree.
8. **Semantic serialization** — encode deterministic DOM IDs, XML-escape all
   untrusted content, use locale-independent bounded numeric precision, and
   serialize UTF-8 SVG.
9. **Viewer enhancement** — separately, JavaScript verifies the SVG contract
   and attaches interactions without changing geometry.

Generation is fail-closed: no SVG is returned if any preceding stage fails.
Validation errors are distinguished from unsupported version, assembly load,
geometry invariant, and serialization failures.

The CLI wraps stages 1-8 without altering them: it opens the selected input,
passes it to `CircosApplication`, and returns the validated model,
and maps typed failures to the exit codes defined in section 3. Viewer
enhancement remains a separate browser step.

## 10. Testing strategy

The initial renderer phase prioritizes deterministic structural SVG tests using
JUnit 5, a JSON Schema validator, and XML parsing. Tests assert the SVG element
tree, ordering, paths, attributes, IDs, classes, numeric values, and escaping
directly. Repeated rendering must also produce byte-identical UTF-8 SVG.

Repository-owned golden SVG comparison is added only after those structural
tests pass and the SVG contract is stable. PNG rendering and pixel-based visual
regression infrastructure are explicitly deferred until structural SVG parity
has been established; they are not part of the initial Maven dependencies or
test lifecycle.

### Contract and validation

- accept every valid synthetic example against JSON Schema 1.0;
- reject missing, malformed, and unsupported schema versions before mapping;
- reject extra properties, wrong types, unresolved/duplicate IDs, and invalid
  patient/cohort aggregate placement;
- verify null copy number remains null and is valid only for allowed event
  types; reject omitted copy number, null gain values, non-integral values, and
  every gain value below the absolute-copy-number minimum of 3;
- accept empty arrays/categories without exceptions or fabricated elements;
- reject negative, empty, reversed, and assembly-out-of-range coordinates with
  stable structured errors;
- accept exact assembly aliases and reject fuzzy/unsupported build strings;
- reject inconsistent cohort counts and all patient/cohort sentinel conventions.

### Assembly and geometry

- load GRCh37 and GRCh38 resource lengths and canonical order; reject unknown
  assemblies and malformed resource files;
- normalize documented chromosome aliases and reject noncanonical contigs;
- map chromosome start, exact midpoint, and end boundaries to expected angles
  within a strict tolerance;
- verify 1-22, X, Y sector ordering, proportional spans, each specified gap, the
  larger closing gap, clockwise direction, and 90-degree origin;
- compare segment annular paths at sector boundaries and across representative
  short/long intervals; assert finite coordinates and correct winding;
- verify translocation attachment intervals derive from the correct source and
  target point positions, including chromosome-edge clamping, chr9-chr22, and
  same-chromosome cases;
- lock specified ribbon width, opacity, and curvature calculations at minimum,
  maximum, and representative patient/cohort inputs, including `eventCount` 1,
  10, and values above 10;
- verify specified track heights, y mappings, margins, 684-square view box, all
  24 chromosome colors, and unconditional legend values.

### Rendering and integration

- inject XML metacharacters, quotes, Unicode, long opaque IDs, and hostile
  label/metadata strings; parse output as XML and assert no executable markup;
- verify stable safe DOM IDs, collision behavior, semantic classes, and every
  required/conditionally omitted `data-*` attribute;
- render patient and cohort fixtures and verify explicit aggregate attributes;
- verify gain/loss/translocation empty categories, conditional event layers,
  three backgrounds, unconditional three-item legend, chromosome labels, and
  z-order;
- render the same logical payload repeatedly, under different default locales
  and time zones, and require identical SVG bytes;
- after structural parity is stable, compare representative output against
  repository-owned canonical SVG golden files; PNG snapshots remain deferred.

### CLI harness

- read a valid fixture path and produce a deterministic validation summary
  through the public facade; SVG output is deferred;
- support standard input/output without closing caller-owned streams;
- return stable exit codes and standard-error diagnostics for usage, schema,
  domain-validation, and generation failures;
- prove that the CLI contains no geometry, renderer, aggregation, database, or
  network logic; and
- keep CLI tests separate from rendering-core tests even though both share the
  single Maven module.

Viewer tests, when implementation begins, will use browser DOM tests for hover,
keyboard focus, selection, group highlighting, filters, clone-control absence,
zoom/reset, export validity, multi-instance isolation, and callback event
payloads. A guard test will assert that viewer source contains no independent
chromosome-length table or circular path generator.

## 11. Initial synthetic examples

All fixture identifiers and labels state that they are synthetic; no real
patient identifiers or source records are permitted.

- `patient-bcr-abl1.json`: one synthetic chr9-chr22 translocation with two
  endpoint segment IDs/points and one event group; breakpoint segments need not
  be duplicated as plotted CNV records.
- `gains-and-losses.json`: representative gain and loss intervals, copy-number
  marker boundaries, plus a loss with explicit null copy number.
- `crossing-links.json`: several links across different chromosome quadrants to
  exercise endpoint placement, z-order, curvature, and overlap.
- `cohort-aggregate.json`: cohort mode with explicit event, patient, and sample
  counts and no raw-link sentinel convention.
- `empty-categories.json`: valid empty segments/links plus variants containing
  gains only, losses only, and links only to verify conditional tracks and the
  unconditional legend.

Phase 1 examples are JSON-only and must remain synthetic. Generated SVGs are
deferred until the renderer phase.

## 12. Approval decisions and implementation sequence

Before implementation, approval should confirm:

1. Java package/artifact naming (`org.mpg.circos`);
2. zero-based half-open coordinates and explicit MPG adapter conversion;
3. the JSON 1.0 fields, especially explicit nullable `copyNumber`/`confidence`,
   the optional root `label`, absolute gain copy number `>= 3`, point-based link
   endpoints with segment identities, and the conditional aggregate object;
4. SHA-256-derived safe DOM IDs plus original IDs in escaped attributes;
5. the semantic group and `data-*` contract;
6. the documented safety changes relative to legacy output: strict input
   bounds, derived ribbon-end clamping, explicit null handling, and the gain
   rectangle floor at y=3.2;
7. one Maven module with the CLI as an outer adapter in the production JAR; and
8. deterministic structural SVG tests before golden SVG and deferred PNG
   regression infrastructure.

After approval, work should proceed in vertical, testable slices: Maven and
model/schema plus the CLI argument contract; assembly resources and validation;
geometry; deterministic structural SVG renderer tests; CLI end-to-end fixture
generation; golden SVG parity; then viewer enhancement. PNG regression work may
begin only after SVG parity is established. No database, REST, Docker, or MPG
adapter work is part of that sequence.

# Host integration contract

## Status and boundary

This document defines the neutral boundary between a future host application
and `circos-generator`. It does not implement an adapter and does not choose
direct Java linkage, REST, FileMaker, JDBC, or any other transport. The renderer
and viewer know nothing about searches, tables, databases, clinical records, or
contributor resolution.

The current clinician GUI row shape is a representative prototype only. A
future host may obtain the same logical values through a different UI or
backend.

The authoritative normalized input and renderer behavior are defined in
[technical-design.md](technical-design.md). The dependency-free viewer API is
summarized in [the viewer README](../viewer/README.md). This document owns only
the boundary between those stable contracts and a future clinician-search host.

## Version 2 renderer boundary

The host or caller decides which biological records should be visualized and
performs any source-specific classification before constructing renderer
objects. The renderer accepts only `GAIN`, `LOSS`, and `TRANSLOCATION`
visualization categories; it does not accept database aliases as new event
types. Gains and losses contain one affected genomic interval. Translocations
contain two affected genomic interval endpoints.

All renderer coordinates are zero-based, half-open. For display, a link uses the
midpoint of each affected interval by default. Midpoint attachment positions are
approximate geometry and are not evidence of exact or confirmed breakpoints.
The host must not describe them as such. Exact-interval cohort grouping should
be described as `Exact genomic intervals`.

Version 1 point inputs remain readable through a compatibility adapter, but all
new host integration should emit Schema Version 2.0 interval endpoints. The
renderer does not query a database, choose eligible records, or know JDBC, H2,
FileMaker, MPG schemas, clinician search, or source-system query logic.

## Responsibility boundaries

| Component | Owns | Must not own |
|---|---|---|
| Renderer | Schema/domain validation, normalized layout, deterministic semantic SVG, size-limit enforcement, safe failure | Searches, eligibility policy, database access, pagination, host state, clinical records, page layout |
| Viewer | Inline SVG attachment, hover/focus summary, one active selection, highlighting/dimming, keyboard behavior, opaque callbacks, instance-local cleanup | Navigation, host controls, record retrieval, contributor resolution, clinical details, search or stale-state tracking |
| Neutral adapter | Eligibility, normalization, stable opaque identity, deduplication, aggregate preparation, rejection reporting, host-owned reverse mappings | Queries, FileMaker schemas, authentication, HTML, navigation, rendering, shared clinical state, transport policy |
| Clinician-search host | Search/filter execution, permissions, records, pagination, selected-row scope, responsive layout, states, synchronization, drill-downs, record export, plot-export action | Renderer geometry, unsafe SVG mutation, invented callback identities |

## Host presentation and concurrency requirements

The host embeds the generated semantic SVG and viewer inline in its existing
search page. Generating a plot must not open a new browser tab, navigate away
from the search workflow, or launch the standalone development example. The
host owns responsive placement; a split layout with the results/details area
and plot each occupying approximately half the available screen is a supported
integration pattern, not renderer behavior. The viewer uses the size of its
host-controlled container (or dimensions explicitly supplied by that host); it
does not size or rearrange the surrounding page. Filters, result-table actions,
and other host controls remain usable while the plot is displayed.

The library and viewer have no global user or clinical session and must not
store mutable clinical state in global static fields. A host may run multiple
independent plot requests concurrently and may attach multiple viewer instances,
provided each request supplies an immutable input snapshot and each viewer has
its own container. A future service or adapter must keep input, output,
selection state, contributor mappings, and clinical context scoped to the
originating request/session. Generated identifiers are deterministic from the
supplied normalized input or safely scoped to that immutable request. Production
concurrency limits and deployment capacity are adapter concerns and must be
load-tested after the integration mechanism is selected.

## Intended clinician workflow and result scope

1. The clinician enters filters and selects **Search**.
2. The host displays matching records through its normal result table and
   pagination.
3. **Export Results** and **Generate Circos Plot** are separate, independent
   actions; the host does not require a modal choice between them.
4. By default, generation uses the complete filtered set of eligible CNV/SV
   findings, never only the currently visible page. The host may additionally
   offer generation from explicitly selected eligible rows.
5. The host communicates the total result count, eligible CNV/SV count, and,
   when applicable, selected eligible count before generation.
6. The host captures an immutable scope snapshot, runs it through the neutral
   adapter, invokes the renderer through the eventually selected transport,
   and inserts the semantic SVG inline in its viewer container.
7. A successful new plot replaces the one active plot in the initial clinician
   UX. Result rows remain visible and usable beside it.
8. The clinician may separately export the current Circos visualization.

SNV/indel and repeat-expansion rows may remain in mixed search results but are
not included in Version 1 Circos input. The host must not silently omit eligible
CNV/SV findings because of pagination, ranking, or size. If the chosen scope
exceeds the configured ceiling, the host requires narrower filters or an
explicit selected-row scope; the renderer rejects the complete oversized input
without truncation.

For patient generation, the chosen row identifies one source result and the
host supplies all eligible CNV/SV findings for that result, not only the chosen
finding. For cohort generation, the scope is either all eligible findings in
the complete filtered result or the explicitly selected eligible rows.

## Same-page lifecycle and states

Changing filters, rerunning a search, or changing selected-row scope does not
automatically regenerate a plot. The existing valid plot remains visible but
the host marks it stale, for example:

```text
Results have changed. Regenerate the plot to update the visualization.
```

The host retains enough non-clinical scope metadata to identify the producing
search snapshot, generation mode (all filtered or selected rows), eligible
count, genome build, and generation time. The renderer does not compare search
states or detect staleness.

Host-owned states are:

| State | Required host behavior |
|---|---|
| No eligible CNV/SV findings | Disable **Generate Circos Plot**, keep record export available when other results exist, and explain that no eligible findings are available |
| Loading | Show progress, prevent duplicate generation, define cancellation if supported, and prevent an older response from replacing a newer request |
| Success | Insert or replace the inline viewer, record its scope, clear stale state, and preserve the result area |
| Error | Show a useful message and retry path, record correlation/logging information, and preserve the previous valid plot when appropriate |

Renderer failures remain fail-closed: invalid input or generation failure must
not replace an existing valid SVG with empty or partial output.

## Java rendering API

The stable entry point is `org.mpg.circos.CircosApplication`:

```java
CircosApplication application = new CircosApplication();

// Versioned JSON input: parse, validate, normalize, lay out, and render.
SvgDocument document = application.render(jsonInputStream);

// Typed input: defensively validate and normalize before rendering.
SvgDocument typedDocument = application.render(circosPlot, RenderOptions.defaults());

// A host-owned limit may be lower or higher than the 20,000-event default.
SvgDocument boundedDocument = application.render(circosPlot, new RenderOptions(10_000));
```

`CircosApplication` never filters search results, queries missing links,
deduplicates database records, aggregates cohorts, resolves contributors, or
silently truncates input. The host supplies a complete fixed snapshot.

## Neutral adapter shape

The transport-neutral adapter receives the host's captured filtered or selected
scope. It:

- determines Version 1 CNV/SV eligibility and reports unsupported findings;
- normalizes build aliases, chromosomes, coordinate convention, and event
  types before creating renderer input;
- validates required values and rejects ambiguous or mixed-build input;
- preserves caller-provided stable opaque finding, result, event-group, and
  aggregate identities;
- deduplicates using the established stable finding identity;
- preserves trusted cohort aggregate IDs, or applies only the documented exact
  interval/breakpoint fallback rules;
- creates separate normalized segment, link, and aggregate-link values rather
  than one large nullable DTO; and
- returns a host-owned reverse mapping from opaque selection/aggregate IDs to
  result-row and contributor identities for synchronization and drill-down.

The reverse mapping never enters SVG, tooltip text, or the generic callback.
The adapter does not execute queries, know a FileMaker schema, render HTML,
manage authentication or navigation, store shared clinical state, or select a
transport.

The following records are an interface definition, not repository
implementation classes. A future adapter may use equivalent types in its own
module:

```java
record NormalizedSegmentRow(
        String findingId,
        String sourceResultId,
        String genomeBuild,
        String chromosome,
        long start,
        long end,
        EventType eventType,
        SegmentDisplayType displayType,
        Integer copyNumber,
        String confidence,
        List<String> genes,
        List<String> methods,
        String label) {}

record NormalizedLinkRow(
        String linkId,
        String sourceResultId,
        String eventGroupId,
        String sourceSegmentId,
        String sourceChromosome,
        long sourcePosition,
        String targetSegmentId,
        String targetChromosome,
        long targetPosition,
        String confidence,
        List<String> sourceGenes,
        List<String> targetGenes,
        List<String> methods) {}

record NormalizedCohortAggregateLinkRow(
        String aggregateId,
        String sourceChromosome,
        long sourcePosition,
        String targetChromosome,
        long targetPosition,
        int eventCount,
        int patientCount,
        int sampleCount,
        String groupingDescription,
        List<ConfidenceCount> confidenceDistribution) {}

record PlotScopeMetadata(
        String opaqueScopeId,
        ScopeMode mode,
        int totalResultCount,
        int eligibleCount,
        int selectedEligibleCount,
        String genomeBuild) {}

record AdapterResult(
        CircosPlot plot,
        PlotScopeMetadata scope,
        SelectionResolutionIndex selectionIndex,
        List<AdapterIssue> issues) {}

interface CircosInputAdapter<S, L, A> {
    AdapterResult patientPlot(String plotId, String genomeBuild,
            String sourceResultId, List<S> segments, List<L> links);

    AdapterResult cohortPlot(String plotId, String genomeBuild,
            List<String> representedSourceResultIds,
            List<S> segments, List<A> aggregateLinks);
}
```

`SelectionResolutionIndex` and `AdapterIssue` are host-owned conceptual types.
The index maps visualization IDs to authorized host records/contributors and is
never serialized into `CircosPlot` or SVG. Issues distinguish rejected,
unsupported, and excluded input so the host can explain counts without silent
omission. `ScopeMode` distinguishes patient, all-filtered cohort, and
selected-row cohort generation.

The generic parameters deliberately prevent this repository from depending on
the prototype GUI's `FindingRow`. Implementations belong to a future host or
adapter module after the production integration mechanism is chosen.

## Representative GUI mapping

Only prototype rows with `findingType = "CNV/SV"` are segment-eligible:

| Neutral value | Representative prototype source |
|---|---|
| `findingId` | string form of `finding_id` (`segment_id` today) |
| `sourceResultId` | string form of `sample_test_result_id` |
| `genomeBuild` | `genome_build` |
| `chromosome` | `chromosome` |
| `start`, `end` | `start_pos`, `end_pos` |
| `eventType` | normalized gain or loss from `event_type` |
| `copyNumber` | strictly parsed integer from `copy_number`, or null for loss |
| `confidence` | optional enrichment; absent from the prototype row today |
| `label` | optional non-clinical label, such as the selected gene annotation |

SNV/Indel and Repeat Expansion rows do not map into V1 plot events. The host
deduplicates prototype CNV/SV rows by the stable tuple
`finding_type + finding_id + sample_test_result_id`. Structural links are not
present in the GUI row and must be supplied separately by the host.

MRN, name, date of birth, accession text, diagnosis, and other clinical values
must not enter `CircosPlot`, SVG metadata, labels, or viewer callbacks. If cohort
aggregation needs patient/sample distinctness, the host may use opaque
aggregation keys internally and must remove them before invoking the renderer.

## Cohort link identity

The renderer consumes already-aggregated cohort links. It does not determine
membership. The host applies these rules:

1. Use a caller-supplied cohort aggregate ID when one is available.
2. Do not group by a database `event_group_id` unless the host explicitly
   guarantees it is a cohort-wide stable identity.
3. Without an aggregate ID, group only by exact normalized breakpoint identity.
   Canonicalize endpoint order so reciprocal representations compare equally,
   but do not group solely by chromosome pair and do not average distinct
   positions.
4. Supply a stable opaque `aggregateId`, representative exact endpoints, and
   explicit event, patient, and sample counts.
5. Retain `aggregateId -> contributing finding IDs` outside this repository.

An aggregate ID need not be stored in a database. It may be stable within a
captured search snapshot. Persist it only if the host later treats aggregates as
stored entities.

## Viewer callback

The SVG must be inline before attaching the viewer:

```html
<div id="plot"></div>
<script src="circos-viewer.js"></script>
<script>
  const host = document.getElementById("plot");
  host.addEventListener("circos-selection-change", event => {
    // Resolve opaque IDs in host-owned state. Do not expect clinical records.
    const selectedId = event.detail.aggregateIds[0]
      || event.detail.segmentIds[0]
      || event.detail.linkIds[0]
      || null;
    hostApplication.onCircosSelection(selectedId);
  });

  // load() is asynchronous and resolves to the container-scoped controller.
  const controllerPromise = CircosViewer.load(
    host,
    "/generated/example.svg"
  );
</script>
```

The bubbling event detail contains only:

```text
plotId
plotSourceResultIds[]
segmentIds[]
linkIds[]
eventGroupIds[]
aggregateIds[]
selectedSourceResultIds[]
```

At most one segment or link ID is selected. Empty selection arrays mean that
selection was cleared. For an aggregated cohort segment or link,
`aggregateIds[0]` is the same caller-supplied opaque identity carried by its
type-specific ID, and `selectedSourceResultIds` is empty; the host must resolve
contributors through its external aggregate mapping. The future host owns table
synchronization, details display, contributor lookup, and any
Events/Patients/Samples drill-down.

Hover is a concise, noninteractive summary and never changes application
selection state. On click or keyboard selection, the host resolves the emitted
opaque identity against the immutable reverse mapping for that plot instance.
For a direct finding, it may highlight or scroll to a matching result row and
update its persistent details area. For a cohort aggregate, the host may expose
persistent **Events**, **Patients**, and **Samples** views from its external
contributor map. Tooltip content contains no transient drill-down links.

Clearing the viewer selection emits empty selection arrays. The host then
clears its Circos-driven row highlight and returns the persistent details area
to its neutral or host-defined state. Table scrolling and details presentation
must not cause a new renderer request.

Callback identities are stable for the lifetime of the rendered snapshot and
scoped to the emitting viewer container. They contain no database credentials,
MRN, accession number, patient name, date of birth, diagnosis, or clinical
record objects. The host retrieves authorized records independently.

## Plot-instance lifecycle

The initial clinician-search view displays one active plot. Generating another
plot replaces it. Before replacement, the host destroys the current viewer
controller so its listeners, tooltip, selection, and transient classes are
removed. The replacement starts with no active selection.

The standalone viewer continues to support multiple containers. Each controller
has isolated hover, selection, callbacks, and cleanup; an event in one container
must not alter another. This supports multiple plots elsewhere and concurrent
clinicians even though the initial host UX chooses one active plot.

## Export ownership

**Export Results** belongs entirely to the clinician-search host and exports
filtered or explicitly selected clinical result records in a host-selected
format such as TSV or CSV. It is independent of plot generation.

**Export Circos Plot** is a host-owned visualization action that exports the
currently rendered visualization without opening a new page or tab. SVG is the
canonical lossless output. PNG may be added later as a convenience format but
does not replace SVG. Version 1 adds no visible export control inside the
standalone viewer.

## Deferred integration mechanism

No direct Java, local/remote REST, or other process boundary is selected here.
The later decision must consider the clinician host technology stack,
deployment topology, security boundary, expected concurrency, latency,
serialization cost, scaling, operational ownership, testing, and maintenance.
None of the stable normalized-input, inline-viewer, or opaque-callback contracts
depends on that choice.

## Performance assumptions

Initial characterization treats 500 findings as a large patient plot and
10,000 findings as a large cohort plot. These are engineering assumptions, not
confirmed clinical limits. Timing and SVG-size measurements are informational;
only functional correctness is asserted. The configurable default safety limit
accepts exactly 20,000 events and deterministically rejects 20,001 without
truncation. Visual readability requires a separate clinical review.

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

## Host presentation and concurrency requirements

The host embeds the generated semantic SVG and viewer inline in its existing
search page. Generating a plot must not open a new browser tab, navigate away
from the search workflow, or launch the standalone development example. The
host owns responsive placement; a split layout with the results/details area
and plot each occupying approximately half the available screen is a supported
integration pattern, not renderer behavior.

The library and viewer have no global user or clinical session. A host may run
multiple independent plot requests concurrently and may attach multiple viewer
instances, provided each request supplies an immutable input snapshot and each
viewer has its own container. A future service or adapter must keep input,
output, selection state, contributor mappings, and clinical context scoped to
the originating request/session. Production concurrency limits and deployment
capacity are adapter concerns and must be tested after the integration
mechanism is selected.

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

interface CircosInputAdapter<S, L, A> {
    CircosPlot patientPlot(String plotId, String genomeBuild,
            String sourceResultId, List<S> segments, List<L> links);

    CircosPlot cohortPlot(String plotId, String genomeBuild,
            List<String> representedSourceResultIds,
            List<S> segments, List<A> aggregateLinks);
}
```

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
  CircosViewer.load(host, "/generated/example.svg");

  host.addEventListener("circos-selection-change", event => {
    // Resolve opaque IDs in host-owned state. Do not expect clinical records.
    const selectedId = event.detail.aggregateIds[0]
      || event.detail.segmentIds[0]
      || event.detail.linkIds[0]
      || null;
    hostApplication.onCircosSelection(selectedId);
  });
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
contributors through its external aggregate mapping. The future host owns table synchronization, details display,
contributor lookup, and any Events/Patients/Samples drill-down.

## Performance assumptions

Initial characterization treats 500 findings as a large patient plot and
10,000 findings as a large cohort plot. These are engineering assumptions, not
confirmed clinical limits. Timing and SVG-size measurements are informational;
only functional correctness is asserted. The configurable default safety limit
accepts exactly 20,000 events and deterministically rejects 20,001 without
truncation. Visual readability requires a separate clinical review.

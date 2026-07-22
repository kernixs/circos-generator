# Circos viewer

Serve the repository root over HTTP and open `viewer/`:

```text
python3 -m http.server 8000
```

Then visit `http://localhost:8000/viewer/`. The example page loads the
representative Java-generated SVG files and inserts each SVG inline in the DOM.

The viewer accepts semantic SVG contracts `1.0` and `2.0`. Version 2 links
carry zero-based, half-open source and target intervals using
`data-source-start`, `data-source-end`, `data-target-start`, and
`data-target-end`. The displayed one-based ranges are labeled **Linked genomic
regions**. Midpoint anchors are approximate display geometry and must not be
interpreted as confirmed breakpoints. Version 1 point attributes remain
supported only for compatibility.

Caller-provided event labels, genes, copy number, confidence, aggregate patient,
sample and event counts, methods, aggregation descriptions, and escaped
additional key/value metadata are shown when present. An aggregation based on
identical V2 endpoints is displayed as **Aggregation: Exact genomic intervals**.

Maven consumers can load the exact packaged script and required styles with
`org.mpg.circos.viewer.CircosViewerAssets.javascript()` and
`CircosViewerAssets.stylesheet()`. The dependency-free browser API is available as `window.CircosViewer`:

- `attach(container, svgElement)` enhances an existing inline semantic SVG.
- `mount(container, svgText)` parses, inserts, and enhances SVG text.
- `load(container, url)` asynchronously fetches, inserts, and enhances a
  generated SVG.

These APIs accept only trusted semantic SVG produced by `circos-generator`.
They do not sanitize arbitrary or user-supplied SVG; hosts that accept untrusted
SVG must sanitize it before calling `attach()`, `mount()`, or `load()`.

`attach()` and `mount()` return a container-scoped controller immediately.
`load()` returns a `Promise` that resolves to the same controller after the SVG
has been fetched and mounted. Callers must await or chain that promise before
using the controller:

```javascript
const controller = await CircosViewer.load(container, "/generated/plot.svg");
controller.clearSelection();
```

The resolved controller provides:

- `svg` references the attached inline SVG element.
- `selectedId()` returns the one selected opaque event ID or `null`.
- `clearSelection()` clears the active selection and emits the normal callback.
- `destroy()` removes viewer listeners, tooltip, transient state, and instance
  registration without changing another container.

Attaching or mounting again in the same container destroys its previous
controller first. Different containers retain independent hover, selection,
callback, and cleanup state. The viewer never navigates, opens a tab, or assumes
that it owns the page; responsive placement and replacement timing belong to
the host.

Selection changes dispatch a bubbling `circos-selection-change` event from the
viewer container. Its `detail` contains only opaque plot, source-result,
segment, link, event-group, and aggregate IDs. For aggregated cohort segments
and links, `aggregateIds[0]` is the caller-provided aggregate ID; contributor
identities are never embedded or resolved by the viewer.

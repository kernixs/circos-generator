# Circos viewer

Serve the repository root over HTTP and open `viewer/`:

```text
python3 -m http.server 8000
```

Then visit `http://localhost:8000/viewer/`. The example page loads all three
representative Java-generated SVG files and inserts each SVG inline in the DOM.

The dependency-free public API is available as `window.CircosViewer`:

- `attach(container, svgElement)` enhances an existing inline semantic SVG.
- `mount(container, svgText)` parses, inserts, and enhances SVG text.
- `load(container, url)` fetches, inserts, and enhances a generated SVG.

Selection changes dispatch a bubbling `circos-selection-change` event from the
viewer container. Its `detail` contains only opaque plot, source-result,
segment, link, and event-group IDs. For cohort links, the link ID is the
caller-provided aggregate ID; contributor identities are never embedded or
resolved by the viewer.

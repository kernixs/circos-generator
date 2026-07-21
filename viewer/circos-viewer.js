(function (global) {
    "use strict";

    var instances = new WeakMap();

    function requireElement(value, name) {
        if (!value || value.nodeType !== 1) throw new TypeError(name + " must be an Element");
        return value;
    }

    function hasClass(element, className) {
        return element && element.classList && element.classList.contains(className);
    }

    function setClass(element, className, enabled) {
        if (enabled) element.classList.add(className);
        else element.classList.remove(className);
    }

    function closestEvent(target, svg) {
        var current = target;
        while (current && current !== svg) {
            if (hasClass(current, "circos-event")) return current;
            current = current.parentNode;
        }
        return hasClass(svg, "circos-event") ? svg : null;
    }

    function capitalize(value) {
        return value ? value.charAt(0).toUpperCase() + value.slice(1) : "Event";
    }

    function coordinate(value, addOne) {
        var number = Number(value) + (addOne ? 1 : 0);
        return number.toLocaleString("en-US");
    }

    function data(element, name) {
        return element.getAttribute("data-" + name);
    }

    function meaningful(value) {
        if (!value || !value.trim()) return null;
        var normalized = value.trim().toLowerCase();
        return normalized === "null" || normalized === "unknown" || normalized === "n/a" ? null : value.trim();
    }

    function jsonArray(element, name) {
        var value = data(element, name);
        if (!value) return [];
        try {
            var parsed = JSON.parse(value);
            return Array.isArray(parsed) ? parsed : [];
        } catch (ignored) {
            return [];
        }
    }

    function jsonObject(element, name) {
        var value = data(element, name);
        if (!value) return {};
        try {
            var parsed = JSON.parse(value);
            return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
        } catch (ignored) {
            return {};
        }
    }

    function formatLength(basePairs) {
        if (basePairs < 1000) return basePairs.toLocaleString("en-US") + " bp";
        if (basePairs < 1000000) return formatDecimal(basePairs / 1000) + " kb";
        return formatDecimal(basePairs / 1000000) + " Mb";
    }

    function formatDecimal(value) {
        return value.toLocaleString("en-US", {minimumFractionDigits: 0, maximumFractionDigits: 2});
    }

    function summarizedGenes(genes) {
        if (!genes.length) return null;
        return genes.length > 10 ? genes.length.toLocaleString("en-US") + " genes" : genes.join(", ");
    }

    function methodsLine(methods) {
        if (!methods.length) return null;
        return (methods.length === 1 ? "Method: " : "Methods: ") + methods.join(", ");
    }

    function chromosomeRank(value) {
        var number = Number(value);
        if (Number.isInteger(number) && number >= 1 && number <= 22) return number;
        if (value === "X") return 23;
        if (value === "Y") return 24;
        return 1000;
    }

    function linkEndpoint(event, prefix) {
        var position = data(event, prefix + "-position");
        var legacy = position !== null;
        return {
            chromosome: data(event, prefix + "-chromosome"),
            start: legacy ? Number(position) : Number(data(event, prefix + "-start")),
            end: legacy ? Number(position) + 1 : Number(data(event, prefix + "-end")),
            legacy: legacy,
            genes: jsonArray(event, prefix + "-genes")
        };
    }

    function canonicalLink(event) {
        var source = linkEndpoint(event, "source");
        var target = linkEndpoint(event, "target");
        var sourceRank = chromosomeRank(source.chromosome);
        var targetRank = chromosomeRank(target.chromosome);
        if (sourceRank > targetRank || (sourceRank === targetRank && source.start > target.start)
                || (sourceRank === targetRank && source.start === target.start && source.end > target.end)
                || (sourceRank === targetRank && source.start === target.start && source.end === target.end
                    && source.chromosome.localeCompare(target.chromosome) > 0)) {
            return {source: target, target: source};
        }
        return {source: source, target: target};
    }

    function genomicRegion(endpoint) {
        var start = coordinate(endpoint.start, true);
        if (endpoint.legacy) return "chr" + endpoint.chromosome + ":" + start;
        return "chr" + endpoint.chromosome + ":" + start + "–" + coordinate(endpoint.end, false);
    }

    function appendDisplayMetadata(lines, event) {
        var label = meaningful(data(event, "label"));
        if (label) lines.push("Event label: " + label);
        var metadata = jsonObject(event, "additional-metadata");
        Object.keys(metadata).sort().forEach(function (key) {
            var value = meaningful(String(metadata[key]));
            if (value) lines.push(key + ": " + value);
        });
    }

    function appendAggregate(lines, event) {
        if (!data(event, "aggregate-event-count")) return;
        lines.push("");
        lines.push("Events: " + Number(data(event, "aggregate-event-count")).toLocaleString("en-US"));
        lines.push("Samples: " + Number(data(event, "aggregate-sample-count")).toLocaleString("en-US"));
        lines.push("Patients: " + Number(data(event, "aggregate-patient-count")).toLocaleString("en-US"));
        var methods = methodsLine(jsonArray(event, "methods"));
        if (methods) lines.push(methods);
        var grouping = meaningful(data(event, "grouping-description"));
        if (grouping) {
            if (grouping.toLowerCase() === "exact breakpoints") grouping = "Exact genomic intervals";
            lines.push("Aggregation: " + grouping);
        }
        var distribution = jsonArray(event, "confidence-distribution").filter(function (value) {
            return value && meaningful(String(value.label || "")) && Number(value.count) > 0;
        });
        if (distribution.length) {
            lines.push("Confidence: " + distribution.map(function (value) {
                return value.label + " " + Number(value.count).toLocaleString("en-US");
            }).join(", "));
        }
    }

    function tooltipText(svg, event) {
        var lines = [];
        if (hasClass(event, "circos-segment")) {
            lines.push("Event type: " + capitalize(data(event, "display-type") || data(event, "event-type")));
            appendDisplayMetadata(lines, event);
            lines.push("Genomic range: chr" + data(event, "chromosome") + ":"
                    + coordinate(data(event, "start"), true)
                    + "–" + coordinate(data(event, "end"), false));
            lines.push("Interval length: "
                    + formatLength(Number(data(event, "end")) - Number(data(event, "start"))));
            if (data(event, "copy-number")) lines.push("Copy number: " + data(event, "copy-number"));
            lines.push("Genome build: " + svg.getAttribute("data-assembly-id"));
            var genes = summarizedGenes(jsonArray(event, "genes"));
            if (genes) lines.push("Genes: " + genes);
            if (data(event, "aggregate-event-count")) appendAggregate(lines, event);
            else {
                var segmentMethods = methodsLine(jsonArray(event, "methods"));
                if (segmentMethods) lines.push(segmentMethods);
                var segmentConfidence = meaningful(data(event, "confidence"));
                if (segmentConfidence) lines.push("Confidence: " + segmentConfidence);
            }
        } else {
            lines.push("Event type: " + capitalize(data(event, "event-type")));
            appendDisplayMetadata(lines, event);
            var canonical = canonicalLink(event);
            lines.push("Linked genomic regions: " + genomicRegion(canonical.source)
                    + " ↔ " + genomicRegion(canonical.target));
            lines.push("Genome build: " + svg.getAttribute("data-assembly-id"));
            var sourceGenes = summarizedGenes(canonical.source.genes);
            var targetGenes = summarizedGenes(canonical.target.genes);
            if (sourceGenes && targetGenes) lines.push("Genes: " + sourceGenes + " ↔ " + targetGenes);
            else if (sourceGenes) lines.push("Source genes: " + sourceGenes);
            else if (targetGenes) lines.push("Target genes: " + targetGenes);
            if (data(event, "attachment-policy") === "midpoint") {
                lines.push("Attachment: Approximate interval midpoint");
            }
            if (data(event, "aggregate-event-count")) appendAggregate(lines, event);
            else {
                var linkMethods = methodsLine(jsonArray(event, "methods"));
                if (linkMethods) lines.push(linkMethods);
                var linkConfidence = meaningful(data(event, "confidence"));
                if (linkConfidence) lines.push("Confidence: " + linkConfidence);
            }
        }
        return lines.join("\n");
    }

    function sourceResultIds(svg) {
        var metadata = svg.querySelector("metadata");
        if (!metadata) return [];
        try {
            var parsed = JSON.parse(metadata.textContent);
            return Array.isArray(parsed.sourceResultIds) ? parsed.sourceResultIds.slice() : [];
        } catch (ignored) {
            return [];
        }
    }

    function selectionDetail(svg, selected) {
        var segment = selected && hasClass(selected, "circos-segment");
        var link = selected && hasClass(selected, "circos-link");
        return {
            plotId: svg.getAttribute("data-plot-id"),
            plotSourceResultIds: sourceResultIds(svg),
            segmentIds: segment ? [data(selected, "segment-id")] : [],
            linkIds: link ? [data(selected, "link-id")] : [],
            eventGroupIds: selected && data(selected, "event-group-id")
                    ? [data(selected, "event-group-id")] : [],
            aggregateIds: selected && data(selected, "aggregate-id")
                    ? [data(selected, "aggregate-id")] : [],
            selectedSourceResultIds: selected && !data(selected, "aggregate-id")
                    && data(selected, "source-result-id")
                    ? [data(selected, "source-result-id")] : []
        };
    }

    function parseSvg(svgText) {
        var documentNode = new DOMParser().parseFromString(svgText, "image/svg+xml");
        if (documentNode.querySelector("parsererror")) throw new Error("Invalid SVG document");
        var svg = documentNode.documentElement;
        if (!svg || svg.localName !== "svg") throw new Error("Expected an SVG root element");
        return document.importNode(svg, true);
    }

    function attach(container, suppliedSvg) {
        var host = requireElement(container, "container");
        if (instances.has(host)) instances.get(host).destroy();
        var svg = suppliedSvg || host.querySelector("svg.circos-plot");
        requireElement(svg, "svg");
        var contractVersion = svg.getAttribute("data-contract-version");
        if (!hasClass(svg, "circos-plot") || (contractVersion !== "1.0" && contractVersion !== "2.0")) {
            throw new Error("Unsupported Circos SVG contract");
        }

        host.classList.add("circos-viewer");
        var tooltip = document.createElement("div");
        tooltip.className = "circos-tooltip";
        tooltip.setAttribute("role", "tooltip");
        tooltip.setAttribute("aria-hidden", "true");
        tooltip.hidden = true;
        host.appendChild(tooltip);

        var events = Array.prototype.slice.call(svg.querySelectorAll(".circos-event"));
        var selected = null;
        var tooltipEvent = null;
        events.forEach(function (event) {
            event.setAttribute("aria-pressed", "false");
            event.setAttribute("aria-label", tooltipText(svg, event));
        });

        function positionTooltip(clientX, clientY) {
            var bounds = host.getBoundingClientRect();
            var desiredLeft = clientX - bounds.left + 12;
            var desiredTop = clientY - bounds.top + 12;
            var maxLeft = Math.max(8, host.clientWidth - tooltip.offsetWidth - 8);
            var maxTop = Math.max(8, host.clientHeight - tooltip.offsetHeight - 8);
            tooltip.style.left = Math.min(Math.max(8, desiredLeft), maxLeft) + "px";
            tooltip.style.top = Math.min(Math.max(8, desiredTop), maxTop) + "px";
        }

        function showTooltip(event, clientX, clientY) {
            if (tooltipEvent && tooltipEvent !== event) tooltipEvent.classList.remove("is-hovered");
            tooltipEvent = event;
            event.classList.add("is-hovered");
            tooltip.textContent = tooltipText(svg, event);
            tooltip.hidden = false;
            tooltip.setAttribute("aria-hidden", "false");
            if (typeof clientX === "number") positionTooltip(clientX, clientY);
            else {
                var bounds = event.getBoundingClientRect();
                positionTooltip(bounds.left + bounds.width / 2, bounds.top + bounds.height / 2);
            }
        }

        function hideTooltip(event) {
            if (event) event.classList.remove("is-hovered");
            if (tooltipEvent === event) tooltipEvent = null;
            tooltip.hidden = true;
            tooltip.setAttribute("aria-hidden", "true");
        }

        function emitSelection() {
            host.dispatchEvent(new CustomEvent("circos-selection-change", {
                bubbles: true,
                detail: selectionDetail(svg, selected)
            }));
        }

        function applySelection(next, emit) {
            selected = next;
            var selectedGroup = selected && data(selected, "event-group-id");
            events.forEach(function (event) {
                var isSelected = event === selected;
                var isRelated = !isSelected && selectedGroup
                        && data(event, "event-group-id") === selectedGroup;
                setClass(event, "is-selected", isSelected);
                setClass(event, "is-related", Boolean(isRelated));
                setClass(event, "is-dimmed", Boolean(selected && !isSelected && !isRelated));
                event.setAttribute("aria-pressed", isSelected ? "true" : "false");
            });
            if (emit) emitSelection();
        }

        function toggleSelection(event) {
            applySelection(selected === event ? null : event, true);
        }

        function onMouseOver(domEvent) {
            var event = closestEvent(domEvent.target, svg);
            if (event) showTooltip(event, domEvent.clientX, domEvent.clientY);
        }

        function onMouseMove(domEvent) {
            if (tooltipEvent) positionTooltip(domEvent.clientX, domEvent.clientY);
        }

        function onMouseOut(domEvent) {
            var event = closestEvent(domEvent.target, svg);
            if (!event || event.contains(domEvent.relatedTarget) || document.activeElement === event) return;
            hideTooltip(event);
        }

        function onFocusIn(domEvent) {
            var event = closestEvent(domEvent.target, svg);
            if (event) showTooltip(event);
        }

        function onFocusOut(domEvent) {
            var event = closestEvent(domEvent.target, svg);
            if (event) hideTooltip(event);
        }

        function onClick(domEvent) {
            var event = closestEvent(domEvent.target, svg);
            if (event) toggleSelection(event);
            else if (svg.contains(domEvent.target)) applySelection(null, Boolean(selected));
        }

        function onKeyDown(domEvent) {
            var event = closestEvent(domEvent.target, svg);
            if (event && (domEvent.key === "Enter" || domEvent.key === " ")) {
                domEvent.preventDefault();
                toggleSelection(event);
            } else if (domEvent.key === "Escape" && selected) {
                domEvent.preventDefault();
                applySelection(null, true);
            }
        }

        host.addEventListener("mouseover", onMouseOver);
        host.addEventListener("mousemove", onMouseMove);
        host.addEventListener("mouseout", onMouseOut);
        host.addEventListener("focusin", onFocusIn);
        host.addEventListener("focusout", onFocusOut);
        host.addEventListener("click", onClick);
        host.addEventListener("keydown", onKeyDown);

        var controller = {
            svg: svg,
            clearSelection: function () {
                if (selected) applySelection(null, true);
            },
            selectedId: function () {
                if (!selected) return null;
                return data(selected, "segment-id") || data(selected, "link-id");
            },
            destroy: function () {
                host.removeEventListener("mouseover", onMouseOver);
                host.removeEventListener("mousemove", onMouseMove);
                host.removeEventListener("mouseout", onMouseOut);
                host.removeEventListener("focusin", onFocusIn);
                host.removeEventListener("focusout", onFocusOut);
                host.removeEventListener("click", onClick);
                host.removeEventListener("keydown", onKeyDown);
                events.forEach(function (event) {
                    event.classList.remove("is-hovered", "is-selected", "is-related", "is-dimmed");
                    event.removeAttribute("aria-pressed");
                    event.removeAttribute("aria-label");
                });
                if (tooltip.parentNode) tooltip.parentNode.removeChild(tooltip);
                host.classList.remove("circos-viewer");
                instances.delete(host);
            }
        };
        instances.set(host, controller);
        return controller;
    }

    function mount(container, svgText) {
        var host = requireElement(container, "container");
        var svg = parseSvg(svgText);
        while (host.firstChild) host.removeChild(host.firstChild);
        host.appendChild(svg);
        return attach(host, svg);
    }

    function load(container, source) {
        return fetch(source, {credentials: "same-origin"}).then(function (response) {
            if (!response.ok) throw new Error("Unable to load Circos SVG: " + response.status);
            return response.text();
        }).then(function (svgText) {
            return mount(container, svgText);
        });
    }

    function loadExamples() {
        var hosts = document.querySelectorAll("[data-circos-svg]");
        Array.prototype.forEach.call(hosts, function (host) {
            load(host, host.getAttribute("data-circos-svg")).catch(function (error) {
                host.textContent = error.message;
                host.classList.add("circos-viewer-error");
            });
        });
    }

    global.CircosViewer = Object.freeze({attach: attach, mount: mount, load: load});
    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", loadExamples);
    else loadExamples();
}(window));

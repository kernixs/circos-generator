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

    function tooltipText(svg, event) {
        var parts = [capitalize(data(event, "event-type"))];
        if (hasClass(event, "circos-segment")) {
            parts.push("chr" + data(event, "chromosome") + ":" + coordinate(data(event, "start"), true)
                    + "–" + coordinate(data(event, "end"), false));
            if (data(event, "copy-number")) parts.push("copy number " + data(event, "copy-number"));
        } else {
            parts.push("chr" + data(event, "source-chromosome") + ":"
                    + coordinate(data(event, "source-position"), true)
                    + " ↔ chr" + data(event, "target-chromosome") + ":"
                    + coordinate(data(event, "target-position"), true));
            if (data(event, "aggregate-event-count")) {
                parts.push(data(event, "aggregate-event-count") + " events");
            }
            if (data(event, "aggregate-patient-count")) {
                parts.push(data(event, "aggregate-patient-count") + " patients");
            }
            if (data(event, "aggregate-sample-count")) {
                parts.push(data(event, "aggregate-sample-count") + " samples");
            }
        }
        if (data(event, "confidence")) parts.push(data(event, "confidence") + " confidence");
        return parts.join(" · ");
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
            selectedSourceResultIds: selected && data(selected, "source-result-id")
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
        if (!hasClass(svg, "circos-plot") || svg.getAttribute("data-contract-version") !== "1.0") {
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
            tooltip.style.left = Math.max(8, clientX - bounds.left + 12) + "px";
            tooltip.style.top = Math.max(8, clientY - bounds.top + 12) + "px";
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

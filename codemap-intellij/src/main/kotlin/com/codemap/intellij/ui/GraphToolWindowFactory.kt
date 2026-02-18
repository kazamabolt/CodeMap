package com.codemap.intellij.ui

import com.codemap.intellij.model.AnalysisResult
import com.google.gson.Gson
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.content.ContentFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Registers the CodeMap tool window with a JCEF-based graph panel.
 */
class GraphToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = GraphPanel(project)
        val content = ContentFactory.getInstance().createContent(panel.component, "Graph", false)
        toolWindow.contentManager.addContent(content)
        panels[project] = panel
    }

    companion object {
        private val panels = ConcurrentHashMap<Project, GraphPanel>()

        fun updateGraph(project: Project, result: AnalysisResult) {
            panels[project]?.updateGraph(result)
        }
    }
}

/**
 * JCEF-based graph visualization panel using Cytoscape.js.
 */
class GraphPanel(private val project: Project) {

    private val browser: JBCefBrowser = JBCefBrowser()
    private val gson = Gson()

    val component get() = browser.component

    init {
        browser.loadHTML(getHtmlContent())
    }

    fun updateGraph(result: AnalysisResult) {
        val json = gson.toJson(result)
        // Escape for JavaScript string
        val escaped = json
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        browser.cefBrowser.executeJavaScript(
            "if (typeof updateGraphData === 'function') { updateGraphData(JSON.parse('$escaped')); }",
            browser.cefBrowser.url, 0
        )
    }

    private fun getHtmlContent(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CodeMap Graph</title>
    <script src="https://unpkg.com/cytoscape@3.30.4/dist/cytoscape.min.js"></script>
    <script src="https://unpkg.com/dagre@0.8.5/dist/dagre.min.js"></script>
    <script src="https://unpkg.com/cytoscape-dagre@2.5.0/cytoscape-dagre.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'JetBrains Mono', 'Segoe UI', monospace;
            background: #2b2b2b;
            color: #a9b7c6;
            overflow: hidden;
            height: 100vh;
        }
        #toolbar {
            display: flex; align-items: center; gap: 8px;
            padding: 6px 12px; background: #3c3f41;
            border-bottom: 1px solid #515151;
        }
        #toolbar label { font-size: 11px; color: #999; }
        #toolbar select, #toolbar input {
            background: #45494a; color: #a9b7c6; border: 1px solid #646464;
            padding: 2px 6px; font-size: 11px; border-radius: 2px;
        }
        #toolbar button {
            background: #4a6da7; color: #fff; border: none;
            padding: 3px 8px; font-size: 11px; border-radius: 2px; cursor: pointer;
        }
        #toolbar button:hover { background: #5a7db7; }
        #stats { font-size: 10px; color: #777; margin-left: auto; }
        #cy { width: 100%; height: calc(100vh - 34px); }
        #tooltip {
            position: absolute; display: none; background: #3c3f41;
            border: 1px solid #515151; padding: 8px 12px; border-radius: 3px;
            font-size: 11px; max-width: 350px; z-index: 1000;
            box-shadow: 0 4px 12px rgba(0,0,0,0.5); pointer-events: none;
        }
        #tooltip .title { font-weight: bold; margin-bottom: 4px; color: #ffc66d; }
        #tooltip .detail { color: #a9b7c6; margin: 1px 0; white-space: pre-line; }
        #loading {
            position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
            font-size: 14px; color: #777;
            display: flex; flex-direction: column; align-items: center; gap: 10px;
        }
        .spinner {
            width: 28px; height: 28px; border: 3px solid #515151;
            border-top: 3px solid #4a6da7; border-radius: 50%;
            animation: spin 0.8s linear infinite;
        }
        @keyframes spin { to { transform: rotate(360deg); } }
    </style>
</head>
<body>
    <div id="toolbar">
        <label>Layout:</label>
        <select id="layoutSelect">
            <option value="dagre">Hierarchical</option>
            <option value="cose">Force-Directed</option>
            <option value="breadthfirst">Breadth-First</option>
            <option value="circle">Circle</option>
        </select>
        <button id="fitBtn">⊞ Fit</button>
        <button id="resetBtn">↺ Reset</button>
        <button id="collapseBtn">▼ Collapse</button>
        <span id="stats"></span>
    </div>
    <div id="cy"></div>
    <div id="tooltip"><div class="title" id="tooltipTitle"></div><div class="detail" id="tooltipDetail"></div></div>
    <div id="loading"><div class="spinner"></div><span>Waiting for analysis...</span></div>

    <script>
        let cy = null;
        const nodeColors = {
            CLASS: '#6897bb', INTERFACE: '#6a8759', ENUM: '#cc7832',
            METHOD: '#9876aa', CONSTRUCTOR: '#b5585a', PACKAGE: '#808080'
        };
        const nodeShapes = {
            CLASS: 'round-rectangle', INTERFACE: 'diamond', ENUM: 'hexagon',
            METHOD: 'ellipse', CONSTRUCTOR: 'ellipse', PACKAGE: 'rectangle'
        };
        const edgeColors = {
            CALLS: '#888', EXTENDS: '#6897bb', IMPLEMENTS: '#6a8759',
            DEPENDENCY: '#cc7832', CONTAINS: '#444', IMPORTS: '#555', OVERRIDES: '#9876aa'
        };

        function updateGraphData(data) {
            document.getElementById('loading').style.display = 'none';
            const elements = [];
            data.graph.nodes.forEach(n => elements.push({
                data: { id: n.id, label: n.name, qualifiedName: n.qualifiedName, nodeType: n.type,
                        filePath: n.filePath, lineNumber: n.lineNumber, metadata: n.metadata || {} }
            }));
            data.graph.edges.forEach(e => elements.push({
                data: { id: e.id, source: e.source, target: e.target, edgeType: e.type, label: e.type.toLowerCase() }
            }));

            if (cy) cy.destroy();
            cy = cytoscape({
                container: document.getElementById('cy'),
                elements: elements,
                style: [
                    { selector: 'node', style: {
                        'label': 'data(label)', 'text-valign': 'center', 'text-halign': 'center',
                        'font-size': '10px', 'color': '#fff', 'text-outline-color': '#2b2b2b',
                        'text-outline-width': 1, 'padding': '6px', 'width': 'label', 'height': '28px',
                        'background-opacity': 0.9, 'border-width': 2
                    }},
                    ...Object.entries(nodeColors).map(([t, c]) => ({
                        selector: 'node[nodeType="'+t+'"]',
                        style: { 'background-color': c, 'border-color': c, 'shape': nodeShapes[t] || 'ellipse' }
                    })),
                    { selector: 'edge', style: {
                        'width': 1.5, 'line-color': '#555', 'target-arrow-color': '#555',
                        'target-arrow-shape': 'triangle', 'curve-style': 'bezier', 'arrow-scale': 0.8
                    }},
                    ...Object.entries(edgeColors).map(([t, c]) => ({
                        selector: 'edge[edgeType="'+t+'"]',
                        style: { 'line-color': c, 'target-arrow-color': c,
                                 'line-style': t === 'IMPLEMENTS' ? 'dashed' : 'solid' }
                    })),
                    { selector: ':selected', style: { 'border-width': 3, 'border-color': '#fff' } },
                    { selector: '.faded', style: { 'opacity': 0.15 } }
                ],
                layout: { name: 'dagre', rankDir: 'TB', nodeSep: 50, rankSep: 70 },
                minZoom: 0.1, maxZoom: 5, wheelSensitivity: 0.3
            });

            cy.on('mouseover', 'node', evt => {
                const d = evt.target.data();
                document.getElementById('tooltipTitle').textContent = d.qualifiedName || d.label;
                let det = 'Type: ' + d.nodeType;
                if (d.metadata) Object.entries(d.metadata).forEach(([k,v]) => det += '\n' + k + ': ' + v);
                if (d.filePath) det += '\nFile: ' + d.filePath;
                if (d.lineNumber) det += '\nLine: ' + d.lineNumber;
                document.getElementById('tooltipDetail').textContent = det;
                document.getElementById('tooltip').style.display = 'block';
                cy.elements().addClass('faded');
                evt.target.removeClass('faded');
                evt.target.connectedEdges().removeClass('faded').connectedNodes().removeClass('faded');
            });
            cy.on('mouseout', 'node', () => {
                document.getElementById('tooltip').style.display = 'none';
                cy.elements().removeClass('faded');
            });
            cy.on('mousemove', evt => {
                const t = document.getElementById('tooltip');
                t.style.left = (evt.originalEvent.clientX + 12) + 'px';
                t.style.top = (evt.originalEvent.clientY + 12) + 'px';
            });

            if (data.stats) {
                document.getElementById('stats').textContent =
                    data.stats.graphNodes + ' nodes · ' + data.stats.graphEdges + ' edges · ' +
                    data.stats.totalClassesParsed + ' classes · ' + (data.analysisTimeMs || 0) + 'ms';
            }
        }

        document.getElementById('fitBtn').addEventListener('click', () => cy && cy.fit());
        document.getElementById('resetBtn').addEventListener('click', () => cy && cy.reset());
        document.getElementById('layoutSelect').addEventListener('change', e => {
            if (!cy) return;
            const l = e.target.value;
            cy.layout(l === 'dagre' ? {name:'dagre',rankDir:'TB',nodeSep:50,rankSep:70} : {name:l,animate:true,animationDuration:500}).run();
        });
        document.getElementById('collapseBtn').addEventListener('click', () => {
            if (!cy) return;
            const m = cy.nodes('[nodeType="METHOD"], [nodeType="CONSTRUCTOR"]');
            if (m.first().visible()) { m.hide(); document.getElementById('collapseBtn').textContent = '▶ Expand'; }
            else { m.show(); document.getElementById('collapseBtn').textContent = '▼ Collapse'; }
        });
    </script>
</body>
</html>
    """.trimIndent()
}

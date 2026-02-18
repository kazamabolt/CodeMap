import * as vscode from 'vscode';
import { AnalysisResult } from '../types/graph';

/**
 * Manages the WebView panel for graph visualization.
 * Uses Cytoscape.js for interactive graph rendering.
 */
export class GraphPanel {
    public static currentPanel: GraphPanel | undefined;
    private readonly panel: vscode.WebviewPanel;
    private readonly extensionUri: vscode.Uri;
    private disposables: vscode.Disposable[] = [];

    private constructor(panel: vscode.WebviewPanel, extensionUri: vscode.Uri) {
        this.panel = panel;
        this.extensionUri = extensionUri;

        this.panel.onDidDispose(() => this.dispose(), null, this.disposables);
        this.panel.webview.html = this.getHtmlContent();

        // Handle messages from WebView (e.g., click-to-navigate)
        this.panel.webview.onDidReceiveMessage(
            (message: { command: string; filePath?: string; lineNumber?: number }) => {
                if (message.command === 'navigateToSource' && message.filePath) {
                    const uri = vscode.Uri.file(message.filePath);
                    const line = (message.lineNumber || 1) - 1;
                    vscode.window.showTextDocument(uri, {
                        selection: new vscode.Range(line, 0, line, 0),
                        preview: false
                    });
                }
            },
            null,
            this.disposables
        );
    }

    /**
     * Create or reveal the graph panel.
     */
    public static createOrShow(extensionUri: vscode.Uri, title: string): GraphPanel {
        const column = vscode.ViewColumn.Beside;

        if (GraphPanel.currentPanel) {
            GraphPanel.currentPanel.panel.reveal(column);
            GraphPanel.currentPanel.panel.title = title;
            return GraphPanel.currentPanel;
        }

        const panel = vscode.window.createWebviewPanel(
            'codemapGraph',
            title,
            column,
            {
                enableScripts: true,
                retainContextWhenHidden: true,
                localResourceRoots: [vscode.Uri.joinPath(extensionUri, 'media')]
            }
        );

        GraphPanel.currentPanel = new GraphPanel(panel, extensionUri);
        return GraphPanel.currentPanel;
    }

    /**
     * Send analysis result to the WebView for rendering.
     */
    public updateGraph(result: AnalysisResult): void {
        this.panel.webview.postMessage({
            command: 'updateGraph',
            data: result
        });
    }

    private dispose(): void {
        GraphPanel.currentPanel = undefined;
        this.panel.dispose();
        while (this.disposables.length) {
            const disposable = this.disposables.pop();
            if (disposable) {
                disposable.dispose();
            }
        }
    }

    private getHtmlContent(): string {
        return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CodeMap Graph</title>
    <script src="https://unpkg.com/cytoscape@3.30.4/dist/cytoscape.min.js"></script>
    <script src="https://unpkg.com/cytoscape-dagre@2.5.0/cytoscape-dagre.js"></script>
    <script src="https://unpkg.com/dagre@0.8.5/dist/dagre.min.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: var(--vscode-editor-background, #1e1e1e);
            color: var(--vscode-editor-foreground, #d4d4d4);
            overflow: hidden;
            height: 100vh;
        }

        #toolbar {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 8px 12px;
            background: var(--vscode-sideBar-background, #252526);
            border-bottom: 1px solid var(--vscode-panel-border, #3c3c3c);
            flex-wrap: wrap;
        }

        #toolbar label { font-size: 12px; color: var(--vscode-descriptionForeground, #999); }

        #toolbar select, #toolbar input {
            background: var(--vscode-input-background, #3c3c3c);
            color: var(--vscode-input-foreground, #ccc);
            border: 1px solid var(--vscode-input-border, #555);
            padding: 3px 6px;
            font-size: 12px;
            border-radius: 3px;
        }

        #toolbar button {
            background: var(--vscode-button-background, #0e639c);
            color: var(--vscode-button-foreground, #fff);
            border: none;
            padding: 4px 10px;
            font-size: 12px;
            border-radius: 3px;
            cursor: pointer;
        }

        #toolbar button:hover {
            background: var(--vscode-button-hoverBackground, #1177bb);
        }

        #stats {
            font-size: 11px;
            color: var(--vscode-descriptionForeground, #888);
            margin-left: auto;
        }

        #cy {
            width: 100%;
            height: calc(100vh - 44px);
        }

        #tooltip {
            position: absolute;
            display: none;
            background: var(--vscode-editorHoverWidget-background, #2d2d2d);
            border: 1px solid var(--vscode-editorHoverWidget-border, #454545);
            padding: 8px 12px;
            border-radius: 4px;
            font-size: 12px;
            max-width: 350px;
            z-index: 1000;
            box-shadow: 0 4px 12px rgba(0,0,0,0.4);
            pointer-events: none;
        }

        #tooltip .title { font-weight: bold; margin-bottom: 4px; color: var(--vscode-editor-foreground, #fff); }
        #tooltip .detail { color: var(--vscode-descriptionForeground, #aaa); margin: 2px 0; }

        #loading {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            font-size: 16px;
            color: var(--vscode-descriptionForeground, #999);
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 12px;
        }

        .spinner {
            width: 32px; height: 32px;
            border: 3px solid var(--vscode-panel-border, #3c3c3c);
            border-top: 3px solid var(--vscode-button-background, #0e639c);
            border-radius: 50%;
            animation: spin 0.8s linear infinite;
        }

        @keyframes spin { to { transform: rotate(360deg); } }
    </style>
</head>
<body>
    <div id="toolbar">
        <label>Depth:</label>
        <input type="number" id="depthFilter" value="5" min="1" max="20" style="width:50px">
        <label>Layout:</label>
        <select id="layoutSelect">
            <option value="dagre">Hierarchical</option>
            <option value="cose">Force-Directed</option>
            <option value="breadthfirst">Breadth-First</option>
            <option value="circle">Circle</option>
        </select>
        <button id="fitBtn" title="Fit to screen">⊞ Fit</button>
        <button id="resetBtn" title="Reset view">↺ Reset</button>
        <button id="collapseBtn" title="Collapse all">▼ Collapse</button>
        <span id="stats"></span>
    </div>
    <div id="cy"></div>
    <div id="tooltip">
        <div class="title" id="tooltipTitle"></div>
        <div class="detail" id="tooltipDetail"></div>
    </div>
    <div id="loading">
        <div class="spinner"></div>
        <span>Waiting for analysis...</span>
    </div>

    <script>
        const vscode = acquireVsCodeApi();
        let cy = null;

        const nodeColors = {
            CLASS: '#4fc3f7',
            INTERFACE: '#81c784',
            ENUM: '#ffb74d',
            METHOD: '#ce93d8',
            CONSTRUCTOR: '#f48fb1',
            PACKAGE: '#90a4ae'
        };

        const edgeColors = {
            CALLS: '#aaa',
            EXTENDS: '#4fc3f7',
            IMPLEMENTS: '#81c784',
            DEPENDENCY: '#ffb74d',
            CONTAINS: '#555',
            IMPORTS: '#666',
            OVERRIDES: '#ce93d8'
        };

        const nodeShapes = {
            CLASS: 'round-rectangle',
            INTERFACE: 'diamond',
            ENUM: 'hexagon',
            METHOD: 'ellipse',
            CONSTRUCTOR: 'ellipse',
            PACKAGE: 'rectangle'
        };

        function initCytoscape(graphData) {
            document.getElementById('loading').style.display = 'none';

            const elements = [];

            // Add nodes
            graphData.graph.nodes.forEach(node => {
                elements.push({
                    data: {
                        id: node.id,
                        label: node.name,
                        qualifiedName: node.qualifiedName,
                        nodeType: node.type,
                        filePath: node.filePath,
                        lineNumber: node.lineNumber,
                        metadata: node.metadata || {}
                    }
                });
            });

            // Add edges
            graphData.graph.edges.forEach(edge => {
                elements.push({
                    data: {
                        id: edge.id,
                        source: edge.source,
                        target: edge.target,
                        edgeType: edge.type,
                        label: edge.type.toLowerCase()
                    }
                });
            });

            if (cy) cy.destroy();

            cy = cytoscape({
                container: document.getElementById('cy'),
                elements: elements,
                style: [
                    {
                        selector: 'node',
                        style: {
                            'label': 'data(label)',
                            'text-valign': 'center',
                            'text-halign': 'center',
                            'font-size': '11px',
                            'font-family': '-apple-system, sans-serif',
                            'color': '#fff',
                            'text-outline-color': '#333',
                            'text-outline-width': 1,
                            'padding': '8px',
                            'width': 'label',
                            'height': '32px',
                            'background-opacity': 0.9,
                            'border-width': 2,
                            'border-opacity': 0.8
                        }
                    },
                    ...Object.entries(nodeColors).map(([type, color]) => ({
                        selector: 'node[nodeType="' + type + '"]',
                        style: {
                            'background-color': color,
                            'border-color': color,
                            'shape': nodeShapes[type] || 'ellipse'
                        }
                    })),
                    {
                        selector: 'edge',
                        style: {
                            'width': 1.5,
                            'line-color': '#666',
                            'target-arrow-color': '#666',
                            'target-arrow-shape': 'triangle',
                            'curve-style': 'bezier',
                            'arrow-scale': 0.8,
                            'font-size': '9px',
                            'color': '#888',
                            'text-background-color': '#1e1e1e',
                            'text-background-opacity': 0.8,
                            'text-background-padding': '2px'
                        }
                    },
                    ...Object.entries(edgeColors).map(([type, color]) => ({
                        selector: 'edge[edgeType="' + type + '"]',
                        style: {
                            'line-color': color,
                            'target-arrow-color': color,
                            'line-style': type === 'IMPLEMENTS' ? 'dashed' : 'solid'
                        }
                    })),
                    {
                        selector: ':selected',
                        style: {
                            'border-width': 3,
                            'border-color': '#fff',
                            'background-color': '#f44336'
                        }
                    },
                    {
                        selector: '.highlighted',
                        style: {
                            'background-color': '#ff9800',
                            'border-color': '#ff9800',
                            'z-index': 999
                        }
                    },
                    {
                        selector: '.faded',
                        style: {
                            'opacity': 0.2
                        }
                    }
                ],
                layout: { name: 'dagre', rankDir: 'TB', nodeSep: 50, rankSep: 80 },
                minZoom: 0.1,
                maxZoom: 5,
                wheelSensitivity: 0.3
            });

            // Click-to-navigate
            cy.on('tap', 'node', function(evt) {
                const data = evt.target.data();
                if (data.filePath && data.lineNumber) {
                    vscode.postMessage({
                        command: 'navigateToSource',
                        filePath: data.filePath,
                        lineNumber: data.lineNumber
                    });
                }
            });

            // Hover tooltip
            const tooltip = document.getElementById('tooltip');
            cy.on('mouseover', 'node', function(evt) {
                const data = evt.target.data();
                document.getElementById('tooltipTitle').textContent = data.qualifiedName || data.label;
                let details = 'Type: ' + data.nodeType;
                if (data.metadata) {
                    Object.entries(data.metadata).forEach(([k, v]) => {
                        details += '\\n' + k + ': ' + v;
                    });
                }
                if (data.filePath) details += '\\nFile: ' + data.filePath;
                if (data.lineNumber) details += '\\nLine: ' + data.lineNumber;
                document.getElementById('tooltipDetail').textContent = details;
                tooltip.style.display = 'block';

                // Highlight connected
                cy.elements().addClass('faded');
                evt.target.removeClass('faded').addClass('highlighted');
                evt.target.connectedEdges().removeClass('faded');
                evt.target.connectedEdges().connectedNodes().removeClass('faded');
            });

            cy.on('mouseout', 'node', function() {
                tooltip.style.display = 'none';
                cy.elements().removeClass('faded highlighted');
            });

            cy.on('mousemove', function(evt) {
                tooltip.style.left = (evt.originalEvent.clientX + 12) + 'px';
                tooltip.style.top = (evt.originalEvent.clientY + 12) + 'px';
            });

            // Update stats
            document.getElementById('stats').textContent =
                graphData.stats.graphNodes + ' nodes · ' +
                graphData.stats.graphEdges + ' edges · ' +
                graphData.stats.totalClassesParsed + ' classes · ' +
                graphData.analysisTimeMs + 'ms';
        }

        // Toolbar handlers
        document.getElementById('fitBtn').addEventListener('click', () => cy && cy.fit());
        document.getElementById('resetBtn').addEventListener('click', () => cy && cy.reset());
        document.getElementById('layoutSelect').addEventListener('change', (e) => {
            if (!cy) return;
            const layout = e.target.value;
            const opts = layout === 'dagre'
                ? { name: 'dagre', rankDir: 'TB', nodeSep: 50, rankSep: 80 }
                : { name: layout, animate: true, animationDuration: 500 };
            cy.layout(opts).run();
        });
        document.getElementById('collapseBtn').addEventListener('click', () => {
            if (!cy) return;
            // Toggle: hide/show METHOD nodes
            const methods = cy.nodes('[nodeType="METHOD"], [nodeType="CONSTRUCTOR"]');
            if (methods.first().visible()) {
                methods.hide();
                document.getElementById('collapseBtn').textContent = '▶ Expand';
            } else {
                methods.show();
                document.getElementById('collapseBtn').textContent = '▼ Collapse';
            }
        });

        // Receive messages from extension
        window.addEventListener('message', event => {
            const message = event.data;
            if (message.command === 'updateGraph') {
                initCytoscape(message.data);
            }
        });
    </script>
</body>
</html>`;
    }
}

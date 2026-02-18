/**
 * Type definitions for CodeMap graph data structures.
 * Matches the JSON output from the core engine CLI.
 */

export interface GraphNode {
    id: string;
    name: string;
    qualifiedName: string;
    type: 'CLASS' | 'INTERFACE' | 'ENUM' | 'METHOD' | 'CONSTRUCTOR' | 'PACKAGE';
    filePath?: string;
    lineNumber?: number;
    metadata?: Record<string, string>;
}

export interface GraphEdge {
    id: string;
    source: string;
    target: string;
    type: 'CALLS' | 'EXTENDS' | 'IMPLEMENTS' | 'DEPENDENCY' | 'IMPORTS' | 'OVERRIDES' | 'CONTAINS';
    metadata?: Record<string, string>;
}

export interface CodeGraph {
    nodes: GraphNode[];
    edges: GraphEdge[];
}

export interface AnalysisResult {
    command: string;
    target: string;
    timestamp: string;
    analysisTimeMs: number;
    stats: {
        totalClassesParsed: number;
        totalMethodsParsed: number;
        graphNodes: number;
        graphEdges: number;
    };
    graph: CodeGraph;
}

export interface GraphFilter {
    maxDepth?: number;
    includePackages?: string[];
    excludePackages?: string[];
    hideLibraryClasses?: boolean;
    nodeTypes?: string[];
    edgeTypes?: string[];
}

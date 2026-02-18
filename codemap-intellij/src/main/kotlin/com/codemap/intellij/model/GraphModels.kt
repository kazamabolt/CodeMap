package com.codemap.intellij.model

/**
 * Data models matching the JSON output from the core engine.
 */

data class GraphNode(
    val id: String,
    val name: String,
    val qualifiedName: String?,
    val type: String,
    val filePath: String?,
    val lineNumber: Int?,
    val metadata: Map<String, String>?
)

data class GraphEdge(
    val id: String,
    val source: String,
    val target: String,
    val type: String,
    val metadata: Map<String, String>?
)

data class CodeGraph(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>
)

data class AnalysisStats(
    val totalClassesParsed: Int,
    val totalMethodsParsed: Int,
    val graphNodes: Int,
    val graphEdges: Int
)

data class AnalysisResult(
    val command: String,
    val target: String?,
    val timestamp: String?,
    val analysisTimeMs: Long?,
    val stats: AnalysisStats?,
    val graph: CodeGraph
)

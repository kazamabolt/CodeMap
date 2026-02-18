package com.codemap.graph;

import com.codemap.model.*;

import java.util.*;

/**
 * Provides graph traversal and query utilities on a CodeGraph.
 */
public class GraphQuery {

    private final CodeGraph graph;

    public GraphQuery(CodeGraph graph) {
        this.graph = graph;
    }

    /**
     * BFS traversal from a start node, following edges of the specified types,
     * limited by depth.
     *
     * @param startNodeId starting node ID
     * @param maxDepth    max traversal depth (-1 for unlimited)
     * @param edgeTypes   edge types to follow (null/empty = all)
     * @param forward     true = follow outgoing, false = follow incoming
     * @return subgraph of reachable nodes and edges
     */
    public CodeGraph traverse(String startNodeId, int maxDepth, Set<EdgeType> edgeTypes, boolean forward) {
        Set<String> visited = new LinkedHashSet<>();
        Set<String> edgeIds = new LinkedHashSet<>();
        Queue<String> queue = new LinkedList<>();
        Map<String, Integer> depth = new HashMap<>();

        queue.add(startNodeId);
        visited.add(startNodeId);
        depth.put(startNodeId, 0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDepth = depth.get(current);

            if (maxDepth >= 0 && currentDepth >= maxDepth)
                continue;

            List<GraphEdge> edges = forward
                    ? graph.getOutgoingEdges(current)
                    : graph.getIncomingEdges(current);

            for (GraphEdge edge : edges) {
                if (edgeTypes != null && !edgeTypes.isEmpty() && !edgeTypes.contains(edge.getType())) {
                    continue;
                }

                String neighbor = forward ? edge.getTargetId() : edge.getSourceId();
                edgeIds.add(edge.getId());

                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    depth.put(neighbor, currentDepth + 1);
                    queue.add(neighbor);
                }
            }
        }

        return graph.subgraph(visited);
    }

    /**
     * Forward traversal: outgoing edges from start node.
     */
    public CodeGraph forwardTraverse(String startNodeId, int maxDepth, Set<EdgeType> edgeTypes) {
        return traverse(startNodeId, maxDepth, edgeTypes, true);
    }

    /**
     * Reverse traversal: incoming edges to start node.
     */
    public CodeGraph reverseTraverse(String startNodeId, int maxDepth, Set<EdgeType> edgeTypes) {
        return traverse(startNodeId, maxDepth, edgeTypes, false);
    }

    /**
     * Filter graph to include only nodes matching the given package prefix.
     */
    public CodeGraph filterByPackage(String packagePrefix) {
        Set<String> matchingIds = new HashSet<>();
        for (GraphNode node : graph.getNodes()) {
            String qn = node.getQualifiedName();
            if (qn != null && qn.startsWith(packagePrefix)) {
                matchingIds.add(node.getId());
            }
        }
        return graph.subgraph(matchingIds);
    }

    /**
     * Exclude nodes that match the given package prefix (e.g., library classes).
     */
    public CodeGraph excludePackage(String packagePrefix) {
        Set<String> matchingIds = new HashSet<>();
        for (GraphNode node : graph.getNodes()) {
            String qn = node.getQualifiedName();
            if (qn == null || !qn.startsWith(packagePrefix)) {
                matchingIds.add(node.getId());
            }
        }
        return graph.subgraph(matchingIds);
    }
}

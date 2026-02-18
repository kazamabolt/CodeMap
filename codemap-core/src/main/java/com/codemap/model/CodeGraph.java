package com.codemap.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Container for the complete code graph — all nodes and edges.
 * Provides index-based lookups and subgraph extraction.
 */
public class CodeGraph {

    private final List<GraphNode> nodes;
    private final List<GraphEdge> edges;
    private final Map<String, GraphNode> nodeIndex;
    private final Map<String, List<GraphEdge>> outgoingEdges;
    private final Map<String, List<GraphEdge>> incomingEdges;

    public CodeGraph(List<GraphNode> nodes, List<GraphEdge> edges) {
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        this.edges = Collections.unmodifiableList(new ArrayList<>(edges));

        this.nodeIndex = new HashMap<>();
        for (GraphNode node : nodes) {
            nodeIndex.put(node.getId(), node);
        }

        this.outgoingEdges = new HashMap<>();
        this.incomingEdges = new HashMap<>();
        for (GraphEdge edge : edges) {
            outgoingEdges.computeIfAbsent(edge.getSourceId(), k -> new ArrayList<>()).add(edge);
            incomingEdges.computeIfAbsent(edge.getTargetId(), k -> new ArrayList<>()).add(edge);
        }
    }

    public List<GraphNode> getNodes() {
        return nodes;
    }

    public List<GraphEdge> getEdges() {
        return edges;
    }

    public Optional<GraphNode> getNode(String id) {
        return Optional.ofNullable(nodeIndex.get(id));
    }

    public List<GraphNode> getNodesByType(NodeType type) {
        return nodes.stream().filter(n -> n.getType() == type).collect(Collectors.toList());
    }

    public List<GraphEdge> getOutgoingEdges(String nodeId) {
        return outgoingEdges.getOrDefault(nodeId, Collections.emptyList());
    }

    public List<GraphEdge> getIncomingEdges(String nodeId) {
        return incomingEdges.getOrDefault(nodeId, Collections.emptyList());
    }

    public List<GraphEdge> getEdgesByType(EdgeType type) {
        return edges.stream().filter(e -> e.getType() == type).collect(Collectors.toList());
    }

    /**
     * Extract a subgraph containing only the specified node IDs and edges between
     * them.
     */
    public CodeGraph subgraph(Set<String> nodeIds) {
        List<GraphNode> subNodes = nodes.stream()
                .filter(n -> nodeIds.contains(n.getId()))
                .collect(Collectors.toList());
        List<GraphEdge> subEdges = edges.stream()
                .filter(e -> nodeIds.contains(e.getSourceId()) && nodeIds.contains(e.getTargetId()))
                .collect(Collectors.toList());
        return new CodeGraph(subNodes, subEdges);
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    @Override
    public String toString() {
        return String.format("CodeGraph{nodes=%d, edges=%d}", nodes.size(), edges.size());
    }
}

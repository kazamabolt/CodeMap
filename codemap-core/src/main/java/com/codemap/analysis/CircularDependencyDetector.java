package com.codemap.analysis;

import com.codemap.model.*;

import java.util.*;

/**
 * Detects circular dependencies between classes using Tarjan's SCC algorithm.
 */
public class CircularDependencyDetector {

    private final CodeGraph graph;

    public CircularDependencyDetector(CodeGraph graph) {
        this.graph = graph;
    }

    /**
     * Find all strongly connected components (circular dependencies) in the class
     * dependency graph.
     *
     * @return list of SCCs (each SCC is a list of class node IDs involved in the
     *         cycle)
     */
    public List<List<String>> detectCircularDependencies() {
        // Build adjacency from class-level edges only
        Set<EdgeType> classEdges = EnumSet.of(EdgeType.DEPENDENCY, EdgeType.EXTENDS, EdgeType.IMPLEMENTS);
        List<GraphNode> classNodes = new ArrayList<>();
        classNodes.addAll(graph.getNodesByType(NodeType.CLASS));
        classNodes.addAll(graph.getNodesByType(NodeType.INTERFACE));
        classNodes.addAll(graph.getNodesByType(NodeType.ENUM));

        Set<String> classIds = new HashSet<>();
        for (GraphNode node : classNodes) {
            classIds.add(node.getId());
        }

        // Tarjan's algorithm
        Map<String, Integer> index = new HashMap<>();
        Map<String, Integer> lowLink = new HashMap<>();
        Map<String, Boolean> onStack = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        List<List<String>> sccs = new ArrayList<>();
        int[] counter = { 0 };

        for (GraphNode node : classNodes) {
            if (!index.containsKey(node.getId())) {
                strongConnect(node.getId(), index, lowLink, onStack, stack, sccs, counter, classIds, classEdges);
            }
        }

        // Filter out single-node SCCs (they aren't cycles)
        List<List<String>> cycles = new ArrayList<>();
        for (List<String> scc : sccs) {
            if (scc.size() > 1) {
                cycles.add(scc);
            }
        }
        return cycles;
    }

    private void strongConnect(String nodeId,
            Map<String, Integer> index,
            Map<String, Integer> lowLink,
            Map<String, Boolean> onStack,
            Deque<String> stack,
            List<List<String>> sccs,
            int[] counter,
            Set<String> classIds,
            Set<EdgeType> classEdges) {
        index.put(nodeId, counter[0]);
        lowLink.put(nodeId, counter[0]);
        counter[0]++;
        stack.push(nodeId);
        onStack.put(nodeId, true);

        for (GraphEdge edge : graph.getOutgoingEdges(nodeId)) {
            if (!classEdges.contains(edge.getType()))
                continue;
            String target = edge.getTargetId();
            if (!classIds.contains(target))
                continue;

            if (!index.containsKey(target)) {
                strongConnect(target, index, lowLink, onStack, stack, sccs, counter, classIds, classEdges);
                lowLink.put(nodeId, Math.min(lowLink.get(nodeId), lowLink.get(target)));
            } else if (onStack.getOrDefault(target, false)) {
                lowLink.put(nodeId, Math.min(lowLink.get(nodeId), index.get(target)));
            }
        }

        if (lowLink.get(nodeId).equals(index.get(nodeId))) {
            List<String> scc = new ArrayList<>();
            String w;
            do {
                w = stack.pop();
                onStack.put(w, false);
                scc.add(w);
            } while (!w.equals(nodeId));
            sccs.add(scc);
        }
    }
}

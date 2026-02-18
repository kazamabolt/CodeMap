package com.codemap.rules;

import com.codemap.model.*;

import java.util.*;

/**
 * Detects deep inheritance chains beyond a configurable threshold.
 */
public class DeepInheritanceRule implements ArchitectureRule {

    private int maxDepth = 4;

    @Override
    public String getName() {
        return "deep-inheritance";
    }

    @Override
    public String getDescription() {
        return "Detects inheritance chains deeper than the configured threshold";
    }

    @Override
    public void configure(Map<String, Object> config) {
        if (config.containsKey("maxDepth")) {
            maxDepth = ((Number) config.get("maxDepth")).intValue();
        }
    }

    @Override
    public List<Violation> evaluate(CodeGraph graph) {
        List<Violation> violations = new ArrayList<>();
        List<GraphNode> classNodes = graph.getNodesByType(NodeType.CLASS);

        for (GraphNode node : classNodes) {
            int depth = computeInheritanceDepth(node.getId(), graph, new HashSet<>());
            if (depth > maxDepth) {
                violations.add(new Violation(
                        getName(), "WARNING",
                        String.format("Inheritance depth is %d (max: %d)", depth, maxDepth),
                        node.getId(), node.getFilePath(), node.getLineNumber()));
            }
        }
        return violations;
    }

    private int computeInheritanceDepth(String nodeId, CodeGraph graph, Set<String> visited) {
        if (visited.contains(nodeId))
            return 0; // cycle guard
        visited.add(nodeId);

        int maxChildDepth = 0;
        for (GraphEdge edge : graph.getOutgoingEdges(nodeId)) {
            if (edge.getType() == EdgeType.EXTENDS) {
                int childDepth = 1 + computeInheritanceDepth(edge.getTargetId(), graph, visited);
                maxChildDepth = Math.max(maxChildDepth, childDepth);
            }
        }
        return maxChildDepth;
    }
}

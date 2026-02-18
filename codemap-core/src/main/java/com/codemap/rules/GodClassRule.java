package com.codemap.rules;

import com.codemap.model.*;

import java.util.*;

/**
 * Detects "god classes" — classes with too many methods or dependencies.
 */
public class GodClassRule implements ArchitectureRule {

    private int maxMethods = 20;
    private int maxDependencies = 15;

    @Override
    public String getName() {
        return "god-class";
    }

    @Override
    public String getDescription() {
        return "Detects classes with too many methods or dependencies";
    }

    @Override
    public void configure(Map<String, Object> config) {
        if (config.containsKey("maxMethods")) {
            maxMethods = ((Number) config.get("maxMethods")).intValue();
        }
        if (config.containsKey("maxDependencies")) {
            maxDependencies = ((Number) config.get("maxDependencies")).intValue();
        }
    }

    @Override
    public List<Violation> evaluate(CodeGraph graph) {
        List<Violation> violations = new ArrayList<>();

        for (GraphNode node : graph.getNodesByType(NodeType.CLASS)) {
            // Count methods (CONTAINS edges to METHOD nodes)
            long methodCount = graph.getOutgoingEdges(node.getId()).stream()
                    .filter(e -> e.getType() == EdgeType.CONTAINS)
                    .count();

            // Count dependencies (DEPENDENCY edges outgoing)
            long depCount = graph.getOutgoingEdges(node.getId()).stream()
                    .filter(e -> e.getType() == EdgeType.DEPENDENCY)
                    .count();

            if (methodCount > maxMethods) {
                violations.add(new Violation(
                        getName(), "WARNING",
                        String.format("Class has %d methods (max: %d)", methodCount, maxMethods),
                        node.getId(), node.getFilePath(), node.getLineNumber()));
            }

            if (depCount > maxDependencies) {
                violations.add(new Violation(
                        getName(), "WARNING",
                        String.format("Class has %d dependencies (max: %d)", depCount, maxDependencies),
                        node.getId(), node.getFilePath(), node.getLineNumber()));
            }
        }
        return violations;
    }
}

package com.codemap.rules;

import com.codemap.model.*;

import java.util.*;

/**
 * Detects classes that are not referenced by any other class.
 */
public class UnusedClassRule implements ArchitectureRule {

    @Override
    public String getName() {
        return "unused-class";
    }

    @Override
    public String getDescription() {
        return "Detects classes with no incoming dependencies";
    }

    @Override
    public List<Violation> evaluate(CodeGraph graph) {
        List<Violation> violations = new ArrayList<>();

        List<GraphNode> classNodes = new ArrayList<>();
        classNodes.addAll(graph.getNodesByType(NodeType.CLASS));
        classNodes.addAll(graph.getNodesByType(NodeType.INTERFACE));

        for (GraphNode node : classNodes) {
            List<GraphEdge> incoming = graph.getIncomingEdges(node.getId());
            boolean hasExternalReference = incoming.stream()
                    .anyMatch(e -> e.getType() == EdgeType.DEPENDENCY
                            || e.getType() == EdgeType.EXTENDS
                            || e.getType() == EdgeType.IMPLEMENTS);

            if (!hasExternalReference) {
                violations.add(new Violation(
                        getName(), "INFO",
                        "Class has no incoming dependencies — may be unused",
                        node.getId(), node.getFilePath(), node.getLineNumber()));
            }
        }
        return violations;
    }
}

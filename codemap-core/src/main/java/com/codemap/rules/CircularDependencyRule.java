package com.codemap.rules;

import com.codemap.analysis.CircularDependencyDetector;
import com.codemap.model.CodeGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule that detects circular dependencies between classes.
 */
public class CircularDependencyRule implements ArchitectureRule {

    @Override
    public String getName() {
        return "circular-dependency";
    }

    @Override
    public String getDescription() {
        return "Detects circular dependencies between classes";
    }

    @Override
    public List<Violation> evaluate(CodeGraph graph) {
        CircularDependencyDetector detector = new CircularDependencyDetector(graph);
        List<List<String>> cycles = detector.detectCircularDependencies();

        List<Violation> violations = new ArrayList<>();
        for (List<String> cycle : cycles) {
            String cycleStr = String.join(" -> ", cycle);
            for (String nodeId : cycle) {
                graph.getNode(nodeId).ifPresent(node -> violations.add(new Violation(
                        getName(), "WARNING",
                        "Class is part of circular dependency: " + cycleStr,
                        nodeId, node.getFilePath(), node.getLineNumber())));
            }
        }
        return violations;
    }
}

package com.codemap.rules;

import com.codemap.model.*;

import java.util.*;

/**
 * Detects violations of layered architecture conventions.
 * Configurable layer order (e.g., controller → service → repository).
 */
public class LayerViolationRule implements ArchitectureRule {

    private List<String> layerOrder = List.of(
            "controller", "service", "repository", "model");

    @Override
    public String getName() {
        return "layer-violation";
    }

    @Override
    public String getDescription() {
        return "Detects violations of layered architecture conventions";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, Object> config) {
        if (config.containsKey("layerOrder")) {
            layerOrder = (List<String>) config.get("layerOrder");
        }
    }

    @Override
    public List<Violation> evaluate(CodeGraph graph) {
        List<Violation> violations = new ArrayList<>();

        for (GraphEdge edge : graph.getEdgesByType(EdgeType.DEPENDENCY)) {
            Optional<GraphNode> sourceOpt = graph.getNode(edge.getSourceId());
            Optional<GraphNode> targetOpt = graph.getNode(edge.getTargetId());

            if (sourceOpt.isEmpty() || targetOpt.isEmpty())
                continue;

            GraphNode source = sourceOpt.get();
            GraphNode target = targetOpt.get();

            int sourceLayer = getLayerIndex(source.getQualifiedName());
            int targetLayer = getLayerIndex(target.getQualifiedName());

            // Violation: lower layer depends on higher layer (e.g., repository →
            // controller)
            if (sourceLayer >= 0 && targetLayer >= 0 && sourceLayer > targetLayer) {
                violations.add(new Violation(
                        getName(), "ERROR",
                        String.format("Layer violation: '%s' (layer: %s) depends on '%s' (layer: %s)",
                                source.getName(), layerOrder.get(sourceLayer),
                                target.getName(), layerOrder.get(targetLayer)),
                        source.getId(), source.getFilePath(), source.getLineNumber()));
            }
        }
        return violations;
    }

    private int getLayerIndex(String qualifiedName) {
        if (qualifiedName == null)
            return -1;
        String lower = qualifiedName.toLowerCase();
        for (int i = 0; i < layerOrder.size(); i++) {
            if (lower.contains(layerOrder.get(i)))
                return i;
        }
        return -1;
    }
}

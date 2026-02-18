package com.codemap.analysis;

import com.codemap.graph.GraphQuery;
import com.codemap.model.*;

import java.util.*;

/**
 * Impact analysis — determines which classes/methods are affected
 * if a given class is modified.
 */
public class ImpactAnalyzer {

    private final CodeGraph graph;
    private final GraphQuery query;

    public ImpactAnalyzer(CodeGraph graph) {
        this.graph = graph;
        this.query = new GraphQuery(graph);
    }

    /**
     * Analyze the impact of modifying the given class.
     * Returns all classes that transitively depend on it.
     *
     * @param className fully qualified class name
     * @return subgraph of impacted classes
     */
    public CodeGraph getImpactAnalysis(String className) {
        String classId = resolveClassId(className);
        if (classId == null)
            return new CodeGraph(Collections.emptyList(), Collections.emptyList());

        // Reverse traverse: find everything that depends on this class
        Set<EdgeType> depEdges = EnumSet.of(
                EdgeType.DEPENDENCY, EdgeType.EXTENDS, EdgeType.IMPLEMENTS, EdgeType.CALLS);
        return query.reverseTraverse(classId, -1, depEdges);
    }

    /**
     * Get the direct impact count (number of classes directly affected).
     */
    public int getDirectImpactCount(String className) {
        String classId = resolveClassId(className);
        if (classId == null)
            return 0;

        Set<String> directDeps = new HashSet<>();
        for (GraphEdge edge : graph.getIncomingEdges(classId)) {
            if (edge.getType() == EdgeType.DEPENDENCY
                    || edge.getType() == EdgeType.EXTENDS
                    || edge.getType() == EdgeType.IMPLEMENTS) {
                directDeps.add(edge.getSourceId());
            }
        }
        return directDeps.size();
    }

    private String resolveClassId(String className) {
        String directId = "class:" + className;
        if (graph.getNode(directId).isPresent())
            return directId;

        for (GraphNode node : graph.getNodesByType(NodeType.CLASS)) {
            if (node.getQualifiedName() != null && node.getQualifiedName().endsWith(className)) {
                return node.getId();
            }
        }
        for (GraphNode node : graph.getNodesByType(NodeType.INTERFACE)) {
            if (node.getQualifiedName() != null && node.getQualifiedName().endsWith(className)) {
                return node.getId();
            }
        }
        return null;
    }
}

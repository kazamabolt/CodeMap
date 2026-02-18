package com.codemap.analysis;

import com.codemap.graph.GraphQuery;
import com.codemap.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes class-level dependencies (fields, parameters, inheritance).
 */
public class DependencyAnalyzer {

    private final CodeGraph graph;
    private final GraphQuery query;

    public DependencyAnalyzer(CodeGraph graph) {
        this.graph = graph;
        this.query = new GraphQuery(graph);
    }

    /**
     * Get all classes that the given class depends on.
     *
     * @param className fully qualified class name
     * @return subgraph of class dependencies
     */
    public CodeGraph getClassDependencies(String className) {
        String classId = resolveClassId(className);
        if (classId == null)
            return new CodeGraph(Collections.emptyList(), Collections.emptyList());

        Set<EdgeType> depEdges = EnumSet.of(
                EdgeType.DEPENDENCY, EdgeType.EXTENDS, EdgeType.IMPLEMENTS, EdgeType.IMPORTS);
        return query.forwardTraverse(classId, 1, depEdges);
    }

    /**
     * Get all classes that depend on the given class.
     *
     * @param className fully qualified class name
     * @return subgraph of reverse dependencies
     */
    public CodeGraph getDependents(String className) {
        String classId = resolveClassId(className);
        if (classId == null)
            return new CodeGraph(Collections.emptyList(), Collections.emptyList());

        Set<EdgeType> depEdges = EnumSet.of(
                EdgeType.DEPENDENCY, EdgeType.EXTENDS, EdgeType.IMPLEMENTS);
        return query.reverseTraverse(classId, -1, depEdges);
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

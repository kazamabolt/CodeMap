package com.codemap.analysis;

import com.codemap.graph.GraphQuery;
import com.codemap.model.*;

import java.util.*;

/**
 * Analyzes method call graphs — forward and reverse.
 */
public class CallGraphAnalyzer {

    private final CodeGraph graph;
    private final GraphQuery query;

    public CallGraphAnalyzer(CodeGraph graph) {
        this.graph = graph;
        this.query = new GraphQuery(graph);
    }

    /**
     * Get the call graph starting from the given method, limited by depth.
     *
     * @param methodSignature fully qualified method signature (e.g.,
     *                        "com.example.MyClass.myMethod(String)")
     * @param depth           max call depth (-1 for unlimited)
     * @return subgraph of the call chain
     */
    public CodeGraph getCallGraph(String methodSignature, int depth) {
        String nodeId = resolveMethodId(methodSignature);
        if (nodeId == null)
            return new CodeGraph(Collections.emptyList(), Collections.emptyList());

        Set<EdgeType> callEdges = EnumSet.of(EdgeType.CALLS);
        return query.forwardTraverse(nodeId, depth, callEdges);
    }

    /**
     * Get all methods that call the given method (reverse call graph).
     *
     * @param methodSignature fully qualified method signature
     * @return subgraph of incoming callers
     */
    public CodeGraph getIncomingCalls(String methodSignature) {
        String nodeId = resolveMethodId(methodSignature);
        if (nodeId == null)
            return new CodeGraph(Collections.emptyList(), Collections.emptyList());

        Set<EdgeType> callEdges = EnumSet.of(EdgeType.CALLS);
        return query.reverseTraverse(nodeId, -1, callEdges);
    }

    /**
     * Resolve a method signature to its graph node ID.
     */
    private String resolveMethodId(String methodSignature) {
        // Try direct match
        String directId = "method:" + methodSignature;
        if (graph.getNode(directId).isPresent())
            return directId;

        // Fuzzy match by qualified name suffix
        for (GraphNode node : graph.getNodesByType(NodeType.METHOD)) {
            if (node.getQualifiedName() != null && node.getQualifiedName().contains(methodSignature)) {
                return node.getId();
            }
        }
        for (GraphNode node : graph.getNodesByType(NodeType.CONSTRUCTOR)) {
            if (node.getQualifiedName() != null && node.getQualifiedName().contains(methodSignature)) {
                return node.getId();
            }
        }

        return null;
    }
}

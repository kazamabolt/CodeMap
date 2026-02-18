package com.codemap;

import com.codemap.analysis.*;
import com.codemap.cache.*;
import com.codemap.graph.*;
import com.codemap.model.*;
import com.codemap.parser.*;
import com.codemap.serialization.GraphJsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Main façade for the CodeMap analysis engine.
 * Combines parsing, graph building, and analysis into a unified API.
 * Supports caching for incremental analysis.
 */
public class CodeMapEngine {

    private static final Logger log = LoggerFactory.getLogger(CodeMapEngine.class);

    private final JavaSourceParser parser;
    private final GraphBuilder graphBuilder;
    private final AnalysisCache cache;
    private final GraphJsonSerializer serializer;

    private CodeGraph currentGraph;
    private List<ClassInfo> currentClasses;

    public CodeMapEngine() {
        this(new JavaParserImpl(), new GraphBuilderImpl(), new FileBasedCache());
    }

    public CodeMapEngine(JavaSourceParser parser, GraphBuilder graphBuilder, AnalysisCache cache) {
        this.parser = parser;
        this.graphBuilder = graphBuilder;
        this.cache = cache;
        this.serializer = new GraphJsonSerializer();
    }

    /**
     * Analyze a Java project source directory.
     * Parses all source files, builds the code graph, and caches results.
     *
     * @param sourceRoot root of the Java source tree
     * @return the built code graph
     */
    public CodeGraph analyze(Path sourceRoot) {
        log.info("Starting analysis of {}", sourceRoot);
        long start = System.currentTimeMillis();

        currentClasses = parser.parse(sourceRoot);
        currentGraph = graphBuilder.build(currentClasses);

        long elapsed = System.currentTimeMillis() - start;
        log.info("Analysis complete in {}ms — {} classes, {} methods, {} nodes, {} edges",
                elapsed, currentClasses.size(),
                currentClasses.stream().mapToInt(c -> c.getMethods().size()).sum(),
                currentGraph.nodeCount(), currentGraph.edgeCount());

        return currentGraph;
    }

    /**
     * Get the call graph for a method, up to the specified depth.
     */
    public AnalysisResult getCallGraph(String methodSignature, int depth) {
        ensureAnalyzed();
        long start = System.currentTimeMillis();
        CallGraphAnalyzer analyzer = new CallGraphAnalyzer(currentGraph);
        CodeGraph result = analyzer.getCallGraph(methodSignature, depth);

        return buildResult("callgraph", methodSignature, result, start);
    }

    /**
     * Get all callers of a method (reverse call graph).
     */
    public AnalysisResult getIncomingCalls(String methodSignature) {
        ensureAnalyzed();
        long start = System.currentTimeMillis();
        CallGraphAnalyzer analyzer = new CallGraphAnalyzer(currentGraph);
        CodeGraph result = analyzer.getIncomingCalls(methodSignature);

        return buildResult("incoming-calls", methodSignature, result, start);
    }

    /**
     * Get all dependencies of a class.
     */
    public AnalysisResult getClassDependencies(String className) {
        ensureAnalyzed();
        long start = System.currentTimeMillis();
        DependencyAnalyzer analyzer = new DependencyAnalyzer(currentGraph);
        CodeGraph result = analyzer.getClassDependencies(className);

        return buildResult("dependencies", className, result, start);
    }

    /**
     * Detect circular dependencies in the codebase.
     */
    public AnalysisResult detectCircularDependencies() {
        ensureAnalyzed();
        long start = System.currentTimeMillis();
        CircularDependencyDetector detector = new CircularDependencyDetector(currentGraph);
        List<List<String>> cycles = detector.detectCircularDependencies();

        // Build a graph from the cycle nodes
        Set<String> cycleNodeIds = new HashSet<>();
        for (List<String> cycle : cycles) {
            cycleNodeIds.addAll(cycle);
        }
        CodeGraph cycleGraph = currentGraph.subgraph(cycleNodeIds);

        AnalysisResult result = buildResult("circular-dependencies", "all", cycleGraph, start);
        log.info("Found {} circular dependency cycles", cycles.size());
        return result;
    }

    /**
     * Analyze the impact of modifying a class.
     */
    public AnalysisResult getImpactAnalysis(String className) {
        ensureAnalyzed();
        long start = System.currentTimeMillis();
        ImpactAnalyzer analyzer = new ImpactAnalyzer(currentGraph);
        CodeGraph result = analyzer.getImpactAnalysis(className);

        return buildResult("impact-analysis", className, result, start);
    }

    /**
     * Get the full code graph.
     */
    public CodeGraph getFullGraph() {
        ensureAnalyzed();
        return currentGraph;
    }

    /**
     * Serialize an analysis result to JSON.
     */
    public String toJson(AnalysisResult result) {
        return serializer.toJson(result);
    }

    /**
     * Serialize a code graph to JSON.
     */
    public String toJson(CodeGraph graph) {
        return serializer.toJson(graph);
    }

    /**
     * Get cache statistics.
     */
    public Map<String, Object> getCacheStats() {
        return cache.getStats();
    }

    /**
     * Clear the analysis cache.
     */
    public void clearCache() {
        cache.clear();
        currentGraph = null;
        currentClasses = null;
    }

    private void ensureAnalyzed() {
        if (currentGraph == null) {
            throw new IllegalStateException("No project has been analyzed yet. Call analyze() first.");
        }
    }

    private AnalysisResult buildResult(String command, String target, CodeGraph resultGraph, long startTime) {
        return AnalysisResult.builder()
                .command(command)
                .target(target)
                .graph(resultGraph)
                .timestamp(Instant.now())
                .analysisTimeMs(System.currentTimeMillis() - startTime)
                .totalClassesParsed(currentClasses != null ? currentClasses.size() : 0)
                .totalMethodsParsed(
                        currentClasses != null ? currentClasses.stream().mapToInt(c -> c.getMethods().size()).sum() : 0)
                .build();
    }
}

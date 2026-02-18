package com.codemap.analysis;

import com.codemap.graph.GraphBuilderImpl;
import com.codemap.model.*;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the call graph analyzer — forward and reverse traversal.
 */
class CallGraphAnalyzerTest {

    private CodeGraph graph;

    @BeforeEach
    void setUp() {
        // Build a chain: A.main -> A.process -> B.compute -> C.store
        MethodInfo mainMethod = MethodInfo.builder()
                .name("main").className("com.example.A").addMethodCall("process").build();
        MethodInfo processMethod = MethodInfo.builder()
                .name("process").className("com.example.A").addMethodCall("B.compute").build();
        MethodInfo computeMethod = MethodInfo.builder()
                .name("compute").className("com.example.B").addMethodCall("C.store").build();
        MethodInfo storeMethod = MethodInfo.builder()
                .name("store").className("com.example.C").build();

        List<ClassInfo> classes = List.of(
                ClassInfo.builder().name("A").packageName("com.example")
                        .addMethod(mainMethod).addMethod(processMethod).build(),
                ClassInfo.builder().name("B").packageName("com.example")
                        .addMethod(computeMethod).build(),
                ClassInfo.builder().name("C").packageName("com.example")
                        .addMethod(storeMethod).build());

        graph = new GraphBuilderImpl().build(classes);
    }

    @Test
    void shouldGetCallGraph() {
        CallGraphAnalyzer analyzer = new CallGraphAnalyzer(graph);
        CodeGraph result = analyzer.getCallGraph("A.main", 5);
        assertTrue(result.nodeCount() > 0, "Call graph should contain nodes");
    }

    @Test
    void shouldLimitDepth() {
        CallGraphAnalyzer analyzer = new CallGraphAnalyzer(graph);
        CodeGraph shallow = analyzer.getCallGraph("A.main", 1);
        CodeGraph deep = analyzer.getCallGraph("A.main", 5);
        assertTrue(deep.nodeCount() >= shallow.nodeCount(),
                "Deeper traversal should find >= nodes than shallow");
    }

    @Test
    void shouldGetIncomingCalls() {
        CallGraphAnalyzer analyzer = new CallGraphAnalyzer(graph);
        CodeGraph result = analyzer.getIncomingCalls("C.store");
        assertTrue(result.nodeCount() > 0, "Should find callers of store()");
    }

    @Test
    void shouldReturnEmptyForUnknownMethod() {
        CallGraphAnalyzer analyzer = new CallGraphAnalyzer(graph);
        CodeGraph result = analyzer.getCallGraph("Unknown.method", 5);
        assertEquals(0, result.nodeCount());
    }
}

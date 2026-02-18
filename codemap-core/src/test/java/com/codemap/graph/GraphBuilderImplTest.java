package com.codemap.graph;

import com.codemap.model.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GraphBuilderImpl — verifies node/edge construction from parsed
 * data.
 */
class GraphBuilderImplTest {

    private GraphBuilderImpl builder;

    @BeforeEach
    void setUp() {
        builder = new GraphBuilderImpl();
    }

    @Test
    void shouldCreateClassNodes() {
        List<ClassInfo> classes = List.of(
                ClassInfo.builder()
                        .name("MyClass")
                        .packageName("com.example")
                        .filePath("/src/MyClass.java")
                        .lineNumber(1)
                        .build());

        CodeGraph graph = builder.build(classes);
        assertEquals(1, graph.getNodesByType(NodeType.CLASS).size());

        GraphNode classNode = graph.getNodesByType(NodeType.CLASS).get(0);
        assertEquals("MyClass", classNode.getName());
        assertEquals("com.example.MyClass", classNode.getQualifiedName());
    }

    @Test
    void shouldCreateMethodNodes() {
        MethodInfo method = MethodInfo.builder()
                .name("doWork")
                .className("com.example.MyClass")
                .returnType("void")
                .lineNumber(5)
                .build();

        List<ClassInfo> classes = List.of(
                ClassInfo.builder()
                        .name("MyClass")
                        .packageName("com.example")
                        .addMethod(method)
                        .build());

        CodeGraph graph = builder.build(classes);
        assertEquals(1, graph.getNodesByType(NodeType.METHOD).size());

        // Should have CONTAINS edge from class to method
        List<GraphEdge> containsEdges = graph.getEdgesByType(EdgeType.CONTAINS);
        assertEquals(1, containsEdges.size());
    }

    @Test
    void shouldCreateExtendsEdge() {
        List<ClassInfo> classes = List.of(
                ClassInfo.builder().name("Parent").packageName("com.example").build(),
                ClassInfo.builder().name("Child").packageName("com.example").superClass("Parent").build());

        CodeGraph graph = builder.build(classes);
        List<GraphEdge> extendsEdges = graph.getEdgesByType(EdgeType.EXTENDS);
        assertEquals(1, extendsEdges.size());

        GraphEdge edge = extendsEdges.get(0);
        assertTrue(edge.getSourceId().contains("Child"));
        assertTrue(edge.getTargetId().contains("Parent"));
    }

    @Test
    void shouldCreateImplementsEdge() {
        List<ClassInfo> classes = List.of(
                ClassInfo.builder().name("MyInterface").packageName("com.example").isInterface(true).build(),
                ClassInfo.builder().name("MyImpl").packageName("com.example").addInterface("MyInterface").build());

        CodeGraph graph = builder.build(classes);
        List<GraphEdge> implEdges = graph.getEdgesByType(EdgeType.IMPLEMENTS);
        assertEquals(1, implEdges.size());
    }

    @Test
    void shouldCreateCallEdges() {
        MethodInfo caller = MethodInfo.builder()
                .name("caller")
                .className("com.example.A")
                .addMethodCall("helper")
                .build();

        MethodInfo callee = MethodInfo.builder()
                .name("helper")
                .className("com.example.A")
                .build();

        List<ClassInfo> classes = List.of(
                ClassInfo.builder()
                        .name("A")
                        .packageName("com.example")
                        .addMethod(caller)
                        .addMethod(callee)
                        .build());

        CodeGraph graph = builder.build(classes);
        List<GraphEdge> callEdges = graph.getEdgesByType(EdgeType.CALLS);
        assertEquals(1, callEdges.size());
    }

    @Test
    void shouldHandleMultipleClasses() {
        List<ClassInfo> classes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            classes.add(ClassInfo.builder()
                    .name("Class" + i)
                    .packageName("com.example")
                    .build());
        }

        CodeGraph graph = builder.build(classes);
        assertEquals(5, graph.getNodesByType(NodeType.CLASS).size());
    }
}

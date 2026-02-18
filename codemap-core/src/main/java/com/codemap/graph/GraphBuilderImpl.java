package com.codemap.graph;

import com.codemap.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Converts parsed class/method data into a directed code graph.
 * Creates nodes for classes and methods, and edges for calls, inheritance, etc.
 */
public class GraphBuilderImpl implements GraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(GraphBuilderImpl.class);

    @Override
    public CodeGraph build(List<ClassInfo> classes) {
        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        // Index for qualified name -> node id lookup
        Map<String, String> classIdMap = new HashMap<>();
        Map<String, String> methodIdMap = new HashMap<>();

        // --- Pass 1: Create class nodes ---
        for (ClassInfo cls : classes) {
            String classId = "class:" + cls.getQualifiedName();
            NodeType nodeType = cls.isInterface() ? NodeType.INTERFACE
                    : cls.isEnum() ? NodeType.ENUM : NodeType.CLASS;

            GraphNode classNode = GraphNode.builder()
                    .id(classId)
                    .name(cls.getName())
                    .qualifiedName(cls.getQualifiedName())
                    .type(nodeType)
                    .filePath(cls.getFilePath())
                    .lineNumber(cls.getLineNumber())
                    .metadata("package", cls.getPackageName())
                    .metadata("isAbstract", String.valueOf(cls.isAbstract()))
                    .build();

            nodes.add(classNode);
            classIdMap.put(cls.getQualifiedName(), classId);
            classIdMap.put(cls.getName(), classId); // short-name fallback

            // Create method nodes
            for (MethodInfo method : cls.getMethods()) {
                String methodId = "method:" + cls.getQualifiedName() + "." + method.getSignature();
                NodeType methodType = method.isConstructor() ? NodeType.CONSTRUCTOR : NodeType.METHOD;

                GraphNode methodNode = GraphNode.builder()
                        .id(methodId)
                        .name(method.getName())
                        .qualifiedName(cls.getQualifiedName() + "." + method.getSignature())
                        .type(methodType)
                        .filePath(cls.getFilePath())
                        .lineNumber(method.getLineNumber())
                        .metadata("returnType", method.getReturnType() != null ? method.getReturnType() : "void")
                        .metadata("access", method.getAccessModifier())
                        .metadata("isStatic", String.valueOf(method.isStatic()))
                        .build();

                nodes.add(methodNode);
                methodIdMap.put(cls.getQualifiedName() + "." + method.getSignature(), methodId);
                methodIdMap.put(cls.getQualifiedName() + "." + method.getName(), methodId);
                methodIdMap.put(method.getName(), methodId); // short-name fallback

                // CONTAINS edge: class -> method
                edges.add(GraphEdge.builder()
                        .sourceId(classId)
                        .targetId(methodId)
                        .type(EdgeType.CONTAINS)
                        .build());
            }
        }

        // --- Pass 2: Create edges ---
        for (ClassInfo cls : classes) {
            String classId = classIdMap.get(cls.getQualifiedName());

            // EXTENDS edges
            if (cls.getSuperClass() != null && !cls.getSuperClass().isEmpty()) {
                String superClassId = resolveClassId(cls.getSuperClass(), cls, classIdMap);
                if (superClassId != null) {
                    edges.add(GraphEdge.builder()
                            .sourceId(classId)
                            .targetId(superClassId)
                            .type(EdgeType.EXTENDS)
                            .build());
                }
            }

            // IMPLEMENTS edges
            for (String iface : cls.getInterfaces()) {
                String ifaceId = resolveClassId(iface, cls, classIdMap);
                if (ifaceId != null) {
                    edges.add(GraphEdge.builder()
                            .sourceId(classId)
                            .targetId(ifaceId)
                            .type(EdgeType.IMPLEMENTS)
                            .build());
                }
            }

            // CALLS edges (method -> method)
            for (MethodInfo method : cls.getMethods()) {
                String methodId = methodIdMap.get(cls.getQualifiedName() + "." + method.getSignature());
                if (methodId == null)
                    continue;

                for (String call : method.getMethodCalls()) {
                    String targetMethodId = resolveMethodId(call, cls, methodIdMap, classIdMap);
                    if (targetMethodId != null && !targetMethodId.equals(methodId)) {
                        edges.add(GraphEdge.builder()
                                .sourceId(methodId)
                                .targetId(targetMethodId)
                                .type(EdgeType.CALLS)
                                .build());
                    }
                }
            }

            // DEPENDENCY edges (field types, parameter types)
            Set<String> depTargets = new HashSet<>();
            for (String field : cls.getFields()) {
                String typeName = field.split("\\s+")[0];
                String depId = resolveClassId(typeName, cls, classIdMap);
                if (depId != null && !depId.equals(classId) && depTargets.add(depId)) {
                    edges.add(GraphEdge.builder()
                            .sourceId(classId)
                            .targetId(depId)
                            .type(EdgeType.DEPENDENCY)
                            .metadata("via", "field")
                            .build());
                }
            }

            // DEPENDENCY edges from imports (when imported class exists in codebase)
            for (String imp : cls.getImports()) {
                if (classIdMap.containsKey(imp)) {
                    String depId = classIdMap.get(imp);
                    if (!depId.equals(classId) && depTargets.add(depId)) {
                        edges.add(GraphEdge.builder()
                                .sourceId(classId)
                                .targetId(depId)
                                .type(EdgeType.DEPENDENCY)
                                .metadata("via", "import")
                                .build());
                    }
                }
            }
        }

        log.info("Built graph with {} nodes and {} edges", nodes.size(), edges.size());
        return new CodeGraph(nodes, edges);
    }

    /**
     * Resolve a class name to its node ID, trying qualified names and imports.
     */
    private String resolveClassId(String name, ClassInfo context, Map<String, String> classIdMap) {
        // Try exact qualified name
        if (classIdMap.containsKey(name))
            return classIdMap.get(name);

        // Try with same package
        String inPackage = context.getPackageName() + "." + name;
        if (classIdMap.containsKey(inPackage))
            return classIdMap.get(inPackage);

        // Try imports
        for (String imp : context.getImports()) {
            if (imp.endsWith("." + name)) {
                if (classIdMap.containsKey(imp))
                    return classIdMap.get(imp);
            }
        }

        // Try class: prefix fallback
        String prefixed = "class:" + name;
        for (String key : classIdMap.values()) {
            if (key.equals(prefixed))
                return key;
        }

        return null;
    }

    /**
     * Resolve a method call string to a method node ID.
     */
    private String resolveMethodId(String call, ClassInfo context,
            Map<String, String> methodIdMap,
            Map<String, String> classIdMap) {
        // Direct match
        if (methodIdMap.containsKey(call))
            return methodIdMap.get(call);

        // If call has scope (e.g., "obj.method"), try to resolve
        if (call.contains(".")) {
            String[] parts = call.split("\\.", 2);
            String scope = parts[0];
            String methodName = parts[1];

            // Try fully qualified
            String fqn = context.getPackageName() + "." + scope + "." + methodName;
            if (methodIdMap.containsKey(fqn))
                return methodIdMap.get(fqn);

            // Try scope as class
            String classId = resolveClassId(scope, context, classIdMap);
            if (classId != null) {
                String className = classId.replace("class:", "");
                String fullMethod = className + "." + methodName;
                if (methodIdMap.containsKey(fullMethod))
                    return methodIdMap.get(fullMethod);
            }

            // Try scope as a local variable — search all imported classes for the method
            for (String imp : context.getImports()) {
                if (classIdMap.containsKey(imp)) {
                    String fullMethod = imp + "." + methodName;
                    if (methodIdMap.containsKey(fullMethod))
                        return methodIdMap.get(fullMethod);
                }
            }

            // Try all classes in the same package
            for (Map.Entry<String, String> entry : classIdMap.entrySet()) {
                if (entry.getKey().startsWith(context.getPackageName() + ".")) {
                    String fullMethod = entry.getKey() + "." + methodName;
                    if (methodIdMap.containsKey(fullMethod))
                        return methodIdMap.get(fullMethod);
                }
            }
        } else {
            // Unqualified — assume same class
            String sameClass = context.getQualifiedName() + "." + call;
            if (methodIdMap.containsKey(sameClass))
                return methodIdMap.get(sameClass);
        }

        return null;
    }
}

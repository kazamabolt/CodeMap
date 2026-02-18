package com.codemap.model;

import java.util.*;

/**
 * Represents a node in the code graph (class, method, interface, etc.).
 */
public class GraphNode {

    private final String id;
    private final String name;
    private final String qualifiedName;
    private final NodeType type;
    private final String filePath;
    private final int lineNumber;
    private final Map<String, String> metadata;

    private GraphNode(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.qualifiedName = builder.qualifiedName;
        this.type = builder.type;
        this.filePath = builder.filePath;
        this.lineNumber = builder.lineNumber;
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getQualifiedName() { return qualifiedName; }
    public NodeType getType() { return type; }
    public String getFilePath() { return filePath; }
    public int getLineNumber() { return lineNumber; }
    public Map<String, String> getMetadata() { return metadata; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode that = (GraphNode) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("GraphNode{id='%s', type=%s, name='%s'}", id, type, name);
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String name;
        private String qualifiedName;
        private NodeType type;
        private String filePath;
        private int lineNumber;
        private final Map<String, String> metadata = new HashMap<>();

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder qualifiedName(String qualifiedName) { this.qualifiedName = qualifiedName; return this; }
        public Builder type(NodeType type) { this.type = type; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder lineNumber(int lineNumber) { this.lineNumber = lineNumber; return this; }
        public Builder metadata(String key, String value) { this.metadata.put(key, value); return this; }

        public GraphNode build() {
            Objects.requireNonNull(id, "Node id is required");
            Objects.requireNonNull(name, "Node name is required");
            Objects.requireNonNull(type, "Node type is required");
            return new GraphNode(this);
        }
    }
}

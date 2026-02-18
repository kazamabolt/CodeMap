package com.codemap.model;

import java.util.*;

/**
 * Represents a directed edge in the code graph.
 */
public class GraphEdge {

    private final String id;
    private final String sourceId;
    private final String targetId;
    private final EdgeType type;
    private final Map<String, String> metadata;

    private GraphEdge(Builder builder) {
        this.id = builder.id;
        this.sourceId = builder.sourceId;
        this.targetId = builder.targetId;
        this.type = builder.type;
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
    }

    public String getId() {
        return id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getTargetId() {
        return targetId;
    }

    public EdgeType getType() {
        return type;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GraphEdge that = (GraphEdge) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("GraphEdge{%s -[%s]-> %s}", sourceId, type, targetId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String sourceId;
        private String targetId;
        private EdgeType type;
        private final Map<String, String> metadata = new HashMap<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder targetId(String targetId) {
            this.targetId = targetId;
            return this;
        }

        public Builder type(EdgeType type) {
            this.type = type;
            return this;
        }

        public Builder metadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }

        public GraphEdge build() {
            Objects.requireNonNull(sourceId, "Source ID is required");
            Objects.requireNonNull(targetId, "Target ID is required");
            Objects.requireNonNull(type, "Edge type is required");
            if (id == null) {
                id = sourceId + "-" + type.name() + "-" + targetId;
            }
            return new GraphEdge(this);
        }
    }
}

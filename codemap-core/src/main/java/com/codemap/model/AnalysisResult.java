package com.codemap.model;

import java.time.Instant;

/**
 * Wrapper for analysis results with metadata.
 */
public class AnalysisResult {

    private final CodeGraph graph;
    private final String command;
    private final String target;
    private final Instant timestamp;
    private final long analysisTimeMs;
    private final int totalClassesParsed;
    private final int totalMethodsParsed;

    private AnalysisResult(Builder builder) {
        this.graph = builder.graph;
        this.command = builder.command;
        this.target = builder.target;
        this.timestamp = builder.timestamp;
        this.analysisTimeMs = builder.analysisTimeMs;
        this.totalClassesParsed = builder.totalClassesParsed;
        this.totalMethodsParsed = builder.totalMethodsParsed;
    }

    public CodeGraph getGraph() {
        return graph;
    }

    public String getCommand() {
        return command;
    }

    public String getTarget() {
        return target;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getAnalysisTimeMs() {
        return analysisTimeMs;
    }

    public int getTotalClassesParsed() {
        return totalClassesParsed;
    }

    public int getTotalMethodsParsed() {
        return totalMethodsParsed;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private CodeGraph graph;
        private String command;
        private String target;
        private Instant timestamp = Instant.now();
        private long analysisTimeMs;
        private int totalClassesParsed;
        private int totalMethodsParsed;

        public Builder graph(CodeGraph graph) {
            this.graph = graph;
            return this;
        }

        public Builder command(String command) {
            this.command = command;
            return this;
        }

        public Builder target(String target) {
            this.target = target;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder analysisTimeMs(long ms) {
            this.analysisTimeMs = ms;
            return this;
        }

        public Builder totalClassesParsed(int count) {
            this.totalClassesParsed = count;
            return this;
        }

        public Builder totalMethodsParsed(int count) {
            this.totalMethodsParsed = count;
            return this;
        }

        public AnalysisResult build() {
            java.util.Objects.requireNonNull(graph, "Graph is required");
            return new AnalysisResult(this);
        }
    }
}

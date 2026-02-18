package com.codemap.serialization;

import com.codemap.model.*;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;

/**
 * Serializes CodeGraph and AnalysisResult to JSON format.
 */
public class GraphJsonSerializer {

    private final Gson gson;

    public GraphJsonSerializer() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Instant.class, new InstantAdapter())
                .create();
    }

    /**
     * Serialize an analysis result to JSON.
     */
    public String toJson(AnalysisResult result) {
        return gson.toJson(buildResultObject(result));
    }

    /**
     * Serialize a code graph to JSON.
     */
    public String toJson(CodeGraph graph) {
        return gson.toJson(buildGraphObject(graph));
    }

    private JsonObject buildResultObject(AnalysisResult result) {
        JsonObject root = new JsonObject();
        root.addProperty("command", result.getCommand());
        root.addProperty("target", result.getTarget());
        root.addProperty("timestamp", result.getTimestamp().toString());
        root.addProperty("analysisTimeMs", result.getAnalysisTimeMs());

        JsonObject stats = new JsonObject();
        stats.addProperty("totalClassesParsed", result.getTotalClassesParsed());
        stats.addProperty("totalMethodsParsed", result.getTotalMethodsParsed());
        stats.addProperty("graphNodes", result.getGraph().nodeCount());
        stats.addProperty("graphEdges", result.getGraph().edgeCount());
        root.add("stats", stats);

        root.add("graph", buildGraphObject(result.getGraph()));
        return root;
    }

    private JsonObject buildGraphObject(CodeGraph graph) {
        JsonObject graphObj = new JsonObject();

        JsonArray nodesArr = new JsonArray();
        for (GraphNode node : graph.getNodes()) {
            JsonObject nodeObj = new JsonObject();
            nodeObj.addProperty("id", node.getId());
            nodeObj.addProperty("name", node.getName());
            nodeObj.addProperty("qualifiedName", node.getQualifiedName());
            nodeObj.addProperty("type", node.getType().name());
            if (node.getFilePath() != null)
                nodeObj.addProperty("filePath", node.getFilePath());
            if (node.getLineNumber() > 0)
                nodeObj.addProperty("lineNumber", node.getLineNumber());
            if (!node.getMetadata().isEmpty()) {
                nodeObj.add("metadata", gson.toJsonTree(node.getMetadata()));
            }
            nodesArr.add(nodeObj);
        }
        graphObj.add("nodes", nodesArr);

        JsonArray edgesArr = new JsonArray();
        for (GraphEdge edge : graph.getEdges()) {
            JsonObject edgeObj = new JsonObject();
            edgeObj.addProperty("id", edge.getId());
            edgeObj.addProperty("source", edge.getSourceId());
            edgeObj.addProperty("target", edge.getTargetId());
            edgeObj.addProperty("type", edge.getType().name());
            if (!edge.getMetadata().isEmpty()) {
                edgeObj.add("metadata", gson.toJsonTree(edge.getMetadata()));
            }
            edgesArr.add(edgeObj);
        }
        graphObj.add("edges", edgesArr);

        return graphObj;
    }

    /**
     * Adapter for java.time.Instant serialization.
     */
    private static class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return Instant.parse(json.getAsString());
        }
    }
}

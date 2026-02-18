package com.codemap.rules;

import com.codemap.model.CodeGraph;

import java.util.List;
import java.util.Map;

/**
 * Interface for architecture rules that can detect violations.
 */
public interface ArchitectureRule {

    /**
     * Unique name of the rule.
     */
    String getName();

    /**
     * Description of what the rule checks for.
     */
    String getDescription();

    /**
     * Evaluate the rule against the given code graph.
     *
     * @param graph the code graph to analyze
     * @return list of violations found
     */
    List<Violation> evaluate(CodeGraph graph);

    /**
     * Configure the rule with the given parameters.
     *
     * @param config configuration parameters
     */
    default void configure(Map<String, Object> config) {
        // Default no-op; override if rule has configurable thresholds
    }

    /**
     * Represents a rule violation.
     */
    record Violation(
            String ruleName,
            String severity,
            String message,
            String nodeId,
            String filePath,
            int lineNumber) {
    }
}

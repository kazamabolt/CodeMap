package com.codemap.rules;

import com.codemap.model.CodeGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Engine that runs architecture rules against a code graph and collects
 * violations.
 */
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final List<ArchitectureRule> rules = new ArrayList<>();

    public RuleEngine() {
        // Register default rules
        rules.add(new CircularDependencyRule());
        rules.add(new GodClassRule());
        rules.add(new DeepInheritanceRule());
        rules.add(new UnusedClassRule());
    }

    /**
     * Add a custom rule.
     */
    public void addRule(ArchitectureRule rule) {
        rules.add(rule);
    }

    /**
     * Remove a rule by name.
     */
    public void removeRule(String ruleName) {
        rules.removeIf(r -> r.getName().equals(ruleName));
    }

    /**
     * Configure a specific rule.
     */
    public void configureRule(String ruleName, Map<String, Object> config) {
        for (ArchitectureRule rule : rules) {
            if (rule.getName().equals(ruleName)) {
                rule.configure(config);
                return;
            }
        }
        log.warn("Rule not found: {}", ruleName);
    }

    /**
     * Run all rules against the code graph.
     *
     * @param graph the code graph to analyze
     * @return all violations from all rules
     */
    public List<ArchitectureRule.Violation> evaluate(CodeGraph graph) {
        List<ArchitectureRule.Violation> allViolations = new ArrayList<>();
        for (ArchitectureRule rule : rules) {
            try {
                List<ArchitectureRule.Violation> violations = rule.evaluate(graph);
                allViolations.addAll(violations);
                log.info("Rule '{}': {} violations", rule.getName(), violations.size());
            } catch (Exception e) {
                log.error("Error evaluating rule '{}': {}", rule.getName(), e.getMessage(), e);
            }
        }
        return allViolations;
    }

    /**
     * Get list of registered rules.
     */
    public List<ArchitectureRule> getRules() {
        return Collections.unmodifiableList(rules);
    }
}

package com.codemap.model;

/**
 * Types of edges (relationships) in the code graph.
 */
public enum EdgeType {
    /** Method A calls Method B */
    CALLS,
    /** Class A extends Class B */
    EXTENDS,
    /** Class A implements Interface B */
    IMPLEMENTS,
    /** Class A depends on Class B (field, parameter, local var) */
    DEPENDENCY,
    /** Class A imports Class B */
    IMPORTS,
    /** Method overrides parent method */
    OVERRIDES,
    /** Class contains Method */
    CONTAINS
}

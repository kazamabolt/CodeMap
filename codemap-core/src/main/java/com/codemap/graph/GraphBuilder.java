package com.codemap.graph;

import com.codemap.model.ClassInfo;
import com.codemap.model.CodeGraph;

import java.util.List;

/**
 * Interface for building a code graph from parsed class data.
 */
public interface GraphBuilder {

    /**
     * Build a complete code graph from parsed class information.
     *
     * @param classes list of parsed class information
     * @return the constructed code graph
     */
    CodeGraph build(List<ClassInfo> classes);
}

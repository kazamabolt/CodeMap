package com.codemap.cache;

import com.codemap.model.ClassInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Interface for caching parsed analysis results.
 * Enables incremental analysis by skipping unchanged files.
 */
public interface AnalysisCache {

    /**
     * Check if a source file's cached result is still valid.
     *
     * @param sourceFile path to the source file
     * @return true if the cached result is valid (file unchanged)
     */
    boolean isValid(Path sourceFile);

    /**
     * Get cached parse result for a source file.
     *
     * @param sourceFile path to the source file
     * @return cached class info, or null if not cached
     */
    List<ClassInfo> get(Path sourceFile);

    /**
     * Store parse result in cache.
     *
     * @param sourceFile path to the source file
     * @param classes    parsed class info
     */
    void put(Path sourceFile, List<ClassInfo> classes);

    /**
     * Invalidate cache for a specific file.
     */
    void invalidate(Path sourceFile);

    /**
     * Clear the entire cache.
     */
    void clear();

    /**
     * Get cache statistics.
     */
    Map<String, Object> getStats();
}

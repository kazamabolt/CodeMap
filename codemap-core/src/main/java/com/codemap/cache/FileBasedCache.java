package com.codemap.cache;

import com.codemap.model.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File-hash-based cache that tracks source file checksums to enable incremental
 * analysis.
 * Stores cached results in memory with file hash validation.
 */
public class FileBasedCache implements AnalysisCache {

    private static final Logger log = LoggerFactory.getLogger(FileBasedCache.class);

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private long hits = 0;
    private long misses = 0;

    @Override
    public boolean isValid(Path sourceFile) {
        String key = sourceFile.toAbsolutePath().toString();
        CacheEntry entry = cache.get(key);
        if (entry == null)
            return false;

        String currentHash = computeHash(sourceFile);
        return currentHash != null && currentHash.equals(entry.fileHash);
    }

    @Override
    public List<ClassInfo> get(Path sourceFile) {
        String key = sourceFile.toAbsolutePath().toString();
        CacheEntry entry = cache.get(key);

        if (entry != null && isValid(sourceFile)) {
            hits++;
            log.debug("Cache hit for {}", sourceFile.getFileName());
            return entry.classes;
        }

        misses++;
        return null;
    }

    @Override
    public void put(Path sourceFile, List<ClassInfo> classes) {
        String key = sourceFile.toAbsolutePath().toString();
        String hash = computeHash(sourceFile);
        if (hash != null) {
            cache.put(key, new CacheEntry(hash, classes));
            log.debug("Cached {} classes from {}", classes.size(), sourceFile.getFileName());
        }
    }

    @Override
    public void invalidate(Path sourceFile) {
        String key = sourceFile.toAbsolutePath().toString();
        cache.remove(key);
    }

    @Override
    public void clear() {
        cache.clear();
        hits = 0;
        misses = 0;
        log.info("Cache cleared");
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("entries", cache.size());
        stats.put("hits", hits);
        stats.put("misses", misses);
        stats.put("hitRate", (hits + misses) > 0 ? (double) hits / (hits + misses) : 0.0);
        return stats;
    }

    private String computeHash(Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            log.warn("Failed to compute hash for {}: {}", file, e.getMessage());
            return null;
        }
    }

    private static class CacheEntry {
        final String fileHash;
        final List<ClassInfo> classes;

        CacheEntry(String fileHash, List<ClassInfo> classes) {
            this.fileHash = fileHash;
            this.classes = classes;
        }
    }
}

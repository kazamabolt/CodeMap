package com.codemap.parser;

import com.codemap.model.ClassInfo;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface for parsing Java source files into structured class information.
 */
public interface JavaSourceParser {

    /**
     * Parse all Java source files under the given root directory.
     *
     * @param sourceRoot root directory containing Java source files
     * @return list of parsed class/interface information
     */
    List<ClassInfo> parse(Path sourceRoot);

    /**
     * Parse a single Java source file.
     *
     * @param sourceFile path to the Java source file
     * @return list of classes found in the file (can be multiple for inner classes)
     */
    List<ClassInfo> parseFile(Path sourceFile);
}

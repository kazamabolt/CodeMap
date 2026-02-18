package com.codemap.cli;

import com.codemap.CodeMapEngine;
import com.codemap.model.AnalysisResult;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Command-line interface for the CodeMap core engine.
 * Used by IDE plugins (VS Code, IntelliJ) to invoke analysis.
 *
 * Usage:
 * java -jar codemap-core.jar --project /path/to/src --command callgraph
 * --target "com.example.Main.main(String[])" --depth 3
 */
@Command(name = "codemap", mixinStandardHelpOptions = true, version = "CodeMap 1.0.0", description = "Analyze Java projects and build code interaction graphs.")
public class CodeMapCli implements Callable<Integer> {

    @Option(names = { "-p", "--project" }, required = true, description = "Path to Java source root")
    private Path projectPath;

    @Option(names = { "-c",
            "--command" }, required = true, description = "Analysis command: callgraph, incoming-calls, dependencies, circular-deps, impact")
    private String command;

    @Option(names = { "-t", "--target" }, description = "Target method signature or class name")
    private String target;

    @Option(names = { "-d", "--depth" }, defaultValue = "5", description = "Max traversal depth (default: 5)")
    private int depth;

    @Override
    public Integer call() {
        try {
            CodeMapEngine engine = new CodeMapEngine();
            engine.analyze(projectPath);

            AnalysisResult result;
            switch (command.toLowerCase()) {
                case "callgraph":
                    requireTarget();
                    result = engine.getCallGraph(target, depth);
                    break;
                case "incoming-calls":
                    requireTarget();
                    result = engine.getIncomingCalls(target);
                    break;
                case "dependencies":
                    requireTarget();
                    result = engine.getClassDependencies(target);
                    break;
                case "circular-deps":
                    result = engine.detectCircularDependencies();
                    break;
                case "impact":
                    requireTarget();
                    result = engine.getImpactAnalysis(target);
                    break;
                case "fullgraph":
                    result = AnalysisResult.builder()
                            .command("fullgraph")
                            .target("all")
                            .graph(engine.getFullGraph())
                            .timestamp(java.time.Instant.now())
                            .analysisTimeMs(System.currentTimeMillis() - System.currentTimeMillis())
                            .totalClassesParsed(0)
                            .totalMethodsParsed(0)
                            .build();
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    System.err.println("Available: callgraph, incoming-calls, dependencies, circular-deps, impact");
                    return 1;
            }

            // Output JSON to stdout (captured by IDE plugins)
            System.out.println(engine.toJson(result));
            return 0;

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return 1;
        }
    }

    private void requireTarget() {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("--target is required for command: " + command);
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CodeMapCli()).execute(args);
        System.exit(exitCode);
    }
}

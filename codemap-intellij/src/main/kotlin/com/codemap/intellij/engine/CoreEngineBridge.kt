package com.codemap.intellij.engine

import com.codemap.intellij.model.AnalysisResult
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Bridge to the CodeMap core Java engine.
 * Spawns the engine as a subprocess and captures JSON output.
 */
class CoreEngineBridge(private val project: Project) {

    private val log = Logger.getInstance(CoreEngineBridge::class.java)
    private val gson = Gson()

    /**
     * Execute a core engine command.
     */
    fun execute(command: String, target: String? = null, depth: Int = 5): AnalysisResult {
        val jarPath = resolveJarPath()
        val javaPath = resolveJavaPath()
        val projectPath = resolveSourceRoot()

        val args = mutableListOf(javaPath, "-jar", jarPath, "--project", projectPath, "--command", command)
        if (target != null) {
            args.addAll(listOf("--target", target))
        }
        args.addAll(listOf("--depth", depth.toString()))

        log.info("Executing: ${args.joinToString(" ")}")

        val process = ProcessBuilder(args)
            .directory(File(projectPath))
            .redirectErrorStream(false)
            .start()

        val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
        val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            log.error("Engine failed (code $exitCode): $stderr")
            throw RuntimeException("CodeMap analysis failed: $stderr")
        }

        if (stderr.isNotBlank()) {
            log.info("Engine logs: $stderr")
        }

        return gson.fromJson(stdout, AnalysisResult::class.java)
    }

    fun getCallGraph(target: String, depth: Int = 5): AnalysisResult =
        execute("callgraph", target, depth)

    fun getIncomingCalls(target: String): AnalysisResult =
        execute("incoming-calls", target)

    fun getClassDependencies(target: String): AnalysisResult =
        execute("dependencies", target)

    fun getImpactAnalysis(target: String): AnalysisResult =
        execute("impact", target)

    fun detectCircularDependencies(): AnalysisResult =
        execute("circular-deps")

    private fun resolveJarPath(): String {
        // Look for the JAR in the project directory or default location
        val projectBase = project.basePath ?: "."
        val candidates = listOf(
            "$projectBase/codemap-core/target/codemap-core-1.0.0-SNAPSHOT.jar",
            "$projectBase/../codemap-core/target/codemap-core-1.0.0-SNAPSHOT.jar",
            System.getProperty("user.home") + "/.codemap/codemap-core.jar"
        )
        return candidates.firstOrNull { File(it).exists() }
            ?: throw RuntimeException("codemap-core JAR not found. Build the core engine with 'mvn package'.")
    }

    private fun resolveJavaPath(): String {
        val javaHome = System.getProperty("java.home") ?: System.getenv("JAVA_HOME") ?: ""
        return if (javaHome.isNotEmpty()) "$javaHome/bin/java" else "java"
    }

    private fun resolveSourceRoot(): String {
        val basePath = project.basePath ?: throw RuntimeException("No project path available")
        // Try common source roots
        val candidates = listOf(
            "$basePath/src/main/java",
            "$basePath/src",
            basePath
        )
        return candidates.firstOrNull { File(it).exists() } ?: basePath
    }
}

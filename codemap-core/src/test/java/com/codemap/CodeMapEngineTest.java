package com.codemap;

import com.codemap.model.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for CodeMapEngine — parse → build → query → JSON.
 */
class CodeMapEngineTest {

    private static Path tempDir;
    private CodeMapEngine engine;

    @BeforeAll
    static void createTestProject() throws IOException {
        tempDir = Files.createTempDirectory("codemap-engine-test");

        // Create a small test project
        Path srcDir = tempDir.resolve("com/example");
        Files.createDirectories(srcDir);

        Files.writeString(srcDir.resolve("Service.java"),
                """
                        package com.example;

                        public interface Service {
                            String process(String input);
                        }
                        """);

        Files.writeString(srcDir.resolve("ServiceImpl.java"),
                """
                        package com.example;

                        public class ServiceImpl implements Service {
                            private final Repository repo;

                            public ServiceImpl(Repository repo) {
                                this.repo = repo;
                            }

                            @Override
                            public String process(String input) {
                                String data = repo.fetch(input);
                                return transform(data);
                            }

                            private String transform(String data) {
                                return data.toUpperCase();
                            }
                        }
                        """);

        Files.writeString(srcDir.resolve("Repository.java"),
                """
                        package com.example;

                        public class Repository {
                            public String fetch(String key) {
                                return "data:" + key;
                            }
                        }
                        """);

        Files.writeString(srcDir.resolve("Controller.java"),
                """
                        package com.example;

                        public class Controller {
                            private final Service service;

                            public Controller(Service service) {
                                this.service = service;
                            }

                            public String handle(String request) {
                                return service.process(request);
                            }
                        }
                        """);
    }

    @AfterAll
    static void cleanup() throws IOException {
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
    }

    @BeforeEach
    void setUp() {
        engine = new CodeMapEngine();
    }

    @Test
    void shouldAnalyzeProject() {
        CodeGraph graph = engine.analyze(tempDir);
        assertNotNull(graph);
        assertTrue(graph.nodeCount() > 0, "Should have nodes");
        assertTrue(graph.edgeCount() > 0, "Should have edges");
    }

    @Test
    void shouldGetCallGraph() {
        engine.analyze(tempDir);
        AnalysisResult result = engine.getCallGraph("process", 3);
        assertNotNull(result);
        assertEquals("callgraph", result.getCommand());
    }

    @Test
    void shouldGetClassDependencies() {
        engine.analyze(tempDir);
        AnalysisResult result = engine.getClassDependencies("ServiceImpl");
        assertNotNull(result);
        assertEquals("dependencies", result.getCommand());
    }

    @Test
    void shouldDetectCircularDependencies() {
        engine.analyze(tempDir);
        AnalysisResult result = engine.detectCircularDependencies();
        assertNotNull(result);
        assertEquals("circular-dependencies", result.getCommand());
    }

    @Test
    void shouldGetImpactAnalysis() {
        engine.analyze(tempDir);
        AnalysisResult result = engine.getImpactAnalysis("Repository");
        assertNotNull(result);
        assertEquals("impact-analysis", result.getCommand());
    }

    @Test
    void shouldSerializeToJson() {
        engine.analyze(tempDir);
        AnalysisResult result = engine.getCallGraph("process", 3);
        String json = engine.toJson(result);
        assertNotNull(json);
        assertTrue(json.contains("\"nodes\""));
        assertTrue(json.contains("\"edges\""));
        assertTrue(json.contains("\"command\""));
    }

    @Test
    void shouldThrowWhenNotAnalyzed() {
        assertThrows(IllegalStateException.class, () -> engine.getCallGraph("anything", 1));
    }
}

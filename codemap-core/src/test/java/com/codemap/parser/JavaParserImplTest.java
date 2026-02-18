package com.codemap.parser;

import com.codemap.model.ClassInfo;
import com.codemap.model.MethodInfo;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JavaParserImpl — verifies extraction of classes, methods, calls,
 * inheritance.
 */
class JavaParserImplTest {

    private static Path tempDir;
    private JavaParserImpl parser;

    @BeforeAll
    static void createTempDir() throws IOException {
        tempDir = Files.createTempDirectory("codemap-test");
    }

    @AfterAll
    static void cleanupTempDir() throws IOException {
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
        parser = new JavaParserImpl();
    }

    @Test
    void shouldParseSimpleClass() throws IOException {
        Path file = createJavaFile("SimpleClass.java",
                """
                        package com.example;

                        public class SimpleClass {
                            private String name;

                            public String getName() {
                                return name;
                            }

                            public void setName(String name) {
                                this.name = name;
                            }
                        }
                        """);

        List<ClassInfo> classes = parser.parseFile(file);
        assertEquals(1, classes.size());

        ClassInfo cls = classes.get(0);
        assertEquals("SimpleClass", cls.getName());
        assertEquals("com.example", cls.getPackageName());
        assertEquals("com.example.SimpleClass", cls.getQualifiedName());
        assertFalse(cls.isInterface());
        assertEquals(2, cls.getMethods().size());
    }

    @Test
    void shouldExtractMethodCalls() throws IOException {
        Path file = createJavaFile("CallerClass.java",
                """
                        package com.example;

                        public class CallerClass {
                            private SimpleClass simple = new SimpleClass();

                            public void doWork() {
                                simple.setName("test");
                                String name = simple.getName();
                                System.out.println(name);
                            }
                        }
                        """);

        List<ClassInfo> classes = parser.parseFile(file);
        assertEquals(1, classes.size());

        MethodInfo doWork = classes.get(0).getMethods().get(0);
        assertEquals("doWork", doWork.getName());
        assertTrue(doWork.getMethodCalls().size() >= 2);
        assertTrue(doWork.getMethodCalls().stream().anyMatch(c -> c.contains("setName")));
        assertTrue(doWork.getMethodCalls().stream().anyMatch(c -> c.contains("getName")));
    }

    @Test
    void shouldExtractInheritance() throws IOException {
        Path file = createJavaFile("ChildClass.java",
                """
                        package com.example;

                        import java.io.Serializable;

                        public class ChildClass extends SimpleClass implements Serializable {
                            @Override
                            public String getName() {
                                return "child:" + super.getName();
                            }
                        }
                        """);

        List<ClassInfo> classes = parser.parseFile(file);
        assertEquals(1, classes.size());

        ClassInfo cls = classes.get(0);
        assertEquals("SimpleClass", cls.getSuperClass());
        assertTrue(cls.getInterfaces().contains("Serializable"));
    }

    @Test
    void shouldParseInterface() throws IOException {
        Path file = createJavaFile("MyInterface.java",
                """
                        package com.example;

                        public interface MyInterface {
                            void doSomething();
                            String getValue();
                        }
                        """);

        List<ClassInfo> classes = parser.parseFile(file);
        assertEquals(1, classes.size());
        assertTrue(classes.get(0).isInterface());
        assertEquals(2, classes.get(0).getMethods().size());
    }

    @Test
    void shouldExtractAnnotations() throws IOException {
        Path file = createJavaFile("AnnotatedClass.java",
                """
                        package com.example;

                        @Deprecated
                        public class AnnotatedClass {
                            @Override
                            public String toString() {
                                return "annotated";
                            }
                        }
                        """);

        List<ClassInfo> classes = parser.parseFile(file);
        ClassInfo cls = classes.get(0);
        assertTrue(cls.getAnnotations().contains("Deprecated"));

        MethodInfo method = cls.getMethods().get(0);
        assertTrue(method.getAnnotations().contains("Override"));
    }

    @Test
    void shouldParseDirectoryRecursively() throws IOException {
        Path subDir = tempDir.resolve("pkg");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("A.java"),
                "package pkg; public class A { public void m() {} }");
        Files.writeString(subDir.resolve("B.java"),
                "package pkg; public class B { public void n() {} }");

        List<ClassInfo> classes = parser.parse(tempDir);
        assertTrue(classes.size() >= 2, "Should find at least 2 classes");
    }

    private Path createJavaFile(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }
}

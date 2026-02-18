package com.codemap.parser;

import com.codemap.model.ClassInfo;
import com.codemap.model.MethodInfo;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JavaParser-based implementation of {@link JavaSourceParser}.
 * Walks the source tree, parses each .java file, and extracts
 * classes, methods, calls, inheritance, annotations, and imports.
 */
public class JavaParserImpl implements JavaSourceParser {

    private static final Logger log = LoggerFactory.getLogger(JavaParserImpl.class);
    private final JavaParser parser;

    public JavaParserImpl() {
        this.parser = new JavaParser();
    }

    @Override
    public List<ClassInfo> parse(Path sourceRoot) {
        List<ClassInfo> allClasses = new ArrayList<>();
        try {
            Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        try {
                            allClasses.addAll(parseFile(file));
                        } catch (Exception e) {
                            log.warn("Failed to parse file: {}", file, e);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("Cannot access file: {}", file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Failed to walk source tree: {}", sourceRoot, e);
        }
        log.info("Parsed {} classes from {}", allClasses.size(), sourceRoot);
        return allClasses;
    }

    @Override
    public List<ClassInfo> parseFile(Path sourceFile) {
        List<ClassInfo> classes = new ArrayList<>();
        try {
            ParseResult<CompilationUnit> result = parser.parse(sourceFile);
            if (result.isSuccessful() && result.getResult().isPresent()) {
                CompilationUnit cu = result.getResult().get();
                String packageName = cu.getPackageDeclaration()
                        .map(pd -> pd.getNameAsString())
                        .orElse("");

                List<String> imports = cu.getImports().stream()
                        .map(ImportDeclaration::getNameAsString)
                        .collect(Collectors.toList());

                // Visit all class/interface declarations
                cu.accept(new ClassVisitor(packageName, imports, sourceFile.toString(), classes), null);
            } else {
                log.warn("Parse failed for {}: {}", sourceFile,
                        result.getProblems().stream()
                                .map(Object::toString)
                                .collect(Collectors.joining("; ")));
            }
        } catch (IOException e) {
            log.error("IO error parsing file: {}", sourceFile, e);
        }
        return classes;
    }

    /**
     * Visitor that extracts class/interface declarations.
     */
    private static class ClassVisitor extends VoidVisitorAdapter<Void> {

        private final String packageName;
        private final List<String> imports;
        private final String filePath;
        private final List<ClassInfo> classes;

        ClassVisitor(String packageName, List<String> imports, String filePath, List<ClassInfo> classes) {
            this.packageName = packageName;
            this.imports = imports;
            this.filePath = filePath;
            this.classes = classes;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration decl, Void arg) {
            ClassInfo.Builder builder = ClassInfo.builder()
                    .name(decl.getNameAsString())
                    .packageName(packageName)
                    .filePath(filePath)
                    .lineNumber(decl.getBegin().map(p -> p.line).orElse(0))
                    .isInterface(decl.isInterface())
                    .isAbstract(decl.isAbstract())
                    .imports(new ArrayList<>(imports));

            // Super class
            decl.getExtendedTypes().stream()
                    .findFirst()
                    .ifPresent(t -> builder.superClass(t.getNameAsString()));

            // Interfaces
            decl.getImplementedTypes().forEach(t -> builder.addInterface(t.getNameAsString()));

            // Extended interfaces (for interface declarations)
            if (decl.isInterface()) {
                decl.getExtendedTypes().forEach(t -> builder.addInterface(t.getNameAsString()));
            }

            // Annotations
            decl.getAnnotations().forEach(a -> builder.addAnnotation(a.getNameAsString()));

            // Fields
            decl.getFields().forEach(field -> field.getVariables()
                    .forEach(var -> builder.addField(field.getElementType().asString() + " " + var.getNameAsString())));

            // Methods
            decl.getMethods().forEach(method -> builder.addMethod(extractMethod(method, decl)));

            // Constructors
            decl.getConstructors().forEach(ctor -> builder.addMethod(extractConstructor(ctor, decl)));

            classes.add(builder.build());

            // Continue visiting inner classes
            super.visit(decl, arg);
        }

        @Override
        public void visit(EnumDeclaration decl, Void arg) {
            ClassInfo.Builder builder = ClassInfo.builder()
                    .name(decl.getNameAsString())
                    .packageName(packageName)
                    .filePath(filePath)
                    .lineNumber(decl.getBegin().map(p -> p.line).orElse(0))
                    .isEnum(true)
                    .imports(new ArrayList<>(imports));

            decl.getImplementedTypes().forEach(t -> builder.addInterface(t.getNameAsString()));
            decl.getAnnotations().forEach(a -> builder.addAnnotation(a.getNameAsString()));
            decl.getMethods().forEach(method -> builder.addMethod(extractMethod(method, null)));

            classes.add(builder.build());
            super.visit(decl, arg);
        }

        private MethodInfo extractMethod(MethodDeclaration method, ClassOrInterfaceDeclaration parentClass) {
            String className = parentClass != null ? (packageName.isEmpty() ? parentClass.getNameAsString()
                    : packageName + "." + parentClass.getNameAsString()) : "";

            MethodInfo.Builder builder = MethodInfo.builder()
                    .name(method.getNameAsString())
                    .className(className)
                    .returnType(method.getTypeAsString())
                    .lineNumber(method.getBegin().map(p -> p.line).orElse(0))
                    .isStatic(method.isStatic())
                    .isAbstract(method.isAbstract());

            // Access modifier
            if (method.isPublic())
                builder.accessModifier("public");
            else if (method.isProtected())
                builder.accessModifier("protected");
            else if (method.isPrivate())
                builder.accessModifier("private");

            // Parameters
            method.getParameters().forEach(p -> builder.addParameterType(p.getTypeAsString()));

            // Annotations
            method.getAnnotations().forEach(a -> builder.addAnnotation(a.getNameAsString()));

            // Method calls within the body
            method.findAll(MethodCallExpr.class).forEach(call -> {
                String scope = call.getScope()
                        .map(Object::toString)
                        .orElse("");
                String callStr = scope.isEmpty() ? call.getNameAsString()
                        : scope + "." + call.getNameAsString();
                builder.addMethodCall(callStr);
            });

            return builder.build();
        }

        private MethodInfo extractConstructor(ConstructorDeclaration ctor, ClassOrInterfaceDeclaration parentClass) {
            String className = parentClass != null ? (packageName.isEmpty() ? parentClass.getNameAsString()
                    : packageName + "." + parentClass.getNameAsString()) : "";

            MethodInfo.Builder builder = MethodInfo.builder()
                    .name(ctor.getNameAsString())
                    .className(className)
                    .lineNumber(ctor.getBegin().map(p -> p.line).orElse(0))
                    .isConstructor(true);

            if (ctor.isPublic())
                builder.accessModifier("public");
            else if (ctor.isProtected())
                builder.accessModifier("protected");
            else if (ctor.isPrivate())
                builder.accessModifier("private");

            ctor.getParameters().forEach(p -> builder.addParameterType(p.getTypeAsString()));
            ctor.getAnnotations().forEach(a -> builder.addAnnotation(a.getNameAsString()));

            ctor.findAll(MethodCallExpr.class).forEach(call -> {
                String scope = call.getScope().map(Object::toString).orElse("");
                String callStr = scope.isEmpty() ? call.getNameAsString()
                        : scope + "." + call.getNameAsString();
                builder.addMethodCall(callStr);
            });

            return builder.build();
        }
    }
}

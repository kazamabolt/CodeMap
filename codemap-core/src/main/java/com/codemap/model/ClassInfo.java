package com.codemap.model;

import java.util.*;

/**
 * Parsed information about a Java class or interface.
 */
public class ClassInfo {

    private final String name;
    private final String qualifiedName;
    private final String packageName;
    private final String filePath;
    private final int lineNumber;
    private final boolean isInterface;
    private final boolean isEnum;
    private final boolean isAbstract;
    private final String superClass;
    private final List<String> interfaces;
    private final List<MethodInfo> methods;
    private final List<String> fields;
    private final List<String> annotations;
    private final List<String> imports;

    private ClassInfo(Builder builder) {
        this.name = builder.name;
        this.qualifiedName = builder.qualifiedName;
        this.packageName = builder.packageName;
        this.filePath = builder.filePath;
        this.lineNumber = builder.lineNumber;
        this.isInterface = builder.isInterface;
        this.isEnum = builder.isEnum;
        this.isAbstract = builder.isAbstract;
        this.superClass = builder.superClass;
        this.interfaces = Collections.unmodifiableList(new ArrayList<>(builder.interfaces));
        this.methods = Collections.unmodifiableList(new ArrayList<>(builder.methods));
        this.fields = Collections.unmodifiableList(new ArrayList<>(builder.fields));
        this.annotations = Collections.unmodifiableList(new ArrayList<>(builder.annotations));
        this.imports = Collections.unmodifiableList(new ArrayList<>(builder.imports));
    }

    public String getName() {
        return name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public String getSuperClass() {
        return superClass;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public List<MethodInfo> getMethods() {
        return methods;
    }

    public List<String> getFields() {
        return fields;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public List<String> getImports() {
        return imports;
    }

    @Override
    public String toString() {
        return String.format("ClassInfo{qualifiedName='%s', methods=%d}", qualifiedName, methods.size());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String qualifiedName;
        private String packageName;
        private String filePath;
        private int lineNumber;
        private boolean isInterface;
        private boolean isEnum;
        private boolean isAbstract;
        private String superClass;
        private List<String> interfaces = new ArrayList<>();
        private List<MethodInfo> methods = new ArrayList<>();
        private List<String> fields = new ArrayList<>();
        private List<String> annotations = new ArrayList<>();
        private List<String> imports = new ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder qualifiedName(String qualifiedName) {
            this.qualifiedName = qualifiedName;
            return this;
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder lineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public Builder isInterface(boolean isInterface) {
            this.isInterface = isInterface;
            return this;
        }

        public Builder isEnum(boolean isEnum) {
            this.isEnum = isEnum;
            return this;
        }

        public Builder isAbstract(boolean isAbstract) {
            this.isAbstract = isAbstract;
            return this;
        }

        public Builder superClass(String superClass) {
            this.superClass = superClass;
            return this;
        }

        public Builder interfaces(List<String> interfaces) {
            this.interfaces = interfaces;
            return this;
        }

        public Builder addInterface(String iface) {
            this.interfaces.add(iface);
            return this;
        }

        public Builder methods(List<MethodInfo> methods) {
            this.methods = methods;
            return this;
        }

        public Builder addMethod(MethodInfo method) {
            this.methods.add(method);
            return this;
        }

        public Builder fields(List<String> fields) {
            this.fields = fields;
            return this;
        }

        public Builder addField(String field) {
            this.fields.add(field);
            return this;
        }

        public Builder annotations(List<String> annotations) {
            this.annotations = annotations;
            return this;
        }

        public Builder addAnnotation(String annotation) {
            this.annotations.add(annotation);
            return this;
        }

        public Builder imports(List<String> imports) {
            this.imports = imports;
            return this;
        }

        public Builder addImport(String imp) {
            this.imports.add(imp);
            return this;
        }

        public ClassInfo build() {
            Objects.requireNonNull(name, "Class name is required");
            if (qualifiedName == null) {
                qualifiedName = packageName != null ? packageName + "." + name : name;
            }
            return new ClassInfo(this);
        }
    }
}

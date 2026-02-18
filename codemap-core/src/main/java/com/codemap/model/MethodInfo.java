package com.codemap.model;

import java.util.*;

/**
 * Parsed information about a Java method.
 */
public class MethodInfo {

    private final String name;
    private final String signature;
    private final String qualifiedName;
    private final String className;
    private final String returnType;
    private final List<String> parameterTypes;
    private final List<String> methodCalls;
    private final List<String> annotations;
    private final int lineNumber;
    private final boolean isConstructor;
    private final boolean isStatic;
    private final boolean isAbstract;
    private final String accessModifier;

    private MethodInfo(Builder builder) {
        this.name = builder.name;
        this.signature = builder.signature;
        this.qualifiedName = builder.qualifiedName;
        this.className = builder.className;
        this.returnType = builder.returnType;
        this.parameterTypes = Collections.unmodifiableList(new ArrayList<>(builder.parameterTypes));
        this.methodCalls = Collections.unmodifiableList(new ArrayList<>(builder.methodCalls));
        this.annotations = Collections.unmodifiableList(new ArrayList<>(builder.annotations));
        this.lineNumber = builder.lineNumber;
        this.isConstructor = builder.isConstructor;
        this.isStatic = builder.isStatic;
        this.isAbstract = builder.isAbstract;
        this.accessModifier = builder.accessModifier;
    }

    public String getName() {
        return name;
    }

    public String getSignature() {
        return signature;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getClassName() {
        return className;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public List<String> getMethodCalls() {
        return methodCalls;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public String getAccessModifier() {
        return accessModifier;
    }

    @Override
    public String toString() {
        return String.format("MethodInfo{qualifiedName='%s', calls=%d}", qualifiedName, methodCalls.size());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String signature;
        private String qualifiedName;
        private String className;
        private String returnType;
        private List<String> parameterTypes = new ArrayList<>();
        private List<String> methodCalls = new ArrayList<>();
        private List<String> annotations = new ArrayList<>();
        private int lineNumber;
        private boolean isConstructor;
        private boolean isStatic;
        private boolean isAbstract;
        private String accessModifier = "package-private";

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public Builder qualifiedName(String qualifiedName) {
            this.qualifiedName = qualifiedName;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder returnType(String returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder parameterTypes(List<String> parameterTypes) {
            this.parameterTypes = parameterTypes;
            return this;
        }

        public Builder addParameterType(String paramType) {
            this.parameterTypes.add(paramType);
            return this;
        }

        public Builder methodCalls(List<String> methodCalls) {
            this.methodCalls = methodCalls;
            return this;
        }

        public Builder addMethodCall(String call) {
            this.methodCalls.add(call);
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

        public Builder lineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public Builder isConstructor(boolean isConstructor) {
            this.isConstructor = isConstructor;
            return this;
        }

        public Builder isStatic(boolean isStatic) {
            this.isStatic = isStatic;
            return this;
        }

        public Builder isAbstract(boolean isAbstract) {
            this.isAbstract = isAbstract;
            return this;
        }

        public Builder accessModifier(String accessModifier) {
            this.accessModifier = accessModifier;
            return this;
        }

        public MethodInfo build() {
            Objects.requireNonNull(name, "Method name is required");
            if (signature == null) {
                signature = name + "(" + String.join(", ", parameterTypes) + ")";
            }
            if (qualifiedName == null && className != null) {
                qualifiedName = className + "." + signature;
            }
            return new MethodInfo(this);
        }
    }
}

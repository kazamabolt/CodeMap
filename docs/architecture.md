# CodeMap Architecture

## System Overview

```
┌─────────────────────────────────────────────────────┐
│                    IDE Plugins                       │
│  ┌──────────────────┐    ┌────────────────────────┐ │
│  │   VS Code Ext    │    │   IntelliJ Plugin      │ │
│  │   (TypeScript)   │    │   (Kotlin)             │ │
│  │                  │    │                        │ │
│  │  ┌────────────┐  │    │  ┌──────────────────┐  │ │
│  │  │ WebView    │  │    │  │ JCEF Panel       │  │ │
│  │  │ Cytoscape  │  │    │  │ Cytoscape.js     │  │ │
│  │  └────────────┘  │    │  └──────────────────┘  │ │
│  └───────┬──────────┘    └──────────┬─────────────┘ │
│          │   CLI (java -jar)        │               │
│          └──────────┬───────────────┘               │
└─────────────────────┼───────────────────────────────┘
                      ▼
┌─────────────────────────────────────────────────────┐
│              Core Engine (Java)                      │
│                                                      │
│  CodeMapCli → CodeMapEngine (Façade)                │
│                    │                                 │
│         ┌──────────┼──────────┐                     │
│         ▼          ▼          ▼                     │
│    JavaParser   GraphBuilder  Analyzers             │
│    (AST Parse)  (Nodes/Edges) (CallGraph,           │
│         │          │          DependencyAnalyzer,    │
│         ▼          ▼          ImpactAnalyzer,        │
│    ClassInfo    CodeGraph     CircularDepDetector)   │
│    MethodInfo      │                                 │
│         │          ▼                                 │
│         │    GraphJsonSerializer → JSON stdout       │
│         │                                            │
│         └──► FileBasedCache (MD5 incremental)       │
│                                                      │
│  Phase 2: RuleEngine                                │
│    ├── CircularDependencyRule                       │
│    ├── GodClassRule                                 │
│    ├── DeepInheritanceRule                          │
│    ├── UnusedClassRule                              │
│    └── LayerViolationRule                           │
└─────────────────────────────────────────────────────┘
```

## Data Flow

1. **User Action** → Right-click menu in IDE
2. **Plugin** → Extracts symbol at cursor
3. **Plugin** → Spawns `java -jar codemap-core.jar --command <cmd> --target <symbol>`
4. **Core Engine** → Parses source files → Builds CodeGraph → Runs analysis → JSON to stdout
5. **Plugin** → Parses JSON → Sends to WebView/JCEF
6. **WebView** → Cytoscape.js renders interactive graph

## Key Design Decisions

- **CLI boundary** between core engine and plugins ensures loose coupling
- **JSON over stdout** keeps the protocol simple and debuggable
- **Logging to stderr** prevents log noise from polluting JSON output
- **File-hash cache** enables incremental analysis for large repos
- **Builder pattern** for immutable data models throughout the core
- **Interface-driven design** allows swapping parser/builder/cache implementations

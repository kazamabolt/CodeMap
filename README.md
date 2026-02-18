# CodeMap

**Cross-IDE code analysis and visualization tool for Java projects.**

Analyze your Java codebase and visualize class/method interactions with interactive graphs. Supports both VS Code and IntelliJ IDEA through a shared core analysis engine.

## Quick Start

### Prerequisites

- **Java 17+** (JDK)
- **Maven 3.8+** (for core engine)
- **Node.js 18+** / npm (for VS Code extension)
- **Gradle 8+** (for IntelliJ plugin — uses wrapper)

### 1. Build the Core Engine

```bash
cd codemap-core
mvn clean package -DskipTests
```

This produces `codemap-core/target/codemap-core-1.0.0-SNAPSHOT.jar`.

### 2. Test the CLI

```bash
java -jar codemap-core/target/codemap-core-1.0.0-SNAPSHOT.jar \
  --project /path/to/your/java/src \
  --command callgraph \
  --target "MyClass.myMethod" \
  --depth 3
```

### 3. Run Tests

```bash
cd codemap-core
mvn test
```

---

## VS Code Extension

### Setup

```bash
cd codemap-vscode
npm install
npm run compile
```

### Development

1. Open `codemap-vscode/` in VS Code
2. Press `F5` to launch Extension Development Host
3. Open a Java project
4. Right-click on a method → **CodeMap** menu

### Configuration

| Setting                   | Description                                    | Default               |
|---------------------------|------------------------------------------------|-----------------------|
| `codemap.coreJarPath`     | Path to codemap-core JAR                       | Auto-detected         |
| `codemap.javaHome`        | Java home directory                            | `$JAVA_HOME`          |
| `codemap.defaultDepth`    | Default call graph depth                       | `5`                   |
| `codemap.excludePackages` | Package prefixes to exclude                    | `[]`                  |

---

## IntelliJ IDEA Plugin

### Setup

```bash
cd codemap-intellij
./gradlew build
```

### Development

```bash
cd codemap-intellij
./gradlew runIde
```

This launches a sandboxed IntelliJ instance with the plugin installed.

### Usage

1. Open a Java project in the sandboxed IDE
2. Right-click on a method/class → **CodeMap** submenu
3. Select an analysis action
4. View the graph in the **CodeMap** tool window (bottom panel)

---

## Features

| Action                      | Description                                    | Shortcut              |
|-----------------------------|------------------------------------------------|-----------------------|
| **Show Method Flow**        | Forward call graph from selected method        | Right-click → CodeMap |
| **Who Calls This?**         | Reverse call graph showing all callers         | Right-click → CodeMap |
| **Show Class Dependencies** | Class dependency diagram                       | Right-click → CodeMap |
| **Impact Analysis**         | Classes affected if this class changes         | Right-click → CodeMap |

### Graph Interaction

- **Zoom**: Mouse wheel
- **Pan**: Click and drag background
- **Select**: Click a node
- **Navigate to Source**: Click a node to jump to its file/line
- **Collapse Methods**: Toggle button to show/hide method nodes
- **Layout**: Switch between hierarchical, force-directed, breadth-first, circle
- **Hover**: Shows qualified name, type, file, and metadata

---

## Architecture

```
codemap-core/         → Shared Java analysis engine (JAR)
codemap-vscode/       → VS Code extension (TypeScript)
codemap-intellij/     → IntelliJ plugin (Kotlin)
docs/                 → Architecture docs + example JSON
```

The core engine is invoked via CLI by both plugins. JSON output over stdout keeps the protocol simple and the plugin boundary clean. See [docs/architecture.md](docs/architecture.md) for details.

---

## Project Structure

### Core Engine (`codemap-core`)
|-----------------------------|------------------------------------------------------------------------|
| Package                     | Purpose                                                                |
|-----------------------------|------------------------------------------------------------------------|
| `com.codemap.model`         | Data models (GraphNode, GraphEdge, CodeGraph, ClassInfo, MethodInfo)   |
| `com.codemap.parser`        | Java source parsing via JavaParser                                     |
| `com.codemap.graph`         | Graph construction and traversal                                       |
| `com.codemap.analysis`      | Call graph, dependency, impact, circular dep analyzers                 |
| `com.codemap.cache`         | File-hash-based incremental cache                                      |
| `com.codemap.rules`         | Architecture rule engine (Phase 2)                                     |
| `com.codemap.serialization` | JSON serialization                                                     |
| `com.codemap.cli`           | CLI entry point                                                        |
| `com.codemap`               | CodeMapEngine façade                                                   |
|-----------------------------|------------------------------------------------------------------------|

---

## Phase 2: Architecture Rule Engine

Configurable rules to detect code quality issues:
|-----------------------|------------------------------------------------|
| Rule                  | Detects                                        |
|-----------------------|------------------------------------------------|
| `circular-dependency` | Circular dependencies between classes          |
| `god-class`           | Classes with too many methods/dependencies     |
| `deep-inheritance`    | Inheritance chains beyond threshold            |
| `unused-class`        | Classes with no incoming dependencies          |
| `layer-violation`     | Violations of layered architecture conventions |
|-----------------------|------------------------------------------------|



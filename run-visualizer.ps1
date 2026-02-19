# Automation script for CodeMap Visualizer
# Usage: .\run-visualizer.ps1

$projectPath = "D:\Workspace\Java\E-Commers_Mini_Project\src"
$jarPath = "codemap-core\target\codemap-core-1.0.0-SNAPSHOT.jar"
$outputFile = "graph_data.js"
$visualizerFile = "visualizer.html"

Write-Output "Starting CodeMap analysis on $projectPath..."

# Run the JAR and capture output
$jsonOutput = & java -jar $jarPath -p $projectPath -c fullgraph

if ($LASTEXITCODE -ne 0) {
    Write-Error "CodeMap analysis failed!"
    exit $LASTEXITCODE
}

# Format for graph_data.js
$content = "const FULL_GRAPH = $jsonOutput;"
$content | Out-File -FilePath $outputFile -Encoding utf8

Write-Output "Analysis complete. Data saved to $outputFile."

# Open the visualizer
Write-Output "Opening visualizer..."
Start-Process $visualizerFile

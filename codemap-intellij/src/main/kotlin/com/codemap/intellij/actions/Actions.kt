package com.codemap.intellij.actions

import com.codemap.intellij.engine.CoreEngineBridge
import com.codemap.intellij.ui.GraphToolWindowFactory
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.wm.ToolWindowManager

/**
 * Show Method Flow — forward call graph from the selected method.
 */
class ShowMethodFlowAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val symbol = getSymbolAtCursor(e) ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "CodeMap: Analyzing $symbol...") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val bridge = CoreEngineBridge(project)
                    val result = bridge.getCallGraph(symbol, 5)

                    // Update UI on EDT
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CodeMap")
                        toolWindow?.show()
                        GraphToolWindowFactory.updateGraph(project, result)
                    }
                } catch (ex: Exception) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        com.intellij.openapi.ui.Messages.showErrorDialog(
                            project, "CodeMap Error: ${ex.message}", "CodeMap"
                        )
                    }
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }
}

/**
 * Who Calls This? — reverse call graph.
 */
class ShowCallersAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val symbol = getSymbolAtCursor(e) ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "CodeMap: Finding callers of $symbol...") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val bridge = CoreEngineBridge(project)
                    val result = bridge.getIncomingCalls(symbol)

                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CodeMap")
                        toolWindow?.show()
                        GraphToolWindowFactory.updateGraph(project, result)
                    }
                } catch (ex: Exception) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        com.intellij.openapi.ui.Messages.showErrorDialog(project, "CodeMap Error: ${ex.message}", "CodeMap")
                    }
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }
}

/**
 * Show Class Dependencies.
 */
class ShowDependenciesAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val symbol = getSymbolAtCursor(e) ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "CodeMap: Analyzing dependencies of $symbol...") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val bridge = CoreEngineBridge(project)
                    val result = bridge.getClassDependencies(symbol)

                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CodeMap")
                        toolWindow?.show()
                        GraphToolWindowFactory.updateGraph(project, result)
                    }
                } catch (ex: Exception) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        com.intellij.openapi.ui.Messages.showErrorDialog(project, "CodeMap Error: ${ex.message}", "CodeMap")
                    }
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }
}

/**
 * Impact Analysis.
 */
class ShowImpactAnalysisAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val symbol = getSymbolAtCursor(e) ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "CodeMap: Impact analysis for $symbol...") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val bridge = CoreEngineBridge(project)
                    val result = bridge.getImpactAnalysis(symbol)

                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CodeMap")
                        toolWindow?.show()
                        GraphToolWindowFactory.updateGraph(project, result)
                    }
                } catch (ex: Exception) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        com.intellij.openapi.ui.Messages.showErrorDialog(project, "CodeMap Error: ${ex.message}", "CodeMap")
                    }
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }
}

/**
 * Utility to extract the symbol at the cursor position.
 */
private fun getSymbolAtCursor(e: AnActionEvent): String? {
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
    val caretOffset = editor.caretModel.offset
    val document = editor.document
    val text = document.text

    // Find word boundaries
    var start = caretOffset
    var end = caretOffset

    while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '.' || text[start - 1] == '_')) {
        start--
    }
    while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '.' || text[end] == '_')) {
        end++
    }

    return if (start < end) text.substring(start, end) else null
}

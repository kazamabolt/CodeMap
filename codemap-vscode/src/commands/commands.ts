import * as vscode from 'vscode';
import { CoreEngineBridge } from '../engine/coreEngineBridge';
import { GraphPanel } from '../webview/graphPanel';

/**
 * Extracts the Java symbol at the cursor position (class or method name).
 */
function getSymbolAtCursor(editor: vscode.TextEditor): string {
    const position = editor.selection.active;
    const wordRange = editor.document.getWordRangeAtPosition(position, /[\w$.]+/);
    return wordRange ? editor.document.getText(wordRange) : '';
}

/**
 * Gets the source root of the Java project.
 */
function getSourceRoot(): string {
    const workspaceFolders = vscode.workspace.workspaceFolders;
    if (!workspaceFolders || workspaceFolders.length === 0) {
        throw new Error('No workspace folder open');
    }
    return workspaceFolders[0].uri.fsPath;
}

/**
 * Show Method Flow — forward call graph from the selected method.
 */
export async function showMethodFlow(extensionUri: vscode.Uri): Promise<void> {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
        vscode.window.showWarningMessage('Open a Java file and place cursor on a method.');
        return;
    }

    const symbol = getSymbolAtCursor(editor);
    if (!symbol) {
        vscode.window.showWarningMessage('Place cursor on a method name.');
        return;
    }

    const config = vscode.workspace.getConfiguration('codemap');
    const depth = config.get<number>('defaultDepth') || 5;

    try {
        const bridge = new CoreEngineBridge();
        const panel = GraphPanel.createOrShow(extensionUri, `Method Flow: ${symbol}`);

        await vscode.window.withProgress(
            { location: vscode.ProgressLocation.Notification, title: `CodeMap: Analyzing ${symbol}...` },
            async () => {
                const result = await bridge.getCallGraph(getSourceRoot(), symbol, depth);
                panel.updateGraph(result);
            }
        );
    } catch (error: any) {
        vscode.window.showErrorMessage(`CodeMap Error: ${error.message}`);
    }
}

/**
 * Who Calls This? — reverse call graph of the selected method.
 */
export async function showCallers(extensionUri: vscode.Uri): Promise<void> {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
        vscode.window.showWarningMessage('Open a Java file and place cursor on a method.');
        return;
    }

    const symbol = getSymbolAtCursor(editor);
    if (!symbol) {
        vscode.window.showWarningMessage('Place cursor on a method name.');
        return;
    }

    try {
        const bridge = new CoreEngineBridge();
        const panel = GraphPanel.createOrShow(extensionUri, `Callers of: ${symbol}`);

        await vscode.window.withProgress(
            { location: vscode.ProgressLocation.Notification, title: `CodeMap: Finding callers of ${symbol}...` },
            async () => {
                const result = await bridge.getIncomingCalls(getSourceRoot(), symbol);
                panel.updateGraph(result);
            }
        );
    } catch (error: any) {
        vscode.window.showErrorMessage(`CodeMap Error: ${error.message}`);
    }
}

/**
 * Show Class Dependencies — dependency graph of the selected class.
 */
export async function showDependencies(extensionUri: vscode.Uri): Promise<void> {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
        vscode.window.showWarningMessage('Open a Java file and place cursor on a class name.');
        return;
    }

    const symbol = getSymbolAtCursor(editor);
    if (!symbol) {
        vscode.window.showWarningMessage('Place cursor on a class name.');
        return;
    }

    try {
        const bridge = new CoreEngineBridge();
        const panel = GraphPanel.createOrShow(extensionUri, `Dependencies: ${symbol}`);

        await vscode.window.withProgress(
            { location: vscode.ProgressLocation.Notification, title: `CodeMap: Analyzing dependencies of ${symbol}...` },
            async () => {
                const result = await bridge.getClassDependencies(getSourceRoot(), symbol);
                panel.updateGraph(result);
            }
        );
    } catch (error: any) {
        vscode.window.showErrorMessage(`CodeMap Error: ${error.message}`);
    }
}

/**
 * Impact Analysis — show affected classes if the selected class is modified.
 */
export async function showImpactAnalysis(extensionUri: vscode.Uri): Promise<void> {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
        vscode.window.showWarningMessage('Open a Java file and place cursor on a class name.');
        return;
    }

    const symbol = getSymbolAtCursor(editor);
    if (!symbol) {
        vscode.window.showWarningMessage('Place cursor on a class name.');
        return;
    }

    try {
        const bridge = new CoreEngineBridge();
        const panel = GraphPanel.createOrShow(extensionUri, `Impact Analysis: ${symbol}`);

        await vscode.window.withProgress(
            { location: vscode.ProgressLocation.Notification, title: `CodeMap: Impact analysis for ${symbol}...` },
            async () => {
                const result = await bridge.getImpactAnalysis(getSourceRoot(), symbol);
                panel.updateGraph(result);
            }
        );
    } catch (error: any) {
        vscode.window.showErrorMessage(`CodeMap Error: ${error.message}`);
    }
}

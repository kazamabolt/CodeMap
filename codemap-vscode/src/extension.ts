import * as vscode from 'vscode';
import { showMethodFlow, showCallers, showDependencies, showImpactAnalysis } from './commands/commands';

/**
 * Extension entry point — registers all CodeMap commands.
 */
export function activate(context: vscode.ExtensionContext) {
    console.log('CodeMap extension activated');

    const extensionUri = context.extensionUri;

    context.subscriptions.push(
        vscode.commands.registerCommand('codemap.showMethodFlow', () => showMethodFlow(extensionUri)),
        vscode.commands.registerCommand('codemap.showCallers', () => showCallers(extensionUri)),
        vscode.commands.registerCommand('codemap.showDependencies', () => showDependencies(extensionUri)),
        vscode.commands.registerCommand('codemap.showImpactAnalysis', () => showImpactAnalysis(extensionUri))
    );
}

export function deactivate() {
    console.log('CodeMap extension deactivated');
}

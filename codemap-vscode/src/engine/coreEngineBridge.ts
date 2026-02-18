import * as vscode from 'vscode';
import * as cp from 'child_process';
import * as path from 'path';
import { AnalysisResult } from '../types/graph';

/**
 * Bridge to the CodeMap core Java engine.
 * Invokes the engine via CLI (java -jar) and parses JSON output.
 */
export class CoreEngineBridge {
    private jarPath: string;
    private javaPath: string;

    constructor() {
        const config = vscode.workspace.getConfiguration('codemap');
        this.jarPath = config.get<string>('coreJarPath') || this.resolveDefaultJarPath();
        this.javaPath = this.resolveJavaPath(config.get<string>('javaHome') || '');
    }

    /**
     * Execute a core engine command and return the parsed result.
     */
    async execute(command: string, projectPath: string, target?: string, depth?: number): Promise<AnalysisResult> {
        const args = [
            '-jar', this.jarPath,
            '--project', projectPath,
            '--command', command
        ];

        if (target) {
            args.push('--target', target);
        }
        if (depth !== undefined) {
            args.push('--depth', depth.toString());
        }

        return new Promise((resolve, reject) => {
            const process = cp.spawn(this.javaPath, args, {
                cwd: projectPath,
                env: { ...global.process.env }
            });

            let stdout = '';
            let stderr = '';

            process.stdout.on('data', (data: Buffer) => {
                stdout += data.toString();
            });

            process.stderr.on('data', (data: Buffer) => {
                stderr += data.toString();
            });

            process.on('close', (code: number | null) => {
                if (code === 0) {
                    try {
                        const result: AnalysisResult = JSON.parse(stdout);
                        resolve(result);
                    } catch (parseError) {
                        reject(new Error(`Failed to parse engine output: ${parseError}`));
                    }
                } else {
                    reject(new Error(`Engine exited with code ${code}: ${stderr}`));
                }
            });

            process.on('error', (err: Error) => {
                reject(new Error(`Failed to start engine: ${err.message}. Ensure Java is installed and codemap-core JAR path is configured.`));
            });
        });
    }

    async getCallGraph(projectPath: string, methodSignature: string, depth: number): Promise<AnalysisResult> {
        return this.execute('callgraph', projectPath, methodSignature, depth);
    }

    async getIncomingCalls(projectPath: string, methodSignature: string): Promise<AnalysisResult> {
        return this.execute('incoming-calls', projectPath, methodSignature);
    }

    async getClassDependencies(projectPath: string, className: string): Promise<AnalysisResult> {
        return this.execute('dependencies', projectPath, className);
    }

    async getImpactAnalysis(projectPath: string, className: string): Promise<AnalysisResult> {
        return this.execute('impact', projectPath, className);
    }

    private resolveDefaultJarPath(): string {
        // Look for JAR relative to extension location
        return path.join(__dirname, '..', '..', 'codemap-core', 'target', 'codemap-core-1.0.0-SNAPSHOT.jar');
    }

    private resolveJavaPath(javaHome: string): string {
        if (javaHome) {
            return path.join(javaHome, 'bin', 'java');
        }
        return process.env.JAVA_HOME
            ? path.join(process.env.JAVA_HOME, 'bin', 'java')
            : 'java';
    }
}

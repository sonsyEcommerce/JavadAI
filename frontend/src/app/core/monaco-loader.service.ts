
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class MonacoLoaderService {
  private initialized = false;

  initialize(): void {
    if (this.initialized) return;

    const workers: Record<string, () => Worker> = {
      editor: () =>
        new Worker(
          new URL(
            '../../../node_modules/monaco-editor/esm/vs/editor/editor.worker.js',
            import.meta.url
          ),
          { type: 'module' }
        ),

      json: () =>
        new Worker(
          new URL(
            '../../../node_modules/monaco-editor/esm/vs/language/json/json.worker.js',
            import.meta.url
          ),
          { type: 'module' }
        ),

      css: () =>
        new Worker(
          new URL(
            '../../../node_modules/monaco-editor/esm/vs/language/css/css.worker.js',
            import.meta.url
          ),
          { type: 'module' }
        ),

      html: () =>
        new Worker(
          new URL(
            '../../../node_modules/monaco-editor/esm/vs/language/html/html.worker.js',
            import.meta.url
          ),
          { type: 'module' }
        ),

      typescript: () =>
        new Worker(
          new URL(
            '../../../node_modules/monaco-editor/esm/vs/language/typescript/ts.worker.js',
            import.meta.url
          ),
          { type: 'module' }
        ),
    };

    (globalThis as any).MonacoEnvironment = {
      getWorker: (_workerId: string, label: string): Worker => {
        const key =
          label === 'javascript' ? 'typescript' :
          ['json', 'css', 'scss', 'less', 'html', 'typescript'].includes(label)
            ? label
            : 'editor';

        return (workers[key] ?? workers['editor'])();
      },
    };

    this.initialized = true;
  }
}

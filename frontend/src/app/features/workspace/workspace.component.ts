
import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  inject,
  Output,
  OnDestroy,
  OnInit,
  signal,
  ViewChild,
} from '@angular/core';

import type * as Monaco from 'monaco-editor';

import {
  WorkspaceApiService,
  WorkspaceFile,
} from '../../core/workspace-api.service';

import { MonacoLoaderService } from '../../core/monaco-loader.service';

interface OpenFileTab {
  path: string;
  name: string;
  content: string;
  dirty: boolean;
}

@Component({
  selector: 'app-workspace',
  standalone: true,
  templateUrl: './workspace.component.html',
  styleUrl: './workspace.component.scss',
})
export class WorkspaceComponent implements OnInit, AfterViewInit, OnDestroy {
  @Output() readonly editorContextChange = new EventEmitter<{ path: string; content: string }>();

  private readonly api = inject(WorkspaceApiService);
  private readonly monacoLoader = inject(MonacoLoaderService);

  private monaco?: typeof import('monaco-editor');
  private editor?: Monaco.editor.IStandaloneCodeEditor;

  private readonly models = new Map<string, Monaco.editor.ITextModel>();
  private readonly savedContents = new Map<string, string>();

  protected readonly workspacePath = signal('');
  protected readonly currentPath = signal('');
  protected readonly files = signal<WorkspaceFile[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly newWorkspacePath = signal('');
  protected readonly changingWorkspace = signal(false);

  protected readonly selectedFilePath = signal('');
  protected readonly fileContent = signal('');
  protected readonly fileLoading = signal(false);
  protected readonly saving = signal(false);
  protected readonly fileError = signal('');
  protected readonly fileSaved = signal(false);

  protected readonly tabs = signal<OpenFileTab[]>([]);

  @ViewChild('editorHost')
  set editorHost(ref: ElementRef<HTMLDivElement> | undefined) {
    if (ref && !this.editor) {
      void this.initializeEditor(ref.nativeElement);
    }
  }

  ngOnInit(): void {
    this.loadWorkspace();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.editor?.dispose();

    this.models.forEach(model => model.dispose());
    this.models.clear();
  }

  private async initializeEditor(host: HTMLDivElement): Promise<void> {
    try {
      this.monacoLoader.initialize();

      this.monaco = await import('monaco-editor');

      this.editor = this.monaco.editor.create(host, {
        value: '',
        language: 'plaintext',
        theme: 'vs-dark',
        automaticLayout: true,
        fontSize: 14,
        tabSize: 4,
        minimap: { enabled: true },
        scrollBeyondLastLine: false,
        wordWrap: 'off',
        roundedSelection: false,
        padding: { top: 12 },
      });

      this.editor.addCommand(
        this.monaco.KeyMod.CtrlCmd | this.monaco.KeyCode.KeyS,
        () => this.saveFile()
      );

      // مدل فایل‌هایی که قبل از آماده شدن Monaco باز شده‌اند
      for (const tab of this.tabs()) {
        this.createModel(tab.path, tab.content);
      }

      this.setActiveModel();
    } catch (error) {
      console.error('Monaco initialization failed:', error);
      this.fileError.set('راه‌اندازی ویرایشگر Monaco ناموفق بود.');
    }
  }

  private createModel(
    path: string,
    content: string
  ): Monaco.editor.ITextModel | undefined {
    if (!this.monaco) return undefined;

    const existing = this.models.get(path);
    if (existing) return existing;

    const model = this.monaco.editor.createModel(
      content,
      this.getLanguage(path),
      this.monaco.Uri.file('/' + path.replace(/\\/g, '/'))
    );

    model.onDidChangeContent(() => {
      const currentContent = model.getValue();
      const savedContent = this.savedContents.get(path) ?? '';

      this.tabs.update(tabs =>
        tabs.map(tab =>
          tab.path === path
            ? {
              ...tab,
              content: currentContent,
              dirty: currentContent !== savedContent,
            }
            : tab
        )
      );

      if (this.selectedFilePath() === path) {
        this.fileContent.set(currentContent);
        this.fileSaved.set(false);
        this.editorContextChange.emit({ path, content: currentContent });
      }
    });

    this.models.set(path, model);
    return model;
  }

  private getLanguage(path: string): string {
    const extension = path.split('.').pop()?.toLowerCase();

    const languages: Record<string, string> = {
      java: 'java',
      ts: 'typescript',
      tsx: 'typescript',
      js: 'javascript',
      jsx: 'javascript',
      html: 'html',
      htm: 'html',
      css: 'css',
      scss: 'scss',
      less: 'less',
      json: 'json',
      xml: 'xml',
      md: 'markdown',
      yaml: 'yaml',
      yml: 'yaml',
      sql: 'sql',
      py: 'python',
      properties: 'ini',
      sh: 'shell',
      bash: 'shell',
    };

    return languages[extension ?? ''] ?? 'plaintext';
  }

  private setActiveModel(): void {
    if (!this.editor || !this.monaco) return;

    const path = this.selectedFilePath();
    const model = this.models.get(path);

    if (model) {
      this.editor.setModel(model);
      this.editor.focus();
      this.fileContent.set(model.getValue());
    }
  }

  loadWorkspace(): void {
    this.loading.set(true);
    this.error.set('');

    this.api.getWorkspace().subscribe({
      next: result => {
        this.workspacePath.set(result.path);
        this.loadFiles('');
      },
      error: () => {
        this.error.set('دریافت اطلاعات Workspace ناموفق بود.');
        this.loading.set(false);
      },
    });
  }

  loadFiles(path: string): void {
    this.loading.set(true);
    this.error.set('');

    this.api.getFiles(path).subscribe({
      next: result => {
        this.currentPath.set(result.path);
        this.files.set(result.files);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('دریافت فایل‌ها ناموفق بود.');
        this.loading.set(false);
      },
    });
  }

  openFile(file: WorkspaceFile): void {
    if (file.directory) {
      this.loadFiles(file.path);
      return;
    }

    const existingTab = this.tabs().find(tab => tab.path === file.path);

    if (existingTab) {
      this.activateTab(file.path);
      return;
    }

    this.fileLoading.set(true);
    this.fileError.set('');

    this.api.readFile(file.path).subscribe({
      next: result => {
        this.savedContents.set(result.path, result.content);

        this.tabs.update(tabs => [
          ...tabs,
          {
            path: result.path,
            name: file.name,
            content: result.content,
            dirty: false,
          },
        ]);

        this.selectedFilePath.set(result.path);
        this.fileContent.set(result.content);
        this.editorContextChange.emit({ path: result.path, content: result.content });
        this.fileLoading.set(false);
        this.fileSaved.set(false);

        const model = this.createModel(result.path, result.content);

        if (model && this.editor) {
          this.editor.setModel(model);
          this.editor.focus();
        }
      },
      error: error => {
        this.fileError.set(
          error.error?.message || 'باز کردن فایل ناموفق بود.'
        );
        this.fileLoading.set(false);
      },
    });
  }

  activateTab(path: string): void {
    const tab = this.tabs().find(item => item.path === path);
    if (!tab) return;

    this.selectedFilePath.set(path);
    this.fileContent.set(this.models.get(path)?.getValue() ?? tab.content);
    this.editorContextChange.emit({ path, content: this.fileContent() });
    this.fileSaved.set(false);
    this.fileError.set('');

    const model = this.models.get(path);

    if (this.editor && model) {
      this.editor.setModel(model);
      this.editor.focus();
    }
  }

  closeTab(path: string): void {
    const tab = this.tabs().find(item => item.path === path);
    if (!tab) return;

    if (tab.dirty && !confirm(`تغییرات فایل ${tab.name} ذخیره نشده‌اند. تب بسته شود؟`)) {
      return;
    }

    this.models.get(path)?.dispose();
    this.models.delete(path);
    this.savedContents.delete(path);

    const remaining = this.tabs().filter(item => item.path !== path);
    this.tabs.set(remaining);

    if (this.selectedFilePath() === path) {
      const next = remaining[remaining.length - 1];

      if (next) {
        this.activateTab(next.path);
      } else {
        this.selectedFilePath.set('');
        this.fileContent.set('');
        this.editorContextChange.emit({ path: '', content: '' });
        this.fileSaved.set(false);
      }
    }
  }

  closeFile(): void {
    const path = this.selectedFilePath();
    if (path) this.closeTab(path);
  }

  updateFileContent(content: string): void {
    this.fileContent.set(content);

    const model = this.models.get(this.selectedFilePath());

    if (model && model.getValue() !== content) {
      model.setValue(content);
    }
  }

  saveFile(): void {
    const path = this.selectedFilePath();
    const model = this.models.get(path);

    if (!path || !model || this.saving()) return;

    const content = model.getValue();

    this.saving.set(true);
    this.fileError.set('');
    this.fileSaved.set(false);

    this.api.saveFile(path, content).subscribe({
      next: () => {
        this.savedContents.set(path, content);

        this.tabs.update(tabs =>
          tabs.map(tab =>
            tab.path === path
              ? { ...tab, content, dirty: false }
              : tab
          )
        );

        this.saving.set(false);
        this.fileSaved.set(true);
      },
      error: error => {
        this.saving.set(false);
        this.fileError.set(
          error.error?.message || 'ذخیره فایل ناموفق بود.'
        );
      },
    });
  }

  goBack(): void {
    const current = this.currentPath();
    if (!current) return;

    const parts = current.split('/').filter(Boolean);
    parts.pop();

    this.loadFiles(parts.join('/'));
  }

  changeWorkspace(): void {
    const path = this.newWorkspacePath().trim();

    if (!path || this.changingWorkspace()) return;

    this.changingWorkspace.set(true);
    this.error.set('');

    this.api.setWorkspace(path).subscribe({
      next: result => {
        this.workspacePath.set(result.path);
        this.newWorkspacePath.set('');
        this.currentPath.set('');
        this.loadFiles('');
        this.changingWorkspace.set(false);
      },
      error: error => {
        this.error.set(
          error.error?.message || 'تغییر Workspace ناموفق بود.'
        );
        this.changingWorkspace.set(false);
      },
    });
  }

  protected getFileIcon(file: WorkspaceFile): string {
    if (file.directory) return '📁';

    const extension = file.name.split('.').pop()?.toLowerCase();

    const icons: Record<string, string> = {
      java: '☕',
      ts: '🟦',
      html: '🌐',
      scss: '🎨',
      css: '🎨',
      json: '📋',
      xml: '📋',
      md: '📝',
      yml: '⚙',
      yaml: '⚙',
      properties: '⚙',
      sql: '🗄',
    };

    return icons[extension ?? ''] ?? '📄';
  }
}


import { Component, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AgentApiService } from './core/agent-api.service';
import { WorkspaceComponent } from './features/workspace/workspace.component';

interface ChatMessage {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  time: string;
  status?: string;
  taskId?: string;
}

interface AgentTaskResponse {
  id: string;
  instruction: string;
  result: string;
  status: string;
  error: string | null;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule, WorkspaceComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {

  private readonly agentApi = inject(AgentApiService);

  protected readonly isSending = signal(false);

  protected readonly selectedNav = signal('chat');
  protected readonly activeMode = signal<'chat' | 'terminal' | 'workspace'>('chat');

  protected readonly messages = signal<ChatMessage[]>([]);
  protected readonly messageInput = signal('');
  protected readonly terminalInput = signal('');

  protected readonly terminalHistory = signal<string[]>([
    'JavadAI Terminal v1.0',
    'محیط ترمینال آزمایشی آماده است.',
    'برای مشاهده راهنما، help را وارد کنید.',
  ]);

  protected readonly navItems = [
    { id: 'chat', label: 'گفتگو', icon: '◈' },
    { id: 'terminal', label: 'ترمینال', icon: '⌘' },
    { id: 'workspace', label: 'فضای کاری', icon: '▣' },
    { id: 'tasks', label: 'وظایف', icon: '☷' },
    { id: 'memory', label: 'حافظه', icon: '◉' },
    { id: 'settings', label: 'تنظیمات', icon: '⚙' },
  ];

  protected selectNav(id: string): void {
    this.selectedNav.set(id);

    if (id === 'terminal') {
      this.activeMode.set('terminal');
    } else if (id === 'chat') {
      this.activeMode.set('chat');
    } else if (id === 'workspace') {
      this.activeMode.set('workspace');
    }
  }

  protected sendMessage(): void {
    const content = this.messageInput().trim();

    if (!content || this.isSending()) {
      return;
    }

    const now = this.getCurrentTime();
    const userMessageId = Date.now();
    const assistantMessageId = userMessageId + 1;

    this.messages.update(current => [
      ...current,
      {
        id: userMessageId,
        role: 'user',
        content,
        time: now,
      },
      {
        id: assistantMessageId,
        role: 'assistant',
        content: 'در حال بررسی درخواست شما...',
        time: this.getCurrentTime(),
        status: 'RUNNING',
      },
    ]);

    this.messageInput.set('');
    this.isSending.set(true);

    this.agentApi.ask(content).subscribe({

      next: response => {
        const task = response as AgentTaskResponse;

        this.messages.update(current =>
          current.map(message =>
            message.id === assistantMessageId
              ? {
                ...message,
                content: task.error
                  ? `خطا: ${task.error}`
                  : task.result || 'پاسخی دریافت نشد.',
                status: task.status,
                taskId: task.id,
                time: this.getCurrentTime(),
              }
              : message
          )
        );

        this.isSending.set(false);
      },

      error: error => {
        const errorMessage =
          error.error?.message ||
          error.message ||
          'ارتباط با سرور برقرار نشد.';

        this.messages.update(current =>
          current.map(message =>
            message.id === assistantMessageId
              ? {
                ...message,
                content: `ارتباط با JavadAI Backend برقرار نشد:\n${errorMessage}`,
                status: 'FAILED',
                time: this.getCurrentTime(),
              }
              : message
          )
        );

        this.isSending.set(false);
      },
    });
  }

  protected handleComposerKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  protected getStatusLabel(status?: string): string {
    switch (status) {
      case 'PENDING':
        return 'در انتظار اجرا';
      case 'RUNNING':
        return 'در حال اجرا';
      case 'WAITING_APPROVAL':
        return 'در انتظار تأیید';
      case 'COMPLETED':
        return 'با موفقیت انجام شد';
      case 'FAILED':
        return 'ناموفق';
      case 'CANCELLED':
        return 'لغو شده';
      default:
        return '';
    }
  }

  protected getCurrentTime(): string {
    return new Date().toLocaleTimeString('fa-IR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  protected useSuggestion(text: string): void {
    this.messageInput.set(text);
  }

  protected runTerminalCommand(): void {
    const command = this.terminalInput().trim();

    if (!command) return;

    let output = '';

    switch (command.toLowerCase()) {
      case 'help':
        output = 'دستورات آزمایشی: help, clear, date, whoami';
        break;

      case 'clear':
        this.terminalHistory.set([]);
        this.terminalInput.set('');
        return;

      case 'date':
        output = new Date().toLocaleString('fa-IR');
        break;

      case 'whoami':
        output = 'JavadAI User';
        break;

      default:
        output = 'این ترمینال فعلاً آزمایشی است.';
    }

    this.terminalHistory.update(history => [
      ...history,
      `> ${command}`,
      output,
    ]);

    this.terminalInput.set('');
  }
}

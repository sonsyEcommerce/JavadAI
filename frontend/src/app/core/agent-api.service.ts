
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AgentApiService {
  private readonly http = inject(HttpClient);

  private readonly baseUrl = 'http://localhost:8081/api/agent';

  askWithContext(instruction: string, activeFilePath: string, activeFileContent: string): Observable<unknown> {
    return this.http.post<unknown>(`${this.baseUrl}/ask-with-context`, {
      instruction,
      activeFilePath,
      activeFileContent,
    });
  }

  ask(message: string): Observable<unknown> {
    const headers = new HttpHeaders({
      'Content-Type': 'text/plain; charset=UTF-8',
      Accept: 'application/json',
    });

    return this.http.post<unknown>(
      `${this.baseUrl}/ask`,
      message,
      { headers }
    );
  }

  getTask(taskId: string): Observable<unknown> {
    return this.http.get<unknown>(
      `${this.baseUrl}/tasks/${encodeURIComponent(taskId)}`
    );
  }
}

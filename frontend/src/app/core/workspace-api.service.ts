
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface WorkspaceResponse {
  path: string;
}

export interface WorkspaceFile {
  name: string;
  path: string;
  directory: boolean;
}

export interface WorkspaceFilesResponse {
  path: string;
  files: WorkspaceFile[];
}

@Injectable({
  providedIn: 'root',
})
export class WorkspaceApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/workspace';

  getWorkspace(): Observable<WorkspaceResponse> {
    return this.http.get<WorkspaceResponse>(this.baseUrl);
  }

  setWorkspace(path: string): Observable<WorkspaceResponse> {
    return this.http.put<WorkspaceResponse>(this.baseUrl, { path });
  }

  getFiles(path = ''): Observable<WorkspaceFilesResponse> {
    const params = new HttpParams().set('path', path);

    return this.http.get<WorkspaceFilesResponse>(
      `${this.baseUrl}/files`,
      { params }
    );
  }

  readFile(path: string): Observable<{ path: string; content: string }> {
    const params = new HttpParams().set('path', path);

    return this.http.get<{ path: string; content: string }>(
      `${this.baseUrl}/file`,
      { params }
    );
  }

  saveFile(path: string, content: string): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(
      `${this.baseUrl}/file`,
      { path, content }
    );
  }
}

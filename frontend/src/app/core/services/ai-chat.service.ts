import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AiAvailability,
  ChatMessage,
  ChatMessageRequest,
  ChatResponse,
  GenerateSummaryRequest,
  SummaryResponse,
} from '../models/ai-chat.model';

@Injectable({
  providedIn: 'root',
})
export class AiChatService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/manager/ai-chat`;

  readonly aiAvailable = signal<boolean>(false);
  private availabilityChecked = false;

  checkAvailability(force = false): Observable<AiAvailability> {
    if (this.availabilityChecked && !force) {
      return of({ enabled: this.aiAvailable() });
    }

    return this.http.get<AiAvailability>(`${this.baseUrl}/availability`, { withCredentials: true }).pipe(
      tap((res) => {
        this.aiAvailable.set(res?.enabled ?? false);
        this.availabilityChecked = true;
      }),
      catchError(() => {
        this.aiAvailable.set(false);
        this.availabilityChecked = true;
        return of({ enabled: false });
      }),
    );
  }

  sendMessage(history: ChatMessage[], message: string): Observable<ChatResponse> {
    const payload: ChatMessageRequest = { history, message };
    return this.http.post<ChatResponse>(`${this.baseUrl}/message`, payload, { withCredentials: true });
  }

  generateSummary(weekStartDate: string, projectId?: number): Observable<SummaryResponse> {
    const payload: GenerateSummaryRequest = { weekStartDate, projectId };
    return this.http.post<SummaryResponse>(`${this.baseUrl}/summary`, payload, { withCredentials: true });
  }
}

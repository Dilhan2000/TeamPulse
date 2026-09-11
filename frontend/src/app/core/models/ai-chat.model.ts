export type ChatRole = 'user' | 'assistant';

export interface ChatMessage {
  role: ChatRole;
  content: string;
}

export interface ChatMessageRequest {
  history: ChatMessage[];
  message: string;
}

export interface ChatResponse {
  reply: string;
}

export interface GenerateSummaryRequest {
  weekStartDate: string;
  projectId?: number;
}

export interface SummaryResponse {
  summary: string;
}

export interface AiAvailability {
  enabled: boolean;
}

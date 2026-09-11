import {
  Component,
  ElementRef,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { AuthService } from '../../../core/services/auth.service';
import { AiChatService } from '../../../core/services/ai-chat.service';
import { ChatMessage } from '../../../core/models/ai-chat.model';

@Component({
  selector: 'app-ai-chat-widget',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatChipsModule,
  ],
  templateUrl: './ai-chat-widget.component.html',
  styleUrls: ['./ai-chat-widget.component.scss'],
})
export class AiChatWidgetComponent {
  private readonly authService = inject(AuthService);
  private readonly aiChatService = inject(AiChatService);

  @ViewChild('scrollContainer') private scrollContainer?: ElementRef<HTMLDivElement>;
  @ViewChild('chatInput') private chatInput?: ElementRef<HTMLTextAreaElement>;

  readonly shouldRender = computed(
    () => this.authService.isManager() && this.aiChatService.aiAvailable(),
  );

  readonly isOpen = signal<boolean>(false);
  readonly isSending = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly inputMessage = signal<string>('');
  readonly messages = signal<ChatMessage[]>([]);

  readonly quickPrompts = [
    "What did the team accomplish this week?",
    "Are there any blockers reported?",
    "Who hasn't submitted their report yet?",
    "Summarize recent project milestones",
  ];

  toggleOpen(): void {
    const nextState = !this.isOpen();
    this.isOpen.set(nextState);
    if (nextState) {
      this.scrollToBottom();
      setTimeout(() => this.chatInput?.nativeElement?.focus(), 150);
    }
  }

  close(): void {
    this.isOpen.set(false);
  }

  clearHistory(): void {
    this.messages.set([]);
    this.errorMessage.set(null);
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  sendQuickPrompt(prompt: string): void {
    this.inputMessage.set(prompt);
    this.sendMessage();
  }

  sendMessage(): void {
    const text = this.inputMessage().trim();
    if (!text || this.isSending()) {
      return;
    }

    const currentHistory = [...this.messages()];
    const userMessage: ChatMessage = { role: 'user', content: text };

    this.messages.update((list) => [...list, userMessage]);
    this.inputMessage.set('');
    this.errorMessage.set(null);
    this.isSending.set(true);
    this.scrollToBottom();

    this.aiChatService.sendMessage(currentHistory, text).subscribe({
      next: (res) => {
        const assistantMessage: ChatMessage = { role: 'assistant', content: res.reply };
        this.messages.update((list) => [...list, assistantMessage]);
        this.isSending.set(false);
        this.scrollToBottom();
      },
      error: (err) => {
        this.isSending.set(false);
        if (err.status === 503) {
          this.errorMessage.set('AI assistant is not configured.');
        } else if (err.status === 502) {
          this.errorMessage.set('AI assistant is temporarily unavailable — please try again.');
        } else {
          this.errorMessage.set(err.error?.message || 'Failed to get response from AI assistant.');
        }
        this.scrollToBottom();
      },
    });
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.scrollContainer?.nativeElement) {
        this.scrollContainer.nativeElement.scrollTop =
          this.scrollContainer.nativeElement.scrollHeight;
      }
    }, 50);
  }
}

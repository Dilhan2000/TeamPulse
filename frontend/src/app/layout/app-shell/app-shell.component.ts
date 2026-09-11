import { Component, OnInit, effect, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../navbar/navbar.component';
import { AiChatWidgetComponent } from '../../shared/components/ai-chat-widget/ai-chat-widget.component';
import { AuthService } from '../../core/services/auth.service';
import { AiChatService } from '../../core/services/ai-chat.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, AiChatWidgetComponent],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
})
export class AppShellComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly aiChatService = inject(AiChatService);

  constructor() {
    effect(() => {
      if (this.authService.isManager()) {
        this.aiChatService.checkAvailability().subscribe();
      }
    });
  }

  ngOnInit(): void {
    if (this.authService.isManager()) {
      this.aiChatService.checkAvailability().subscribe();
    }
  }
}


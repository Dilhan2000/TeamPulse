import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';

import { AiChatWidgetComponent } from './ai-chat-widget.component';
import { AuthService } from '../../../core/services/auth.service';
import { AiChatService } from '../../../core/services/ai-chat.service';
import { User } from '../../../core/models/user.model';

describe('AiChatWidgetComponent', () => {
  let component: AiChatWidgetComponent;
  let fixture: ComponentFixture<AiChatWidgetComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let aiChatServiceSpy: jasmine.SpyObj<AiChatService>;

  const mockManager: User = {
    id: 1,
    fullName: 'Manager Bob',
    email: 'manager@example.com',
    role: 'MANAGER',
    status: 'ACTIVE',
  };

  beforeEach(async () => {
    const isManagerSignal = signal<boolean>(true);
    const aiAvailableSignal = signal<boolean>(true);

    authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      isManager: isManagerSignal,
      currentUser: signal<User | null>(mockManager),
    });

    aiChatServiceSpy = jasmine.createSpyObj('AiChatService', ['sendMessage', 'checkAvailability'], {
      aiAvailable: aiAvailableSignal,
    });

    await TestBed.configureTestingModule({
      imports: [AiChatWidgetComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: AiChatService, useValue: aiChatServiceSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AiChatWidgetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Visibility Gating (C8-T11, C8-T13)', () => {
    it('should render when user is MANAGER and AI is available', () => {
      expect(component.shouldRender()).toBe(true);
      const fab = fixture.nativeElement.querySelector('.ai-widget-fab');
      expect(fab).toBeTruthy();
    });

    it('should NOT render when user is not MANAGER', () => {
      (authServiceSpy.isManager as any).set(false);
      fixture.detectChanges();

      expect(component.shouldRender()).toBe(false);
      const fab = fixture.nativeElement.querySelector('.ai-widget-fab');
      expect(fab).toBeFalsy();
    });

    it('should NOT render when AI is disabled/unavailable', () => {
      (aiChatServiceSpy.aiAvailable as any).set(false);
      fixture.detectChanges();

      expect(component.shouldRender()).toBe(false);
      const fab = fixture.nativeElement.querySelector('.ai-widget-fab');
      expect(fab).toBeFalsy();
    });
  });

  describe('Interactions and Panel State', () => {
    it('should toggle panel open and close', () => {
      expect(component.isOpen()).toBe(false);
      component.toggleOpen();
      expect(component.isOpen()).toBe(true);

      component.close();
      expect(component.isOpen()).toBe(false);
    });

    it('should clear conversation history', () => {
      component.messages.set([
        { role: 'user', content: 'Hi' },
        { role: 'assistant', content: 'Hello' },
      ]);
      component.errorMessage.set('Some error');

      component.clearHistory();

      expect(component.messages().length).toBe(0);
      expect(component.errorMessage()).toBeNull();
    });
  });

  describe('Message Exchange (C8-T11)', () => {
    it('should not send if message is empty or whitespace', () => {
      component.inputMessage.set('   ');
      component.sendMessage();

      expect(component.messages().length).toBe(0);
      expect(aiChatServiceSpy.sendMessage).not.toHaveBeenCalled();
    });

    it('should send message, append user query, call service, and append reply', fakeAsync(() => {
      aiChatServiceSpy.sendMessage.and.returnValue(
        of({ reply: 'Here is the report summary for the team.' }),
      );

      component.inputMessage.set('What did Alice accomplish?');
      component.sendMessage();
      tick(100);

      // Immediately, user message should be added and input cleared
      expect(component.messages().length).toBe(2); // user + assistant immediately resolved by of()
      expect(component.messages()[0]).toEqual({
        role: 'user',
        content: 'What did Alice accomplish?',
      });
      expect(component.messages()[1]).toEqual({
        role: 'assistant',
        content: 'Here is the report summary for the team.',
      });
      expect(component.isSending()).toBe(false);
      expect(component.inputMessage()).toBe('');
      expect(aiChatServiceSpy.sendMessage).toHaveBeenCalledWith(
        [],
        'What did Alice accomplish?',
      );
    }));

    it('should handle 503 Service Unavailable error gracefully', fakeAsync(() => {
      aiChatServiceSpy.sendMessage.and.returnValue(
        throwError(() => ({ status: 503, error: { message: 'AI assistant is not configured' } })),
      );

      component.inputMessage.set('Hello');
      component.sendMessage();
      tick(100);

      expect(component.isSending()).toBe(false);
      expect(component.errorMessage()).toBe('AI assistant is not configured.');
      expect(component.messages().length).toBe(1); // Only user message
    }));

    it('should handle 502 Bad Gateway error gracefully', fakeAsync(() => {
      aiChatServiceSpy.sendMessage.and.returnValue(
        throwError(() => ({
          status: 502,
          error: { message: 'Upstream Anthropic failure' },
        })),
      );

      component.inputMessage.set('Hello');
      component.sendMessage();
      tick(100);

      expect(component.isSending()).toBe(false);
      expect(component.errorMessage()).toBe(
        'AI assistant is temporarily unavailable — please try again.',
      );
    }));

    it('should handle keyboard events on textarea', () => {
      spyOn(component, 'sendMessage');

      const enterEvent = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: false });
      spyOn(enterEvent, 'preventDefault');
      component.onKeyDown(enterEvent);

      expect(enterEvent.preventDefault).toHaveBeenCalled();
      expect(component.sendMessage).toHaveBeenCalled();

      // Shift+Enter should NOT send
      (component.sendMessage as jasmine.Spy).calls.reset();
      const shiftEnterEvent = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true });
      component.onKeyDown(shiftEnterEvent);
      expect(component.sendMessage).not.toHaveBeenCalled();
    });

    it('should send quick prompt when clicked', () => {
      spyOn(component, 'sendMessage');
      component.sendQuickPrompt('Are there any blockers reported?');

      expect(component.inputMessage()).toBe('Are there any blockers reported?');
      expect(component.sendMessage).toHaveBeenCalled();
    });
  });
});

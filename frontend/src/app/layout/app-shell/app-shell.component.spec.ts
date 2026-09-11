import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { signal } from '@angular/core';

import { AppShellComponent } from './app-shell.component';
import { AuthService } from '../../core/services/auth.service';
import { AiChatService } from '../../core/services/ai-chat.service';

describe('AppShellComponent', () => {
  let component: AppShellComponent;
  let fixture: ComponentFixture<AppShellComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let aiChatServiceSpy: jasmine.SpyObj<AiChatService>;

  beforeEach(async () => {
    const isManagerSignal = signal<boolean>(true);
    const aiAvailableSignal = signal<boolean>(true);

    authServiceSpy = jasmine.createSpyObj('AuthService', ['logout'], {
      isManager: isManagerSignal,
      isTeamMember: signal<boolean>(false),
      isAdmin: signal<boolean>(false),
      isAuthenticated: signal<boolean>(true),
      userRole: signal<'MANAGER'>('MANAGER'),
      currentUser: signal({ id: 1, fullName: 'Manager', role: 'MANAGER', status: 'ACTIVE' }),
    });

    aiChatServiceSpy = jasmine.createSpyObj('AiChatService', ['checkAvailability', 'sendMessage'], {
      aiAvailable: aiAvailableSignal,
    });
    aiChatServiceSpy.checkAvailability.and.returnValue(of({ enabled: true }));

    await TestBed.configureTestingModule({
      imports: [AppShellComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: AiChatService, useValue: aiChatServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and call checkAvailability for managers', () => {
    expect(component).toBeTruthy();
    expect(aiChatServiceSpy.checkAvailability).toHaveBeenCalled();
  });
});

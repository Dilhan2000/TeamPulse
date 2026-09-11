import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { MessageResponse, User } from '../models/user.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockUser: User = {
    id: 1,
    fullName: 'Test Admin',
    email: 'admin@weeklyreport.local',
    role: 'ADMIN',
    status: 'ACTIVE',
  };

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created and have initial null currentUser', () => {
    expect(service).toBeTruthy();
    expect(service.currentUser()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('login() should set currentUser on 200 OK', () => {
    service.login('admin@weeklyreport.local', 'Password123!').subscribe((user) => {
      expect(user).toEqual(mockUser);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockUser);

    expect(service.currentUser()).toEqual(mockUser);
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.isAdmin()).toBeTrue();
  });

  it('login() should propagate 401 error', () => {
    let errorStatus = 0;
    service.login('wrong@example.com', 'badpass').subscribe({
      error: (err) => {
        errorStatus = err.status;
      },
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    req.flush(
      { message: 'Invalid email or password.' },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(errorStatus).toBe(401);
    expect(service.currentUser()).toBeNull();
  });

  it('login() should propagate 403 error for pending user', () => {
    let errorStatus = 0;
    let errorMessage = '';
    service.login('pending@example.com', 'Password123!').subscribe({
      error: (err) => {
        errorStatus = err.status;
        errorMessage = err.error?.message;
      },
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    req.flush(
      { message: 'Your account is pending administrator approval.' },
      { status: 403, statusText: 'Forbidden' },
    );

    expect(errorStatus).toBe(403);
    expect(errorMessage).toBe('Your account is pending administrator approval.');
  });

  it('register() should succeed on 201 Created', () => {
    const mockResponse: MessageResponse = {
      message: 'Registration submitted. An administrator will review your account.',
    };

    service
      .register('New User', 'new@example.com', 'Password123!', 'TEAM_MEMBER')
      .subscribe((res) => {
        expect(res.message).toContain('Registration submitted');
      });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/register`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse, { status: 201, statusText: 'Created' });
  });

  it('register() should propagate 409 Conflict for existing email', () => {
    let errorStatus = 0;
    service.register('Existing', 'existing@example.com', 'Password123!', 'TEAM_MEMBER').subscribe({
      error: (err) => {
        errorStatus = err.status;
      },
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/register`);
    req.flush({ message: 'Email is already registered' }, { status: 409, statusText: 'Conflict' });

    expect(errorStatus).toBe(409);
  });

  it('logout() should clear currentUser and navigate to /login', () => {
    service.currentUser.set(mockUser);

    service.logout().subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/logout`);
    expect(req.request.method).toBe('POST');
    req.flush(null);

    expect(service.currentUser()).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('fetchCurrentUser() should set user on success and return null on 401', () => {
    // 1. Success case
    service.fetchCurrentUser().subscribe((user) => {
      expect(user).toEqual(mockUser);
    });

    let req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/me`);
    req.flush(mockUser);
    expect(service.currentUser()).toEqual(mockUser);

    // 2. 401 Unauthorized case
    service.fetchCurrentUser().subscribe((user) => {
      expect(user).toBeNull();
    });

    req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/me`);
    req.flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(service.currentUser()).toBeNull();
  });

  it('changePassword() should send PATCH /api/auth/me/password', () => {
    const payload = { currentPassword: 'OldPassword123!', newPassword: 'NewPassword123!' };
    const mockResponse = { message: 'Password changed successfully.' };

    service.changePassword(payload).subscribe((res) => {
      expect(res.message).toBe('Password changed successfully.');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/me/password`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(payload);
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockResponse);
  });
});

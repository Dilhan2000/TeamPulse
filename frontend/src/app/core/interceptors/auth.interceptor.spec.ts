import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockUser: User = {
    id: 1,
    fullName: 'Test User',
    email: 'test@example.com',
    role: 'TEAM_MEMBER',
    status: 'ACTIVE',
  };

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['refresh'], {
      currentUser: jasmine.createSpyObj('currentUser', ['set']),
    });
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should attach withCredentials to API requests', () => {
    http.get(`${environment.apiBaseUrl}/some-data`).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/some-data`);
    expect(req.request.withCredentials).toBeTrue();
    req.flush({ data: 123 });
  });

  it('should attempt refresh and retry on 401 from protected endpoint', () => {
    authServiceSpy.refresh.and.returnValue(of(mockUser));

    let responseData: unknown;
    http.get(`${environment.apiBaseUrl}/reports`).subscribe((data) => {
      responseData = data;
    });

    // 1st request fails with 401
    const initialReq = httpMock.expectOne(`${environment.apiBaseUrl}/reports`);
    initialReq.flush({ message: 'Token expired' }, { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.refresh).toHaveBeenCalled();

    // Retried request should have X-Auth-Retry header
    const retryReq = httpMock.expectOne(`${environment.apiBaseUrl}/reports`);
    expect(retryReq.request.headers.get('X-Auth-Retry')).toBe('true');
    retryReq.flush({ success: true });

    expect(responseData).toEqual({ success: true });
  });

  it('should navigate to /login and not loop if refresh fails on 401', () => {
    authServiceSpy.refresh.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' })),
    );

    let errorReceived: unknown;
    http.get(`${environment.apiBaseUrl}/reports`).subscribe({
      error: (err) => {
        errorReceived = err;
      },
    });

    // 1st request fails with 401
    const initialReq = httpMock.expectOne(`${environment.apiBaseUrl}/reports`);
    initialReq.flush({ message: 'Token expired' }, { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.refresh).toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    expect(errorReceived).toBeTruthy();
  });
});

import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, MessageResponse, RegisterRequest, Role, User } from '../models/user.model';
import { ChangePasswordRequest } from '../models/user-profile.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly baseUrl = environment.apiBaseUrl;

  readonly currentUser = signal<User | null>(null);
  readonly isAuthenticated = computed(() => this.currentUser() !== null);
  readonly userRole = computed(() => this.currentUser()?.role ?? null);
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');
  readonly isManager = computed(() => this.currentUser()?.role === 'MANAGER');
  readonly isTeamMember = computed(() => this.currentUser()?.role === 'TEAM_MEMBER');

  login(email: string, password: string): Observable<User> {
    const payload: LoginRequest = { email, password };
    return this.http
      .post<User>(`${this.baseUrl}/auth/login`, payload, { withCredentials: true })
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  register(
    fullName: string,
    email: string,
    password: string,
    requestedRole: Role,
  ): Observable<MessageResponse> {
    const payload: RegisterRequest = { fullName, email, password, requestedRole };
    return this.http.post<MessageResponse>(`${this.baseUrl}/auth/register`, payload, {
      withCredentials: true,
    });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/logout`, {}, { withCredentials: true }).pipe(
      catchError(() => of(undefined as void)),
      tap(() => {
        this.currentUser.set(null);
        this.router.navigate(['/login']);
      }),
      map(() => undefined as void),
    );
  }

  refresh(): Observable<User> {
    return this.http
      .post<User>(`${this.baseUrl}/auth/refresh`, {}, { withCredentials: true })
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  fetchCurrentUser(): Observable<User | null> {
    return this.http.get<User>(`${this.baseUrl}/auth/me`, { withCredentials: true }).pipe(
      tap((user) => this.currentUser.set(user)),
      catchError(() => {
        this.currentUser.set(null);
        return of(null);
      }),
    );
  }

  changePassword(req: ChangePasswordRequest): Observable<MessageResponse> {
    return this.http.patch<MessageResponse>(`${this.baseUrl}/auth/me/password`, req, {
      withCredentials: true,
    });
  }
}

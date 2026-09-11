import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Attach withCredentials: true if targeting our backend API
  let authReq = req;
  if (req.url.startsWith(environment.apiBaseUrl)) {
    authReq = req.clone({ withCredentials: true });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // If error is 401 and request was not already a retry or an auth endpoint
      const isAuthEndpoint =
        req.url.includes('/auth/login') ||
        req.url.includes('/auth/register') ||
        req.url.includes('/auth/refresh') ||
        req.url.includes('/auth/me');

      const isRetry = req.headers.has('X-Auth-Retry');

      if (error.status === 401 && !isAuthEndpoint && !isRetry) {
        return authService.refresh().pipe(
          switchMap(() => {
            const retryReq = authReq.clone({
              headers: authReq.headers.set('X-Auth-Retry', 'true'),
              withCredentials: true,
            });
            return next(retryReq);
          }),
          catchError((refreshError) => {
            authService.currentUser.set(null);
            router.navigate(['/login']);
            return throwError(() => refreshError);
          }),
        );
      }

      return throwError(() => error);
    }),
  );
};

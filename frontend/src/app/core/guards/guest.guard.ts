import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const user = authService.currentUser();

  if (!user) {
    return true;
  }

  // If already authenticated, redirect to role home
  if (user.role === 'ADMIN') {
    return router.createUrlTree(['/admin/users/pending']);
  }
  return router.createUrlTree(['/reports']);
};

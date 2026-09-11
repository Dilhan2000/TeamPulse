import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { User } from '../models/user.model';

describe('authGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  const mockUser: User = {
    id: 1,
    fullName: 'Test User',
    email: 'user@example.com',
    role: 'TEAM_MEMBER',
    status: 'ACTIVE',
  };

  const dummyRoute = {} as ActivatedRouteSnapshot;
  const dummyState = { url: '/protected-page' } as RouterStateSnapshot;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      currentUser: jasmine.createSpy('currentUser'),
    });

    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: authServiceSpy }],
    });

    router = TestBed.inject(Router);
  });

  it('should activate when user is authenticated', () => {
    (authServiceSpy.currentUser as jasmine.Spy).and.returnValue(mockUser);

    const result = TestBed.runInInjectionContext(() => authGuard(dummyRoute, dummyState));
    expect(result).toBeTrue();
  });

  it('should redirect to /login with returnUrl when user is unauthenticated', () => {
    (authServiceSpy.currentUser as jasmine.Spy).and.returnValue(null);

    const result = TestBed.runInInjectionContext(() => authGuard(dummyRoute, dummyState));
    expect(result instanceof UrlTree).toBeTrue();

    const urlTree = result as UrlTree;
    expect(urlTree.toString()).toContain('/login?returnUrl=%2Fprotected-page');
  });
});

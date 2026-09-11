import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';
import { User } from '../models/user.model';

describe('roleGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  const dummyRoute = {} as ActivatedRouteSnapshot;
  const dummyState = { url: '/admin/users' } as RouterStateSnapshot;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      currentUser: jasmine.createSpy('currentUser'),
    });

    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: authServiceSpy }],
    });

    router = TestBed.inject(Router);
  });

  it('should redirect to /login if unauthenticated', () => {
    (authServiceSpy.currentUser as jasmine.Spy).and.returnValue(null);

    const guard = roleGuard(['ADMIN']);
    const result = TestBed.runInInjectionContext(() => guard(dummyRoute, dummyState));

    expect(result instanceof UrlTree).toBeTrue();
    expect((result as UrlTree).toString()).toContain('/login?returnUrl=%2Fadmin%2Fusers');
  });

  it('should allow access if user has one of allowed roles', () => {
    const adminUser: User = {
      id: 1,
      fullName: 'Admin',
      email: 'admin@example.com',
      role: 'ADMIN',
      status: 'ACTIVE',
    };
    (authServiceSpy.currentUser as jasmine.Spy).and.returnValue(adminUser);

    const guard = roleGuard(['ADMIN']);
    const result = TestBed.runInInjectionContext(() => guard(dummyRoute, dummyState));

    expect(result).toBeTrue();
  });

  it('should redirect to /unauthorized if user lacks allowed role', () => {
    const memberUser: User = {
      id: 2,
      fullName: 'Member',
      email: 'member@example.com',
      role: 'TEAM_MEMBER',
      status: 'ACTIVE',
    };
    (authServiceSpy.currentUser as jasmine.Spy).and.returnValue(memberUser);

    const guard = roleGuard(['ADMIN']);
    const result = TestBed.runInInjectionContext(() => guard(dummyRoute, dummyState));

    expect(result instanceof UrlTree).toBeTrue();
    expect((result as UrlTree).toString()).toBe('/unauthorized');
  });
});

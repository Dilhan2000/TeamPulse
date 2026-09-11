import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { AccountSettingsComponent } from './account-settings.component';
import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../../core/models/user.model';

describe('AccountSettingsComponent', () => {
  let component: AccountSettingsComponent;
  let fixture: ComponentFixture<AccountSettingsComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  const mockUser: User = {
    id: 10,
    fullName: 'Jane Doe',
    email: 'jane@example.com',
    role: 'MANAGER',
    status: 'ACTIVE',
    createdAt: '2025-06-01T10:00:00Z',
  };

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj(
      'AuthService',
      ['changePassword', 'fetchCurrentUser'],
      {
        currentUser: () => mockUser,
      }
    );
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    authServiceSpy.fetchCurrentUser.and.returnValue(of(mockUser));

    await TestBed.configureTestingModule({
      imports: [AccountSettingsComponent, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    })
      .overrideComponent(AccountSettingsComponent, {
        set: {
          providers: [{ provide: MatSnackBar, useValue: snackBarSpy }],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(AccountSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and display profile info for current user', () => {
    expect(component).toBeTruthy();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.user-name')?.textContent).toContain('Jane Doe');
    expect(el.querySelector('.user-email')?.textContent).toContain('jane@example.com');
    expect(el.querySelector('.role-badge')?.textContent).toContain('Manager');
    expect(el.querySelector('.user-avatar')?.textContent?.trim()).toBe('JD');
  });

  it('should validate password form controls properly', () => {
    const form = component.passwordForm;
    expect(form.valid).toBeFalse();

    form.patchValue({
      currentPassword: 'oldPass',
      newPassword: 'short',
      confirmPassword: 'short',
    });
    // minlength is 8
    expect(form.get('newPassword')?.hasError('minlength')).toBeTrue();

    // password without digits
    form.patchValue({
      newPassword: 'lettersOnlyHere',
      confirmPassword: 'lettersOnlyHere',
    });
    expect(form.get('newPassword')?.hasError('pattern')).toBeTrue();

    // valid password format
    form.patchValue({
      newPassword: 'ValidPass123',
      confirmPassword: 'ValidPass123',
    });
    expect(form.get('newPassword')?.valid).toBeTrue();
  });

  it('should flag error when new password and confirm password do not match', () => {
    const form = component.passwordForm;
    form.patchValue({
      currentPassword: 'CurrentPassword123!',
      newPassword: 'NewPassword123!',
      confirmPassword: 'DifferentPassword123!',
    });

    expect(form.hasError('passwordMismatch')).toBeTrue();
    expect(form.valid).toBeFalse();
  });

  it('should toggle password visibility flags', () => {
    expect(component.hideCurrent()).toBeTrue();
    component.toggleHideCurrent();
    expect(component.hideCurrent()).toBeFalse();

    expect(component.hideNew()).toBeTrue();
    component.toggleHideNew();
    expect(component.hideNew()).toBeFalse();

    expect(component.hideConfirm()).toBeTrue();
    component.toggleHideConfirm();
    expect(component.hideConfirm()).toBeFalse();
  });

  it('should submit change password successfully and reset the form', () => {
    authServiceSpy.changePassword.and.returnValue(
      of({ message: 'Password updated successfully' })
    );

    component.passwordForm.patchValue({
      currentPassword: 'OldPassword123',
      newPassword: 'NewPassword123',
      confirmPassword: 'NewPassword123',
    });

    component.onSubmit();

    expect(authServiceSpy.changePassword).toHaveBeenCalledWith({
      currentPassword: 'OldPassword123',
      newPassword: 'NewPassword123',
    });
    expect(component.submitting()).toBeFalse();
    expect(component.successMessage()).toContain('Password updated successfully');
    expect(snackBarSpy.open).toHaveBeenCalled();
  });

  it('should handle error when changing password fails', () => {
    authServiceSpy.changePassword.and.returnValue(
      throwError(() => ({
        status: 400,
        error: { message: 'Current password is incorrect' },
      }))
    );

    component.passwordForm.patchValue({
      currentPassword: 'WrongPassword123',
      newPassword: 'NewPassword123',
      confirmPassword: 'NewPassword123',
    });

    component.onSubmit();

    expect(authServiceSpy.changePassword).toHaveBeenCalled();
    expect(component.submitting()).toBeFalse();
    expect(component.errorMessage()).toBe('Current password is incorrect');
  });
});

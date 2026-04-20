import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VerifypasswordComponent } from './verifypassword.component';
import { UserService } from '../../../service/user.service';
import { StorageService } from '../../../service/storage.service';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { HotToastService } from '@ngxpert/hot-toast';
import { of, throwError } from 'rxjs';

describe('VerifypasswordComponent', () => {
  let component: VerifypasswordComponent;
  let fixture: ComponentFixture<VerifypasswordComponent>;
  let mockUserService: any;
  let mockToast: any;
  let mockActivatedRoute: any;

  beforeEach(async () => {
    mockUserService = {
      verifyPasswordToken$: jasmine.createSpy().and.returnValue(of({
        message: 'Token vérifié',
        data: { user: { userUuid: 'uuid-1', email: 'test@test.com' } }
      })),
      createNewPassword$: jasmine.createSpy().and.returnValue(of({ message: 'Mot de passe créé' })),
    };

    mockToast = {
      success: jasmine.createSpy('success'),
      error: jasmine.createSpy('error'),
    };

    mockActivatedRoute = {
      queryParamMap: of({ get: (_: string) => null }),
      snapshot: { params: {}, queryParams: {} }
    };

    await TestBed.configureTestingModule({
      imports: [VerifypasswordComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: mockUserService },
        { provide: StorageService, useValue: { getToken: () => null, getRedirectUrl: () => null } },
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
        { provide: HotToastService, useValue: mockToast },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(VerifypasswordComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should set error when no token', () => {
    mockActivatedRoute.queryParamMap = of({ get: (_: string) => null });
    component.ngOnInit();
    fixture.detectChanges();
    expect(component.state().error).toBeTruthy();
  });

  it('should verify token successfully', () => {
    mockActivatedRoute.queryParamMap = of({ get: (key: string) => key === 'token' ? 'valid-token' : null });

    component.ngOnInit();
    fixture.detectChanges();

    expect(mockUserService.verifyPasswordToken$).toHaveBeenCalledWith('valid-token');
    expect(mockToast.success).toHaveBeenCalled();
    expect(component.state().mode).toBe('reset');
  });

  it('should handle token verification error', () => {
    mockActivatedRoute.queryParamMap = of({ get: (key: string) => key === 'token' ? 'bad-token' : null });
    mockUserService.verifyPasswordToken$.and.returnValue(throwError(() => 'Token invalide'));

    component.ngOnInit();
    fixture.detectChanges();

    expect(mockToast.error).toHaveBeenCalledWith('Token invalide');
    expect(component.state().error).toBe('Token invalide');
  });

  it('should create new password successfully', () => {
    const mockForm: any = { value: { password: 'NewPass123!' } };
    mockUserService.createNewPassword$.and.returnValue(of({ message: 'Mot de passe mis à jour' }));

    component.createNewPassword(mockForm);

    expect(mockToast.success).toHaveBeenCalledWith('Mot de passe mis à jour');
    expect(component.state().success).toBeTrue();
  });

  it('should handle createNewPassword error', () => {
    const mockForm: any = { value: { password: 'weak' } };
    mockUserService.createNewPassword$.and.returnValue(throwError(() => 'Mot de passe trop faible'));

    component.createNewPassword(mockForm);

    expect(mockToast.error).toHaveBeenCalledWith('Mot de passe trop faible');
    expect(component.state().success).toBeFalse();
  });

  it('should toggle password visibility', () => {
    const input = document.createElement('input');
    input.type = 'password';
    component.togglePassword(input);
    expect(input.type).toBe('text');
    component.togglePassword(input);
    expect(input.type).toBe('password');
  });

  it('should close message', () => {
    component.state.update(s => ({ ...s, message: 'test', error: null }));
    component.closeMessage();
    expect(component.state().message).toBeUndefined();
  });
});
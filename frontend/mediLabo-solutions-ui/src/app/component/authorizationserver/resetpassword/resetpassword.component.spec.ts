import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ResetpassordComponent } from './resetpassword.component';
import { UserService } from '../../../service/user.service';
import { StorageService } from '../../../service/storage.service';
import { Router, provideRouter } from '@angular/router';
import { HotToastService } from '@ngxpert/hot-toast';
import { of, throwError } from 'rxjs';

describe('ResetpassordComponent', () => {
  let component: ResetpassordComponent;
  let fixture: ComponentFixture<ResetpassordComponent>;
  let mockUserService: any;
  let mockStorage: any;
  let mockToast: any;
  let router: Router;

  beforeEach(async () => {
    mockUserService = {
      isAuthenticated: jasmine.createSpy().and.returnValue(false),
      isTokenExpired: jasmine.createSpy().and.returnValue(true),
      resetPassword$: jasmine.createSpy().and.returnValue(of({ message: 'Email envoyé' })),
    };

    mockStorage = {
      getToken: jasmine.createSpy().and.returnValue(null),
      getRedirectUrl: jasmine.createSpy().and.returnValue(null),
      setRedirectUrl: jasmine.createSpy(),
      clear: jasmine.createSpy(),
    };

    mockToast = {
      success: jasmine.createSpy('success'),
      error: jasmine.createSpy('error'),
    };

    await TestBed.configureTestingModule({
      imports: [ResetpassordComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: mockUserService },
        { provide: StorageService, useValue: mockStorage },
        { provide: HotToastService, useValue: mockToast },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ResetpassordComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should redirect to dashboard if already authenticated', () => {
    mockUserService.isAuthenticated.and.returnValue(true);
    mockUserService.isTokenExpired.and.returnValue(false);
    mockStorage.getRedirectUrl.and.returnValue(null);
    spyOn(router, 'navigate');

    component.ngOnInit();

    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should redirect to redirectUrl if authenticated and redirectUrl exists', () => {
    mockUserService.isAuthenticated.and.returnValue(true);
    mockUserService.isTokenExpired.and.returnValue(false);
    mockStorage.getRedirectUrl.and.returnValue('/notes');
    spyOn(router, 'navigate');

    component.ngOnInit();

    expect(router.navigate).toHaveBeenCalledWith(['/notes']);
  });

  it('should call resetPassword and show success toast', () => {
    const mockForm: any = {
      value: { email: 'test@test.com' },
      reset: jasmine.createSpy('reset')
    };

    mockUserService.resetPassword$.and.returnValue(of({ message: 'Email envoyé avec succès' }));

    component.resetPassword(mockForm);

    expect(mockUserService.resetPassword$).toHaveBeenCalled();
    expect(mockToast.success).toHaveBeenCalledWith('Email envoyé avec succès');
    expect(component.state().message).toBe('Email envoyé avec succès');
    expect(component.state().loading).toBeFalse();
  });

  it('should handle resetPassword error', () => {
    const mockForm: any = {
      value: { email: 'test@test.com' },
      reset: jasmine.createSpy('reset')
    };

    mockUserService.resetPassword$.and.returnValue(throwError(() => 'Email introuvable'));

    component.resetPassword(mockForm);

    expect(mockToast.error).toHaveBeenCalledWith('Email introuvable');
    expect(component.state().error).toBe('Email introuvable');
    expect(component.state().loading).toBeFalse();
  });

  it('should close message', () => {
    component.state.set({ loading: false, message: 'test', error: null });
    component.closeMessage();
    expect(component.state().message).toBeUndefined();
    expect(component.state().error).toBeUndefined();
  });
});
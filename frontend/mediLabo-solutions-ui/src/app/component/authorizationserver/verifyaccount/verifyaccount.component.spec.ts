import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VerifyaccountComponent } from './verifyaccount.component';
import { UserService } from '../../../service/user.service';
import { StorageService } from '../../../service/storage.service';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { HotToastService } from '@ngxpert/hot-toast';
import { of, throwError } from 'rxjs';

describe('VerifyaccountComponent', () => {
  let component: VerifyaccountComponent;
  let fixture: ComponentFixture<VerifyaccountComponent>;
  let mockUserService: any;
  let mockToast: any;
  let mockActivatedRoute: any;

  beforeEach(async () => {
    mockUserService = {
      verifyAccountToken$: jasmine.createSpy().and.returnValue(of({ message: 'Compte vérifié' })),
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
      imports: [VerifyaccountComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: mockUserService },
        { provide: StorageService, useValue: { getToken: () => null, getRedirectUrl: () => null } },
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
        { provide: HotToastService, useValue: mockToast },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(VerifyaccountComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should set error state when no token in queryParams', () => {
    mockActivatedRoute.queryParamMap = of({ get: (_: string) => null });
    component.ngOnInit();
    fixture.detectChanges();
    expect(component.state().error).toBeTruthy();
  });

  it('should verify account token on success', () => {
    mockActivatedRoute.queryParamMap = of({ get: (key: string) => key === 'token' ? 'valid-token' : null });
    mockUserService.verifyAccountToken$.and.returnValue(of({ message: 'Compte vérifié avec succès' }));

    component.ngOnInit();
    fixture.detectChanges();

    expect(mockUserService.verifyAccountToken$).toHaveBeenCalledWith('valid-token');
    expect(mockToast.success).toHaveBeenCalled();
    expect(component.state().message).toBe('Compte vérifié avec succès');
    expect(component.state().loading).toBeFalse();
  });

  it('should handle token verification error', () => {
    mockActivatedRoute.queryParamMap = of({ get: (key: string) => key === 'token' ? 'invalid-token' : null });
    mockUserService.verifyAccountToken$.and.returnValue(throwError(() => 'Token expiré'));

    component.ngOnInit();
    fixture.detectChanges();

    expect(mockToast.error).toHaveBeenCalledWith('Token expiré');
    expect(component.state().error).toBe('Token expiré');
  });

  it('should close message', () => {
    component.state.set({ loading: false, message: 'test', error: null });
    component.closeMessage();
    expect(component.state().message).toBeUndefined();
    expect(component.state().error).toBeUndefined();
  });
});
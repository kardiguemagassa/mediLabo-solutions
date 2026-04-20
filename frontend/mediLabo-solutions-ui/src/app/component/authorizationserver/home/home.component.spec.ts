import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HomeComponent } from './home.component';
import { UserService } from '../../../service/user.service';
import { StorageService } from '../../../service/storage.service';
import { Router, provideRouter } from '@angular/router';
import { HotToastService } from '@ngxpert/hot-toast';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let mockUserService: any;
  let mockStorage: any;
  let mockToast: any;
  let mockActivatedRoute: any;
  let router: Router;

  beforeEach(async () => {
    mockUserService = {
      isAuthenticated: jasmine.createSpy('isAuthenticated').and.returnValue(false),
      isTokenExpired: jasmine.createSpy('isTokenExpired').and.returnValue(true),
      validateCode$: jasmine.createSpy('validateCode$').and.returnValue(of({
        access_token: 'fake-access-token',
        refresh_token: 'fake-refresh-token'
      })),
    };

    mockStorage = {
      getToken: jasmine.createSpy('getToken').and.returnValue(null),
      getRedirectUrl: jasmine.createSpy('getRedirectUrl').and.returnValue(null),
      setRedirectUrl: jasmine.createSpy('setRedirectUrl'),
      set: jasmine.createSpy('set'),
      clear: jasmine.createSpy('clear'),
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
      imports: [HomeComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: mockUserService },
        { provide: StorageService, useValue: mockStorage },
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
        { provide: HotToastService, useValue: mockToast },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
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
    mockStorage.getRedirectUrl.and.returnValue('/patients');
    spyOn(router, 'navigate');

    component.ngOnInit();

    expect(router.navigate).toHaveBeenCalledWith(['/patients']);
  });

  it('should set loading to false when no code in queryParams', () => {
    mockActivatedRoute.queryParamMap = of({ get: (_: string) => null });

    component.ngOnInit();
    fixture.detectChanges();

    expect(component.loading()).toBeFalse();
  });

  it('should validate code and navigate to dashboard on success', () => {
    mockActivatedRoute.queryParamMap = of({ get: (key: string) => key === 'code' ? 'auth-code-123' : null });
    mockUserService.validateCode$.and.returnValue(of({
      access_token: 'token123',
      refresh_token: 'refresh123'
    }));
    mockStorage.getRedirectUrl.and.returnValue(null);
    spyOn(router, 'navigate');

    component.ngOnInit();
    fixture.detectChanges();

    expect(mockUserService.validateCode$).toHaveBeenCalled();
    expect(mockStorage.set).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should validate code and navigate to redirectUrl on success', () => {
    mockActivatedRoute.queryParamMap = of({ get: (key: string) => key === 'code' ? 'auth-code-123' : null });
    mockUserService.validateCode$.and.returnValue(of({
      access_token: 'token123',
      refresh_token: 'refresh123'
    }));
    mockStorage.getRedirectUrl.and.returnValue('/notes');
    spyOn(router, 'navigate');

    component.ngOnInit();
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/notes']);
  });

  it('should show toast error when validateCode fails', () => {
    mockActivatedRoute.queryParamMap = of({ get: (key: string) => key === 'code' ? 'bad-code' : null });
    mockUserService.validateCode$.and.returnValue(throwError(() => 'Code invalide'));

    component.ngOnInit();
    fixture.detectChanges();

    expect(mockToast.error).toHaveBeenCalledWith('Code invalide');
    expect(component.loading()).toBeFalse();
  });

  it('should have login function defined', () => {
    expect(component.login).toBeDefined();
    expect(typeof component.login).toBe('function');
  });
});
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthorizationserverComponent } from './authorizationserver.component';
import { provideRouter } from '@angular/router';
import { UserService } from '../../service/user.service';
import { StorageService } from '../../service/storage.service';

describe('AuthorizationserverComponent', () => {
  let component: AuthorizationserverComponent;
  let fixture: ComponentFixture<AuthorizationserverComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthorizationserverComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: { isAuthenticated: () => false, isTokenExpired: () => true, getToken: () => null, connectedUser: () => null, logout: jasmine.createSpy() } },
        { provide: StorageService, useValue: { getToken: () => null, setToken: jasmine.createSpy(), clear: jasmine.createSpy(), getRedirectUrl: () => null } },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuthorizationserverComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { TestBed } from '@angular/core/testing';
import { UserService } from './user.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { StorageService } from './storage.service';
import { Router } from '@angular/router';

describe('UserService', () => {
  let service: UserService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        UserService,
        { provide: StorageService, useValue: { getToken: () => null, setToken: jasmine.createSpy(), clear: jasmine.createSpy(), getRedirectUrl: () => null, setRedirectUrl: jasmine.createSpy() } },
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate') } },
      ]
    });
    service = TestBed.inject(UserService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

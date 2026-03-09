import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuthorizationserverComponent } from './authorizationserver.component';

describe('AuthorizationserverComponent', () => {
  let component: AuthorizationserverComponent;
  let fixture: ComponentFixture<AuthorizationserverComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthorizationserverComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AuthorizationserverComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

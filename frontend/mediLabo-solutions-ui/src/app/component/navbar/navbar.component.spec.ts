import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NavbarComponent } from './navbar.component';
import { UserService } from '../../service/user.service';
import { StorageService } from '../../service/storage.service';
import { PatientService } from '../../service/patient.service';
import { NoteService } from '../../service/note.service';
import { AssessmentService } from '../../service/assessment.service';
import { NotificationService } from '../../service/notification.service';
import { PermissionService } from '../../service/permission.service';
import { HotToastService } from '@ngxpert/hot-toast';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

describe('NavbarComponent', () => {
  let component: NavbarComponent;
  let fixture: ComponentFixture<NavbarComponent>;

  const mockStore: any = {
    getProfile: jasmine.createSpy('getProfile'),
    getMessages: jasmine.createSpy('getMessages'),
    getAllPatients: jasmine.createSpy('getAllPatients'),
    profile: () => ({ firstName: 'Test', lastName: 'User', email: 'test@test.com' }),
    messages: () => [],
    unreadMessageCount: () => 0,
  };

  const mockPermissionService: any = {
    canViewPatients: jasmine.createSpy('canViewPatients').and.returnValue(true),
    canViewUsers: jasmine.createSpy('canViewUsers').and.returnValue(true),
    isStaff: jasmine.createSpy('isStaff').and.returnValue(true),
  };

  const mockUserService: any = {
    isAuthenticated: () => true,
    isTokenExpired: () => false,
    getToken: () => 'tok',
    getUserRole: () => 'ADMIN',
    connectedUser: () => ({ id: 1 }),
    logOut: jasmine.createSpy('logOut'),
    usersPageable$: () => of({}), users$: () => of({}), user$: () => of({}),
    profile$: () => of({}), update$: () => of({}), updatePassword$: () => of({}),
    updateImage$: () => of({}), updateRole$: () => of({}), updateRoleByUuid$: () => of({}),
    toggleAccountLocked$: () => of({}), toggleAccountExpired$: () => of({}),
    toggleAccountEnabled$: () => of({}), toggleAccountLockedByUuid$: () => of({}),
    toggleAccountExpiredByUuid$: () => of({}), toggleAccountEnabledByUuid$: () => of({}),
    enableMfa$: () => of({}), disableMfa$: () => of({}),
    enableMfaByUuid$: () => of({}), disableMfaByUuid$: () => of({}),
  };

  const mockStorage: any = {
    getToken: () => 'tok',
    setToken: jasmine.createSpy(),
    clear: jasmine.createSpy(),
    getRedirectUrl: () => null,
    setRedirectUrl: jasmine.createSpy(),
    removeRedirectUrl: jasmine.createSpy(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: mockUserService },
        { provide: StorageService, useValue: mockStorage },
        { provide: PatientService, useValue: { allPatientsPageable$: () => of({}), allPatients$: () => of({}), patient$: () => of({}), createPatient$: () => of({}), updatePatient$: () => of({}), deletePatient$: () => of({}), restorePatient$: () => of({}), patientByUserUuid$: () => of({}), patientByEmail$: () => of({}), myPatientRecord$: () => of({}) } },
        { provide: NoteService, useValue: { allNotesPageable$: () => of({}), allNotes$: () => of({}), note$: () => of({}), notesByPatient$: () => of({}), createNote$: () => of({}), updateNote$: () => of({}), deleteNote$: () => of({}), myMedicalNotes$: () => of({}), addComment$: () => of({}), updateComment$: () => of({}), deleteComment$: () => of({}), uploadFile$: () => of({}), deleteFile$: () => of({}) } },
        { provide: AssessmentService, useValue: { allAssessments$: () => of({}), assessPatient$: () => of({}) } },
        { provide: NotificationService, useValue: { messages$: () => of({}), sendMessage$: () => of({}), conversation$: () => of({}), replyToMessage$: () => of({}) } },
        { provide: PermissionService, useValue: mockPermissionService },
        { provide: HotToastService, useValue: { success: jasmine.createSpy(), error: jasmine.createSpy() } },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
    (component as any).store = mockStore;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call store on ngOnInit', () => {
    component.ngOnInit();
    expect(mockStore.getProfile).toHaveBeenCalled();
    expect(mockStore.getMessages).toHaveBeenCalled();
  });

  it('should load patients if has permission', () => {
    mockPermissionService.canViewPatients.and.returnValue(true);
    component.ngOnInit();
    expect(mockStore.getAllPatients).toHaveBeenCalled();
  });

  it('should toggle menu', () => {
    expect(component.isMenuOpen()).toBeFalse();
    component.toggleMenu();
    expect(component.isMenuOpen()).toBeTrue();
    component.toggleMenu();
    expect(component.isMenuOpen()).toBeFalse();
  });

  it('should toggle nav', () => {
    expect(component.isNavOpen()).toBeFalse();
    component.toggleNav();
    expect(component.isNavOpen()).toBeTrue();
  });

  it('should close menu on click outside .usermenu', () => {
    component.isMenuOpen.set(true);
    const event = new MouseEvent('click');
    Object.defineProperty(event, 'target', { value: document.createElement('div') });
    component.onClick(event);
    expect(component.isMenuOpen()).toBeFalse();
  });
});
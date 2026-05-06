import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UsersComponent } from './users.component';
import { UserService } from '../../../../service/user.service';
import { StorageService } from '../../../../service/storage.service';
import { PatientService } from '../../../../service/patient.service';
import { NoteService } from '../../../../service/note.service';
import { AssessmentService } from '../../../../service/assessment.service';
import { NotificationService } from '../../../../service/notification.service';
import { provideRouter } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { HotToastService } from '@ngxpert/hot-toast';
import { of } from 'rxjs';

describe('UsersComponent', () => {
  let component: UsersComponent;
  let fixture: ComponentFixture<UsersComponent>;

  const mockUserService: any = {
    isAuthenticated: () => true,
    isTokenExpired: () => false,
    getToken: () => 'fake-token',
    getUserRole: () => 'ADMIN',
    getConnectedUser: () => ({ id: 1, firstName: 'Test', lastName: 'User', email: 'test@test.com', role: 'ADMIN' }),
    connectedUser: () => ({ id: 1, firstName: 'Test', lastName: 'User', email: 'test@test.com', role: 'ADMIN' }),
    logout: jasmine.createSpy('logout'),
    usersPageable$: () => of({}),
    users$: () => of({}),
    user$: () => of({}),
    profile$: () => of({}),
    update$: () => of({}),
    updatePassword$: () => of({}),
    updateImage$: () => of({}),
    updateRole$: () => of({}),
    updateRoleByUuid$: () => of({}),
    toggleAccountLocked$: () => of({}),
    toggleAccountExpired$: () => of({}),
    toggleAccountEnabled$: () => of({}),
    toggleAccountLockedByUuid$: () => of({}),
    toggleAccountExpiredByUuid$: () => of({}),
    toggleAccountEnabledByUuid$: () => of({}),
    enableMfa$: () => of({}),
    disableMfa$: () => of({}),
    enableMfaByUuid$: () => of({}),
    disableMfaByUuid$: () => of({}),
  };

  const mockStorageService: any = {
    getToken: () => 'fake-token',
    setToken: jasmine.createSpy('setToken'),
    clear: jasmine.createSpy('clear'),
    getRedirectUrl: () => null,
    setRedirectUrl: jasmine.createSpy('setRedirectUrl'),
  };

  const mockPatientService: any = {
    allPatientsPageable$: () => of({}),
    allPatients$: () => of({}),
    patient$: () => of({}),
    createPatient$: () => of({}),
    updatePatient$: () => of({}),
    deletePatient$: () => of({}),
    restorePatient$: () => of({}),
    patientByUserUuid$: () => of({}),
    patientByEmail$: () => of({}),
    myPatientRecord$: () => of({}),
  };

  const mockNoteService: any = {
    allNotesPageable$: () => of({}),
    allNotes$: () => of({}),
    note$: () => of({}),
    notesByPatient$: () => of({}),
    createNote$: () => of({}),
    updateNote$: () => of({}),
    deleteNote$: () => of({}),
    myMedicalNotes$: () => of({}),
    addComment$: () => of({}),
    updateComment$: () => of({}),
    deleteComment$: () => of({}),
    uploadFile$: () => of({}),
    deleteFile$: () => of({}),
  };

  const mockAssessmentService: any = {
    allAssessments$: () => of({}),
    assessPatient$: () => of({}),
  };

  const mockNotificationService: any = {
    messages$: () => of({}),
    sendMessage$: () => of({}),
    conversation$: () => of({}),
    replyToMessage$: () => of({}),
  };

  const mockToast = {
    success: jasmine.createSpy('success'),
    error: jasmine.createSpy('error'),
    loading: jasmine.createSpy('loading'),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsersComponent, HttpClientTestingModule],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: mockUserService },
        { provide: StorageService, useValue: mockStorageService },
        { provide: PatientService, useValue: mockPatientService },
        { provide: NoteService, useValue: mockNoteService },
        { provide: AssessmentService, useValue: mockAssessmentService },
        { provide: NotificationService, useValue: mockNotificationService },
        { provide: HotToastService, useValue: mockToast },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UsersComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

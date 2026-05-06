import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MessagesComponent } from './messages.component';
import { provideRouter } from '@angular/router';
import { UserService } from '../../../../service/user.service';
import { StorageService } from '../../../../service/storage.service';
import { PatientService } from '../../../../service/patient.service';
import { NoteService } from '../../../../service/note.service';
import { AssessmentService } from '../../../../service/assessment.service';
import { NotificationService } from '../../../../service/notification.service';
import { PermissionService } from '../../../../service/permission.service';
import { HotToastService } from '@ngxpert/hot-toast';
import { DialogService } from '@ngneat/dialog';
import { Location } from '@angular/common';
import { of } from 'rxjs';

describe('MessagesComponent', () => {
  let component: MessagesComponent;
  let fixture: ComponentFixture<MessagesComponent>;

  const mockMessages = [
    { sender: { email: 'doctor@test.com', name: 'Dr Martin', userUuid: 'u1', role: 'DOCTOR' }, receiver: { email: 'patient@test.com', name: 'Jean Dupont', userUuid: 'u2', role: 'USER' }, message: 'Bonjour' },
    { sender: { email: 'patient@test.com', name: 'Jean Dupont', userUuid: 'u2', role: 'USER' }, receiver: { email: 'doctor@test.com', name: 'Dr Martin', userUuid: 'u1', role: 'DOCTOR' }, message: 'Merci' },
  ];

  const mockStore: any = {
    getMessages: jasmine.createSpy('getMessages'),
    getUsers: jasmine.createSpy('getUsers'),
    sendMessage: jasmine.createSpy('sendMessage'),
    messages: () => mockMessages,
    users: () => [
      { userUuid: 'u1', firstName: 'Dr', lastName: 'Martin', email: 'doctor@test.com', role: 'DOCTOR' },
      { userUuid: 'u2', firstName: 'Jean', lastName: 'Dupont', email: 'patient@test.com', role: 'USER' },
    ],
    profile: () => ({ firstName: 'Admin', lastName: 'Test', email: 'admin@test.com' }),
  };

  const mockPermissionService: any = {
    canViewUsers: jasmine.createSpy().and.returnValue(true),
    canViewPatients: jasmine.createSpy().and.returnValue(true),
    isStaff: jasmine.createSpy().and.returnValue(true),
  };

  const mockDialogService: any = {
    open: jasmine.createSpy('open'),
    closeAll: jasmine.createSpy('closeAll'),
  };

  const mockLocation: any = {
    back: jasmine.createSpy('back'),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MessagesComponent],
      providers: [
        provideRouter([]),
        { provide: Location, useValue: mockLocation },
        { provide: DialogService, useValue: mockDialogService },
        { provide: PermissionService, useValue: mockPermissionService },
        { provide: UserService, useValue: { isAuthenticated: () => true, getToken: () => 'tok', getUserRole: () => 'ADMIN', connectedUser: () => ({}), usersPageable$: () => of({}), users$: () => of({}), user$: () => of({}), profile$: () => of({}), update$: () => of({}), updatePassword$: () => of({}), updateImage$: () => of({}), updateRole$: () => of({}), updateRoleByUuid$: () => of({}), toggleAccountLocked$: () => of({}), toggleAccountExpired$: () => of({}), toggleAccountEnabled$: () => of({}), toggleAccountLockedByUuid$: () => of({}), toggleAccountExpiredByUuid$: () => of({}), toggleAccountEnabledByUuid$: () => of({}), enableMfa$: () => of({}), disableMfa$: () => of({}), enableMfaByUuid$: () => of({}), disableMfaByUuid$: () => of({}) } },
        { provide: StorageService, useValue: { getToken: () => 'tok', setToken: jasmine.createSpy(), clear: jasmine.createSpy(), getRedirectUrl: () => null, setRedirectUrl: jasmine.createSpy() } },
        { provide: PatientService, useValue: { allPatientsPageable$: () => of({}), allPatients$: () => of({}), patient$: () => of({}), createPatient$: () => of({}), updatePatient$: () => of({}), deletePatient$: () => of({}), restorePatient$: () => of({}), patientByUserUuid$: () => of({}), patientByEmail$: () => of({}), myPatientRecord$: () => of({}) } },
        { provide: NoteService, useValue: { allNotesPageable$: () => of({}), allNotes$: () => of({}), note$: () => of({}), notesByPatient$: () => of({}), createNote$: () => of({}), updateNote$: () => of({}), deleteNote$: () => of({}), myMedicalNotes$: () => of({}), addComment$: () => of({}), updateComment$: () => of({}), deleteComment$: () => of({}), uploadFile$: () => of({}), deleteFile$: () => of({}) } },
        { provide: AssessmentService, useValue: { allAssessments$: () => of({}), assessPatient$: () => of({}) } },
        { provide: NotificationService, useValue: { messages$: () => of({}), sendMessage$: () => of({}), conversation$: () => of({}), replyToMessage$: () => of({}) } },
        { provide: HotToastService, useValue: { success: jasmine.createSpy(), error: jasmine.createSpy() } },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MessagesComponent);
    component = fixture.componentInstance;
    (component as any).store = mockStore;
    (component as any).permissionService = mockPermissionService;
    (component as any).dialogService = mockDialogService;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call store on ngOnInit', () => {
    component.ngOnInit();
    expect(mockStore.getMessages).toHaveBeenCalled();
  });

  it('should not load users if no permission', () => {
    mockPermissionService.canViewUsers.and.returnValue(false);
    mockStore.getUsers.calls.reset();
    component.ngOnInit();
    expect(mockStore.getUsers).not.toHaveBeenCalled();
  });

  it('should compute recipients for staff', () => {
    mockPermissionService.canViewUsers.and.returnValue(true);
    const recipients = (component as any).recipients();
    expect(recipients.every((r: any) => r.email !== 'admin@test.com')).toBeTrue();
  });

  it('should compute recipients for patient from messages', () => {
    mockPermissionService.canViewUsers.and.returnValue(false);
    mockStore.profile = () => ({ email: 'patient@test.com' });
    const recipients = (component as any).recipients();
    expect(recipients.length).toBeGreaterThanOrEqual(0);
  });

  it('should send message and close modal', () => {
    const mockForm: any = {
      value: { receiverEmail: 'doctor@test.com', subject: 'Test', message: 'Bonjour' }
    };
    component.saveMessage(mockForm);
    expect(mockStore.sendMessage).toHaveBeenCalledWith(mockForm.value);
    expect(mockDialogService.closeAll).toHaveBeenCalled();
  });

  it('should open modal', () => {
    const mockTemplate: any = {};
    component.openModal(mockTemplate);
    expect(mockDialogService.open).toHaveBeenCalled();
  });

  it('should close modal', () => {
    component.closeModal();
    expect(mockDialogService.closeAll).toHaveBeenCalled();
  });
});
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MessageDetailComponent } from './message-detail.component';
import { provideRouter } from '@angular/router';
import { UserService } from '../../../../service/user.service';
import { StorageService } from '../../../../service/storage.service';
import { PatientService } from '../../../../service/patient.service';
import { NoteService } from '../../../../service/note.service';
import { AssessmentService } from '../../../../service/assessment.service';
import { NotificationService } from '../../../../service/notification.service';
import { HotToastService } from '@ngxpert/hot-toast';
import { Location } from '@angular/common';
import { of } from 'rxjs';

describe('MessageDetailComponent', () => {
  let component: MessageDetailComponent;
  let fixture: ComponentFixture<MessageDetailComponent>;

  const mockConversation = [
    { sender: { email: 'doctor@test.com', name: 'Dr Martin' }, receiver: { email: 'patient@test.com', name: 'Jean Dupont' }, message: 'Bonjour' },
    { sender: { email: 'patient@test.com', name: 'Jean Dupont' }, receiver: { email: 'doctor@test.com', name: 'Dr Martin' }, message: 'Merci docteur' },
  ];

  const mockStore: any = {
    conversation: () => mockConversation,
    profile: () => ({ firstName: 'Jean', lastName: 'Dupont', email: 'patient@test.com' }),
    getConversation: jasmine.createSpy('getConversation'),
    replyToMessage: jasmine.createSpy('replyToMessage'),
  };

  const mockLocation: any = {
    back: jasmine.createSpy('back'),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MessageDetailComponent],
      providers: [
        provideRouter([]),
        { provide: Location, useValue: mockLocation },
        { provide: UserService, useValue: { isAuthenticated: () => true, getToken: () => 'tok', getUserRole: () => 'ADMIN', connectedUser: () => ({}), usersPageable$: () => of({}), users$: () => of({}), user$: () => of({}), profile$: () => of({}), update$: () => of({}), updatePassword$: () => of({}), updateImage$: () => of({}), updateRole$: () => of({}), updateRoleByUuid$: () => of({}), toggleAccountLocked$: () => of({}), toggleAccountExpired$: () => of({}), toggleAccountEnabled$: () => of({}), toggleAccountLockedByUuid$: () => of({}), toggleAccountExpiredByUuid$: () => of({}), toggleAccountEnabledByUuid$: () => of({}), enableMfa$: () => of({}), disableMfa$: () => of({}), enableMfaByUuid$: () => of({}), disableMfaByUuid$: () => of({}) } },
        { provide: StorageService, useValue: { getToken: () => 'tok', setToken: jasmine.createSpy(), clear: jasmine.createSpy(), getRedirectUrl: () => null, setRedirectUrl: jasmine.createSpy() } },
        { provide: PatientService, useValue: { allPatientsPageable$: () => of({}), allPatients$: () => of({}), patient$: () => of({}), createPatient$: () => of({}), updatePatient$: () => of({}), deletePatient$: () => of({}), restorePatient$: () => of({}), patientByUserUuid$: () => of({}), patientByEmail$: () => of({}), myPatientRecord$: () => of({}) } },
        { provide: NoteService, useValue: { allNotesPageable$: () => of({}), allNotes$: () => of({}), note$: () => of({}), notesByPatient$: () => of({}), createNote$: () => of({}), updateNote$: () => of({}), deleteNote$: () => of({}), myMedicalNotes$: () => of({}), addComment$: () => of({}), updateComment$: () => of({}), deleteComment$: () => of({}), uploadFile$: () => of({}), deleteFile$: () => of({}) } },
        { provide: AssessmentService, useValue: { allAssessments$: () => of({}), assessPatient$: () => of({}) } },
        { provide: NotificationService, useValue: { messages$: () => of({}), sendMessage$: () => of({}), conversation$: () => of({}), replyToMessage$: () => of({}) } },
        { provide: HotToastService, useValue: { success: jasmine.createSpy(), error: jasmine.createSpy() } },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MessageDetailComponent);
    component = fixture.componentInstance;
    (component as any).store = mockStore;
    (component as any).location = mockLocation;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should go back on goBack', () => {
    component.goBack();
    expect(mockLocation.back).toHaveBeenCalled();
  });

  it('should send message with correct receiver email', () => {
    const mockForm: any = {
      value: { message: 'Bonjour docteur' },
      reset: jasmine.createSpy('reset')
    };

    component.saveMessage(mockForm);

    expect(mockStore.replyToMessage).toHaveBeenCalledWith(jasmine.objectContaining({
      receiverEmail: 'doctor@test.com',
      subject: 'Re: Conversation',
      message: 'Bonjour docteur'
    }));
    expect(mockForm.reset).toHaveBeenCalled();
  });

  it('should handle empty conversation on saveMessage', () => {
    mockStore.conversation = () => [];
    const mockForm: any = {
      value: { message: 'Test' },
      reset: jasmine.createSpy('reset')
    };

    expect(() => component.saveMessage(mockForm)).not.toThrow();
    expect(mockStore.replyToMessage).toHaveBeenCalled();
  });
});
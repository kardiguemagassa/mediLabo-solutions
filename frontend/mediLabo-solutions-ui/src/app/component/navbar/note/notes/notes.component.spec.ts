import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NotesComponent } from './notes.component';
import { provideRouter } from '@angular/router';
import { UserService } from '../../../../service/user.service';
import { StorageService } from '../../../../service/storage.service';
import { PatientService } from '../../../../service/patient.service';
import { NoteService } from '../../../../service/note.service';
import { AssessmentService } from '../../../../service/assessment.service';
import { NotificationService } from '../../../../service/notification.service';
import { HotToastService } from '@ngxpert/hot-toast';
import { of } from 'rxjs';

describe('NotesComponent', () => {
  let component: NotesComponent;
  let fixture: ComponentFixture<NotesComponent>;

  const mockNotes = [
    { noteUuid: '1', patientUuid: 'p1', content: 'Note sur diabète', practitionerName: 'Dr Martin', createdAt: new Date().toISOString(), fileCount: 1, commentCount: 2 },
    { noteUuid: '2', patientUuid: 'p2', content: 'Suivi tension', practitionerName: 'Dr Dupont', createdAt: new Date().toISOString(), fileCount: 0, commentCount: 0 },
  ];

  const mockStore: any = {
    getAllNotesPageable: jasmine.createSpy('getAllNotesPageable'),
    getAllNotes: jasmine.createSpy('getAllNotes'),
    getAllPatients: jasmine.createSpy('getAllPatients'),
    createNote: jasmine.createSpy('createNote'),
    deleteNote: jasmine.createSpy('deleteNote'),
    setCurrentPage: jasmine.createSpy('setCurrentPage'),
    notePage: () => ({ content: mockNotes, currentPage: 0, totalPages: 3, totalElements: 25, size: 10 }),
    allNotes: () => mockNotes,
    allPatients: () => [
      { patientUuid: 'p1', userInfo: { firstName: 'Jean', lastName: 'Dupont', imageUrl: null } },
      { patientUuid: 'p2', userInfo: { firstName: 'Marie', lastName: 'Martin', imageUrl: null } },
    ],
    query: () => ({ page: 0, size: 10, sortBy: 'createdAt', direction: 'desc', status: '', type: '', filter: '' }),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotesComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: { isAuthenticated: () => true, getToken: () => 'tok', getUserRole: () => 'ADMIN', connectedUser: () => ({}), usersPageable$: () => of({}), users$: () => of({}), user$: () => of({}), profile$: () => of({}), update$: () => of({}), updatePassword$: () => of({}), updateImage$: () => of({}), updateRole$: () => of({}), updateRoleByUuid$: () => of({}), toggleAccountLocked$: () => of({}), toggleAccountExpired$: () => of({}), toggleAccountEnabled$: () => of({}), toggleAccountLockedByUuid$: () => of({}), toggleAccountExpiredByUuid$: () => of({}), toggleAccountEnabledByUuid$: () => of({}), enableMfa$: () => of({}), disableMfa$: () => of({}), enableMfaByUuid$: () => of({}), disableMfaByUuid$: () => of({}) } },
        { provide: StorageService, useValue: { getToken: () => 'tok', setToken: jasmine.createSpy(), clear: jasmine.createSpy(), getRedirectUrl: () => null, setRedirectUrl: jasmine.createSpy() } },
        { provide: PatientService, useValue: { allPatientsPageable$: () => of({}), allPatients$: () => of({}), patient$: () => of({}), createPatient$: () => of({}), updatePatient$: () => of({}), deletePatient$: () => of({}), restorePatient$: () => of({}), patientByUserUuid$: () => of({}), patientByEmail$: () => of({}), myPatientRecord$: () => of({}) } },
        { provide: NoteService, useValue: { allNotesPageable$: () => of({}), allNotes$: () => of({}), note$: () => of({}), notesByPatient$: () => of({}), createNote$: () => of({}), updateNote$: () => of({}), deleteNote$: () => of({}), myMedicalNotes$: () => of({}), addComment$: () => of({}), updateComment$: () => of({}), deleteComment$: () => of({}), uploadFile$: () => of({}), deleteFile$: () => of({}) } },
        { provide: AssessmentService, useValue: { allAssessments$: () => of({}), assessPatient$: () => of({}) } },
        { provide: NotificationService, useValue: { messages$: () => of({}), sendMessage$: () => of({}), conversation$: () => of({}), replyToMessage$: () => of({}) } },
        { provide: HotToastService, useValue: { success: jasmine.createSpy(), error: jasmine.createSpy() } },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NotesComponent);
    component = fixture.componentInstance;
    (component as any).store = mockStore;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call store on ngOnInit', () => {
    component.ngOnInit();
    expect(mockStore.getAllNotesPageable).toHaveBeenCalled();
    expect(mockStore.getAllNotes).toHaveBeenCalled();
    expect(mockStore.getAllPatients).toHaveBeenCalled();
  });

  it('should filter notes by search query', () => {
    component.searchQuery.set('diabète');
    const filtered = component.filteredNotes();
    expect(filtered.some((n: any) => n.content.toLowerCase().includes('diabète'))).toBeTrue();
  });

  it('should return all notes when no search query', () => {
    component.searchQuery.set('');
    expect(component.filteredNotes().length).toBe(2);
  });

  it('should toggle create form', () => {
    expect(component.showCreateForm()).toBeFalse();
    component.toggleCreateForm();
    expect(component.showCreateForm()).toBeTrue();
    component.toggleCreateForm();
    expect(component.showCreateForm()).toBeFalse();
  });

  it('should set selectedPatientUuid on patient selected', () => {
    const event = { target: { value: 'p1' } } as any;
    component.onPatientSelected(event);
    expect(component.selectedPatientUuid()).toBe('p1');
  });

  it('should not create note when patientUuid or content empty', () => {
    mockStore.createNote.calls.reset();
    component.selectedPatientUuid.set('');
    component.noteContent.set('Contenu');
    component.createNote();
    expect(mockStore.createNote).not.toHaveBeenCalled();
  });

  it('should go to page', () => {
    component.goToPage(2);
    expect(mockStore.getAllNotesPageable).toHaveBeenCalled();
  });

  it('should go to previous page when not first', () => {
    mockStore.notePage = () => ({ content: mockNotes, currentPage: 2, totalPages: 3, totalElements: 25, size: 10 });
    component.previousPage();
    expect(mockStore.getAllNotesPageable).toHaveBeenCalled();
  });

  it('should not go to previous page when on first page', () => {
    mockStore.getAllNotesPageable.calls.reset();
    mockStore.notePage = () => ({ content: mockNotes, currentPage: 0, totalPages: 3, totalElements: 25, size: 10 });
    component.previousPage();
    expect(mockStore.getAllNotesPageable).not.toHaveBeenCalled();
  });

  it('should go to next page', () => {
    mockStore.notePage = () => ({ content: mockNotes, currentPage: 0, totalPages: 3, totalElements: 25, size: 10 });
    component.nextPage();
    expect(mockStore.getAllNotesPageable).toHaveBeenCalled();
  });

  it('should update search query on search', () => {
    const event = { target: { value: 'tension' } } as any;
    component.onSearch(event);
    expect(component.searchQuery()).toBe('tension');
  });

  it('should clear search', () => {
    component.searchQuery.set('test');
    component.clearSearch();
    expect(component.searchQuery()).toBe('');
  });

  it('should change page size', () => {
    const event = { target: { value: '5' } } as any;
    component.changePageSize(event);
    expect(component.pageSize()).toBe(5);
    expect(mockStore.getAllNotesPageable).toHaveBeenCalled();
  });

  it('should truncate long text', () => {
    const longText = 'a'.repeat(100);
    expect(component.truncate(longText, 80)).toContain('...');
    expect(component.truncate(longText, 80).length).toBe(83);
  });

  it('should return empty string for null text in truncate', () => {
    expect(component.truncate(null as any)).toBe('');
  });

  it('should compute stats correctly', () => {
    expect(component.totalNotes()).toBe(2);
    expect(component.notesWithFiles()).toBe(1);
    expect(component.notesWithComments()).toBe(1);
  });
});
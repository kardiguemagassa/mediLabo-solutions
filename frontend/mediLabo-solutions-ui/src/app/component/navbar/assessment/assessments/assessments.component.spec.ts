import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AssessmentsComponent } from './assessments.component';
import { provideRouter } from '@angular/router';
import { UserService } from '../../../../service/user.service';
import { StorageService } from '../../../../service/storage.service';
import { PatientService } from '../../../../service/patient.service';
import { NoteService } from '../../../../service/note.service';
import { AssessmentService } from '../../../../service/assessment.service';
import { NotificationService } from '../../../../service/notification.service';
import { HotToastService } from '@ngxpert/hot-toast';
import { of } from 'rxjs';

describe('AssessmentsComponent', () => {
  let component: AssessmentsComponent;
  let fixture: ComponentFixture<AssessmentsComponent>;

  const mockAssessments = [
    { patientUuid: '1', patientName: 'Jean Dupont', riskLevel: 'BORDERLINE', gender: 'MALE', triggerCount: 3, triggersFound: ['tabac'] },
    { patientUuid: '2', patientName: 'Marie Martin', riskLevel: 'IN_DANGER', gender: 'FEMALE', triggerCount: 6, triggersFound: ['alcool'] },
    { patientUuid: '3', patientName: 'Paul Durand', riskLevel: 'NONE', gender: 'MALE', triggerCount: 0, triggersFound: [] },
    { patientUuid: '4', patientName: 'Lisa Bernard', riskLevel: 'EARLY_ONSET', gender: 'FEMALE', triggerCount: 8, triggersFound: ['tabac', 'alcool'] },
  ];

  const mockPatients = [
    { patientUuid: '1', userInfo: { firstName: 'Jean', lastName: 'Dupont', imageUrl: 'img1.jpg' } },
    { patientUuid: '2', userInfo: { firstName: 'Marie', lastName: 'Martin', imageUrl: null } },
  ];

  const mockStore: any = {
    getAllAssessments: jasmine.createSpy('getAllAssessments'),
    getAllPatients: jasmine.createSpy('getAllPatients'),
    assessPatient: jasmine.createSpy('assessPatient'),
    setCurrentPage: jasmine.createSpy('setCurrentPage'),
    allAssessments: () => mockAssessments,
    allPatients: () => mockPatients,
    currentPage: () => 0,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssessmentsComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: { isAuthenticated: () => true, getToken: () => 'tok', getUserRole: () => 'ADMIN', connectedUser: () => ({ id: 1 }), usersPageable$: () => of({}), users$: () => of({}), user$: () => of({}), profile$: () => of({}), update$: () => of({}), updatePassword$: () => of({}), updateImage$: () => of({}), updateRole$: () => of({}), updateRoleByUuid$: () => of({}), toggleAccountLocked$: () => of({}), toggleAccountExpired$: () => of({}), toggleAccountEnabled$: () => of({}), toggleAccountLockedByUuid$: () => of({}), toggleAccountExpiredByUuid$: () => of({}), toggleAccountEnabledByUuid$: () => of({}), enableMfa$: () => of({}), disableMfa$: () => of({}), enableMfaByUuid$: () => of({}), disableMfaByUuid$: () => of({}) } },
        { provide: StorageService, useValue: { getToken: () => 'tok', setToken: jasmine.createSpy(), clear: jasmine.createSpy(), getRedirectUrl: () => null, setRedirectUrl: jasmine.createSpy() } },
        { provide: PatientService, useValue: { allPatientsPageable$: () => of({}), allPatients$: () => of({}), patient$: () => of({}), createPatient$: () => of({}), updatePatient$: () => of({}), deletePatient$: () => of({}), restorePatient$: () => of({}), patientByUserUuid$: () => of({}), patientByEmail$: () => of({}), myPatientRecord$: () => of({}) } },
        { provide: NoteService, useValue: { allNotesPageable$: () => of({}), allNotes$: () => of({}), note$: () => of({}), notesByPatient$: () => of({}), createNote$: () => of({}), updateNote$: () => of({}), deleteNote$: () => of({}), myMedicalNotes$: () => of({}), addComment$: () => of({}), updateComment$: () => of({}), deleteComment$: () => of({}), uploadFile$: () => of({}), deleteFile$: () => of({}) } },
        { provide: AssessmentService, useValue: { allAssessments$: () => of({}), assessPatient$: () => of({}) } },
        { provide: NotificationService, useValue: { messages$: () => of({}), sendMessage$: () => of({}), conversation$: () => of({}), replyToMessage$: () => of({}) } },
        { provide: HotToastService, useValue: { success: jasmine.createSpy(), error: jasmine.createSpy() } },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AssessmentsComponent);
    component = fixture.componentInstance;
    // Override store
    (component as any).store = mockStore;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call store on ngOnInit', () => {
    component.ngOnInit();
    expect(mockStore.getAllAssessments).toHaveBeenCalled();
    expect(mockStore.getAllPatients).toHaveBeenCalled();
  });

  it('should filter by search query', () => {
    component.searchQuery.set('jean');
    const filtered = component.filteredAssessments();
    expect(filtered.some((a: any) => a.patientName?.toLowerCase().includes('jean'))).toBeTrue();
  });

  it('should filter by risk level', () => {
    component.riskFilter.set('IN_DANGER');
    const filtered = component.filteredAssessments();
    expect(filtered.every((a: any) => a.riskLevel === 'IN_DANGER')).toBeTrue();
  });

  it('should filter by gender', () => {
    component.genderFilter.set('MALE');
    const filtered = component.filteredAssessments();
    expect(filtered.every((a: any) => a.gender === 'MALE')).toBeTrue();
  });

  it('should clear filters', () => {
    component.searchQuery.set('test');
    component.riskFilter.set('NONE');
    component.genderFilter.set('MALE');
    component.clearFilters();
    expect(component.searchQuery()).toBe('');
    expect(component.riskFilter()).toBe('');
    expect(component.genderFilter()).toBe('');
  });

  it('should change page size', () => {
    const event = { target: { value: '5' } } as any;
    component.changePageSize(event);
    expect(component.pageSize()).toBe(5);
    expect(mockStore.setCurrentPage).toHaveBeenCalledWith(0);
  });

  it('should navigate to page', () => {
    component.goToPage(2);
    expect(mockStore.setCurrentPage).toHaveBeenCalledWith(2);
  });

  it('should go to previous page', () => {
    mockStore.currentPage = () => 2;
    component.previousPage();
    expect(mockStore.setCurrentPage).toHaveBeenCalledWith(1);
  });

  it('should not go below page 0', () => {
    mockStore.currentPage = () => 0;
    mockStore.setCurrentPage.calls.reset();
    component.previousPage();
    expect(mockStore.setCurrentPage).not.toHaveBeenCalled();
  });

  it('should go to next page', () => {
    mockStore.currentPage = () => 0;
    component.pageSize.set(1);
    component.nextPage();
    expect(mockStore.setCurrentPage).toHaveBeenCalled();
  });

  it('should call reassess', () => {
    component.reassess('uuid-123');
    expect(mockStore.assessPatient).toHaveBeenCalledWith('uuid-123');
  });

  it('should return correct risk badge class', () => {
    expect(component.getRiskBadgeClass('NONE')).toContain('emerald');
    expect(component.getRiskBadgeClass('BORDERLINE')).toContain('amber');
    expect(component.getRiskBadgeClass('IN_DANGER')).toContain('orange');
    expect(component.getRiskBadgeClass('EARLY_ONSET')).toContain('red');
    expect(component.getRiskBadgeClass('UNKNOWN')).toContain('gray');
  });

  it('should return correct risk icon bg', () => {
    expect(component.getRiskIconBg('NONE')).toContain('emerald');
    expect(component.getRiskIconBg('BORDERLINE')).toContain('amber');
    expect(component.getRiskIconBg('IN_DANGER')).toContain('orange');
    expect(component.getRiskIconBg('EARLY_ONSET')).toContain('red');
    expect(component.getRiskIconBg('UNKNOWN')).toContain('gray');
  });

  it('should compute avg triggers', () => {
    const avg = component.avgTriggers();
    expect(avg).toBeGreaterThanOrEqual(0);
  });

  it('should update search on input', () => {
    const event = { target: { value: 'dupont' } } as any;
    component.onSearch(event);
    expect(component.searchQuery()).toBe('dupont');
  });

  it('should update risk filter on change', () => {
    const event = { target: { value: 'NONE' } } as any;
    component.onRiskChange(event);
    expect(component.riskFilter()).toBe('NONE');
  });

  it('should update gender filter on change', () => {
    const event = { target: { value: 'FEMALE' } } as any;
    component.onGenderChange(event);
    expect(component.genderFilter()).toBe('FEMALE');
  });
});
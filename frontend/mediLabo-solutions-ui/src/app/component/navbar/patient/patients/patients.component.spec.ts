import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PatientsComponent } from './patients.component';
import { provideRouter } from '@angular/router';
import { UserService } from '../../../../service/user.service';
import { StorageService } from '../../../../service/storage.service';
import { PatientService } from '../../../../service/patient.service';
import { NoteService } from '../../../../service/note.service';
import { AssessmentService } from '../../../../service/assessment.service';
import { NotificationService } from '../../../../service/notification.service';
import { HotToastService } from '@ngxpert/hot-toast';
import { of } from 'rxjs';

describe('PatientsComponent', () => {
  let component: PatientsComponent;
  let fixture: ComponentFixture<PatientsComponent>;

  const mockPatients = [
    { patientUuid: 'p1', userUuid: 'u1', gender: 'MALE', active: true, userInfo: { firstName: 'Jean', lastName: 'Dupont', email: 'jean@test.com' }, medicalRecordNumber: 'MRN001' },
    { patientUuid: 'p2', userUuid: 'u2', gender: 'FEMALE', active: true, userInfo: { firstName: 'Marie', lastName: 'Martin', email: 'marie@test.com' }, medicalRecordNumber: 'MRN002' },
    { patientUuid: 'p3', userUuid: 'u3', gender: 'MALE', active: false, userInfo: { firstName: 'Paul', lastName: 'Bernard', email: 'paul@test.com' }, medicalRecordNumber: 'MRN003' },
  ];

  const mockStore: any = {
    getAllPatientsPageable: jasmine.createSpy('getAllPatientsPageable'),
    getAllPatients: jasmine.createSpy('getAllPatients'),
    getAllAssessments: jasmine.createSpy('getAllAssessments'),
    createPatient: jasmine.createSpy('createPatient'),
    deletePatient: jasmine.createSpy('deletePatient'),
    restorePatient: jasmine.createSpy('restorePatient'),
    assessPatient: jasmine.createSpy('assessPatient'),
    patientPage: () => ({ content: mockPatients, currentPage: 0, totalPages: 2, totalElements: 3, size: 10 }),
    allPatients: () => mockPatients,
    allAssessments: () => [
      { patientUuid: 'p1', riskLevel: 'IN_DANGER' },
      { patientUuid: 'p2', riskLevel: 'NONE' },
      { patientUuid: 'p3', riskLevel: 'EARLY_ONSET' },
    ],
    users: () => [
      { userUuid: 'u4', firstName: 'Nouveau', lastName: 'User', email: 'new@test.com' }
    ],
    query: () => ({ page: 0, size: 10, sortBy: 'createdAt', direction: 'desc', status: '', type: '', filter: '' }),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientsComponent],
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

    fixture = TestBed.createComponent(PatientsComponent);
    component = fixture.componentInstance;
    (component as any).store = mockStore;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call store on ngOnInit', () => {
    component.ngOnInit();
    expect(mockStore.getAllPatientsPageable).toHaveBeenCalled();
    expect(mockStore.getAllPatients).toHaveBeenCalled();
    expect(mockStore.getAllAssessments).toHaveBeenCalled();
  });

  it('should compute enriched patients with riskLevel', () => {
    const enriched = component.enrichedPatients();
    expect(enriched[0].riskLevel).toBe('IN_DANGER');
    expect(enriched[1].riskLevel).toBe('NONE');
  });

  it('should filter by search query', () => {
    component.searchQuery.set('jean');
    const filtered = component.filteredPatients();
    expect(filtered.some((p: any) => p.userInfo?.firstName?.toLowerCase().includes('jean'))).toBeTrue();
  });

  it('should filter by gender', () => {
    component.genderFilter.set('FEMALE');
    const filtered = component.filteredPatients();
    expect(filtered.every((p: any) => p.gender === 'FEMALE')).toBeTrue();
  });

  it('should filter by risk', () => {
    component.riskFilter.set('IN_DANGER');
    const filtered = component.filteredPatients();
    expect(filtered.every((p: any) => p.riskLevel === 'IN_DANGER')).toBeTrue();
  });

  it('should filter by active status', () => {
    component.statusFilter.set('active');
    const filtered = component.filteredPatients();
    expect(filtered.every((p: any) => p.active !== false)).toBeTrue();
  });

  it('should filter by inactive status', () => {
    component.statusFilter.set('inactive');
    const filtered = component.filteredPatients();
    expect(filtered.every((p: any) => p.active === false)).toBeTrue();
  });

  it('should clear filters', () => {
    component.searchQuery.set('test');
    component.genderFilter.set('MALE');
    component.riskFilter.set('NONE');
    component.statusFilter.set('active');
    component.clearFilters();
    expect(component.searchQuery()).toBe('');
    expect(component.genderFilter()).toBe('');
    expect(component.riskFilter()).toBe('');
    expect(component.statusFilter()).toBe('');
  });

  it('should toggle create form', () => {
    expect(component.showCreateForm()).toBeFalse();
    component.toggleCreateForm();
    expect(component.showCreateForm()).toBeTrue();
  });

  it('should set selectedUserUuid on user selected', () => {
    const event = { target: { value: 'u4' } } as any;
    component.onUserSelected(event);
    expect(component.selectedUserUuid()).toBe('u4');
  });

  it('should compute availableUsers excluding existing patients', () => {
    const available = component.availableUsers();
    expect(available.every((u: any) => !['u1', 'u2', 'u3'].includes(u.userUuid))).toBeTrue();
  });

  it('should compute stats', () => {
    expect(component.totalPatients()).toBe(3);
    expect(component.malePatients()).toBe(2);
    expect(component.femalePatients()).toBe(1);
    expect(component.atRiskPatients()).toBe(2);
  });

  it('should go to page', () => {
    component.goToPage(1);
    expect(mockStore.getAllPatientsPageable).toHaveBeenCalled();
  });

  it('should not go to invalid page', () => {
    mockStore.getAllPatientsPageable.calls.reset();
    component.goToPage(-1);
    expect(mockStore.getAllPatientsPageable).not.toHaveBeenCalled();
  });

  it('should assess patient', () => {
    component.assessPatient('p1');
    expect(mockStore.assessPatient).toHaveBeenCalledWith('p1');
  });

  it('should restore patient', () => {
    component.restorePatient('p3');
    expect(mockStore.restorePatient).toHaveBeenCalledWith('p3');
  });

  it('should detect inactive patient', () => {
    expect(component.isInactive({ active: false })).toBeTrue();
    expect(component.isInactive({ active: true })).toBeFalse();
  });

  it('should return correct risk badge class', () => {
    expect(component.getRiskBadgeClass('NONE')).toContain('emerald');
    expect(component.getRiskBadgeClass('BORDERLINE')).toContain('amber');
    expect(component.getRiskBadgeClass('IN_DANGER')).toContain('orange');
    expect(component.getRiskBadgeClass('EARLY_ONSET')).toContain('red');
  });

  it('should return correct risk label', () => {
    expect(component.getRiskLabel('NONE')).toBe('Aucun');
    expect(component.getRiskLabel('BORDERLINE')).toBe('Borderline');
    expect(component.getRiskLabel('IN_DANGER')).toBe('En danger');
    expect(component.getRiskLabel('EARLY_ONSET')).toBe('Précoce');
  });

  it('should compute hasActiveFilters', () => {
    expect(component.hasActiveFilters()).toBeFalse();
    component.searchQuery.set('test');
    expect(component.hasActiveFilters()).toBeTrue();
  });

  it('should handle sort change', () => {
    component.changeSort('createdAt');
    expect(mockStore.getAllPatientsPageable).toHaveBeenCalled();
  });
  
  it('should update patient field', () => {
    component.patientForm.set({ dateOfBirth: '', gender: '', phone: '', address: '' });
    const event = { target: { value: '1990-01-01' } } as any;
    component.updatePatientField('dateOfBirth', event);
    expect(component.patientForm().dateOfBirth).toBe('1990-01-01');
  });

  it('should handle onSearch event', () => {
    const event = { target: { value: 'dupont' } } as any;
    component.onSearch(event);
    expect(component.searchQuery()).toBe('dupont');
  });

  it('should handle onGenderChange event', () => {
    const event = { target: { value: 'MALE' } } as any;
    component.onGenderChange(event);
    expect(component.genderFilter()).toBe('MALE');
  });

  it('should handle onRiskChange event', () => {
    const event = { target: { value: 'BORDERLINE' } } as any;
    component.onRiskChange(event);
    expect(component.riskFilter()).toBe('BORDERLINE');
  });

  it('should handle onStatusChange event', () => {
    const event = { target: { value: 'active' } } as any;
    component.onStatusChange(event);
    expect(component.statusFilter()).toBe('active');
  });

  it('should handle changePageSize', () => {
    const event = { target: { value: '20' } } as any;
    component.changePageSize(event);
    expect(mockStore.getAllPatientsPageable).toHaveBeenCalledWith(
      jasmine.objectContaining({ size: 20, page: 0 })
    );
  });

  it('should go to previous page', () => {
    mockStore.patientPage = () => ({ content: mockPatients, currentPage: 2, totalPages: 5, totalElements: 15, size: 10 });
    mockStore.getAllPatientsPageable.calls.reset();
    component.previousPage();
    expect(mockStore.getAllPatientsPageable).toHaveBeenCalled();
  });

  it('should go to next page', () => {
    mockStore.patientPage = () => ({ content: mockPatients, currentPage: 0, totalPages: 5, totalElements: 15, size: 10 });
    mockStore.getAllPatientsPageable.calls.reset();
    component.nextPage();
    expect(mockStore.getAllPatientsPageable).toHaveBeenCalled();
  });

  it('should not create patient when no user selected', () => {
    mockStore.createPatient.calls.reset();
    component.selectedUserUuid.set('');
    component.createPatient();
    expect(mockStore.createPatient).not.toHaveBeenCalled();
  });

  it('should not create patient when dateOfBirth missing', () => {
    mockStore.createPatient.calls.reset();
    component.selectedUserUuid.set('u4');
    component.patientForm.set({ dateOfBirth: '', gender: 'MALE', phone: '', address: '' });
    component.createPatient();
    expect(mockStore.createPatient).not.toHaveBeenCalled();
  });

  it('should create patient when all fields valid', () => {
    mockStore.createPatient.calls.reset();
    component.selectedUserUuid.set('u4');
    component.patientForm.set({ dateOfBirth: '1990-01-01', gender: 'MALE', phone: '0600000000', address: '10 rue test' });
    component.createPatient();
    expect(mockStore.createPatient).toHaveBeenCalledWith(jasmine.objectContaining({
      userUuid: 'u4',
      dateOfBirth: '1990-01-01',
      gender: 'MALE'
    }));
  });

  it('should restore patient and reload', (done) => {
    mockStore.restorePatient.calls.reset();
    component.restorePatient('p3');
    expect(mockStore.restorePatient).toHaveBeenCalledWith('p3');
    setTimeout(() => {
      expect(mockStore.getAllPatients).toHaveBeenCalled();
      done();
    }, 1100);
  });

  it('should delete patient when confirmed', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    mockStore.deletePatient.calls.reset();
    component.ngOnInit();
    component.deletePatient('p1');
    expect(mockStore.deletePatient).toHaveBeenCalledWith('p1');
  });

  it('should not delete patient when not confirmed', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    mockStore.deletePatient.calls.reset();
    component.deletePatient('p1');
    expect(mockStore.deletePatient).not.toHaveBeenCalled();
  });

  it('should change sort direction', () => {
    mockStore.query = () => ({ sortBy: 'createdAt', direction: 'desc', page: 0, size: 10, status: '', type: '', filter: '' });
    component.changeSort('createdAt');
    expect(mockStore.getAllPatientsPageable).toHaveBeenCalledWith(
      jasmine.objectContaining({ sortBy: 'createdAt', direction: 'asc' })
    );
  });

  it('should filter by email in search', () => {
    component.searchQuery.set('jean@test.com');
    const filtered = component.filteredPatients();
    expect(filtered.some((p: any) => p.userInfo?.email?.includes('jean@test.com'))).toBeTrue();
  });

  it('should filter by medicalRecordNumber', () => {
    component.searchQuery.set('MRN001');
    const filtered = component.filteredPatients();
    expect(filtered.some((p: any) => p.medicalRecordNumber === 'MRN001')).toBeTrue();
  });

  it('should compute selectedUser when uuid matches', () => {
    component.selectedUserUuid.set('u4');
    const user = component.selectedUser();
    expect(user?.userUuid).toBe('u4');
  });

  it('should return null selectedUser when no uuid', () => {
    component.selectedUserUuid.set('');
    expect(component.selectedUser()).toBeNull();
  });
  
});
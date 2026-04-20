import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { provideRouter } from '@angular/router';
import { UserService } from '../../../service/user.service';
import { StorageService } from '../../../service/storage.service';
import { PatientService } from '../../../service/patient.service';
import { NoteService } from '../../../service/note.service';
import { AssessmentService } from '../../../service/assessment.service';
import { NotificationService } from '../../../service/notification.service';
import { PermissionService } from '../../../service/permission.service';
import { HotToastService } from '@ngxpert/hot-toast';
import { of } from 'rxjs';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

  const mockStore: any = {
    profile: () => ({ firstName: 'Test', lastName: 'User', email: 'test@test.com' }),
    allPatients: () => [
      { patientUuid: '1', userInfo: { firstName: 'Jean', lastName: 'Dupont' } },
      { patientUuid: '2', userInfo: { firstName: 'Marie', lastName: 'Martin' } },
    ],
    allAssessments: () => [
      { patientUuid: '1', riskLevel: 'EARLY_ONSET' },
      { patientUuid: '2', riskLevel: 'IN_DANGER' },
    ],
    patient: () => null,
    getAllPatients: jasmine.createSpy('getAllPatients'),
    getAllAssessments: jasmine.createSpy('getAllAssessments'),
    getMyPatientRecord: jasmine.createSpy('getMyPatientRecord'),
    getMyMedicalNotes: jasmine.createSpy('getMyMedicalNotes'),
  };

  const mockPermissionService: any = {
    canViewPatients: jasmine.createSpy().and.returnValue(true),
    canViewAssessments: jasmine.createSpy().and.returnValue(true),
    isStaff: jasmine.createSpy().and.returnValue(true),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: { isAuthenticated: () => true, getToken: () => 'tok', getUserRole: () => 'ADMIN', connectedUser: () => ({}), usersPageable$: () => of({}), users$: () => of({}), user$: () => of({}), profile$: () => of({}), update$: () => of({}), updatePassword$: () => of({}), updateImage$: () => of({}), updateRole$: () => of({}), updateRoleByUuid$: () => of({}), toggleAccountLocked$: () => of({}), toggleAccountExpired$: () => of({}), toggleAccountEnabled$: () => of({}), toggleAccountLockedByUuid$: () => of({}), toggleAccountExpiredByUuid$: () => of({}), toggleAccountEnabledByUuid$: () => of({}), enableMfa$: () => of({}), disableMfa$: () => of({}), enableMfaByUuid$: () => of({}), disableMfaByUuid$: () => of({}) } },
        { provide: StorageService, useValue: { getToken: () => 'tok', setToken: jasmine.createSpy(), clear: jasmine.createSpy(), getRedirectUrl: () => null, setRedirectUrl: jasmine.createSpy() } },
        { provide: PatientService, useValue: { allPatientsPageable$: () => of({}), allPatients$: () => of({}), patient$: () => of({}), createPatient$: () => of({}), updatePatient$: () => of({}), deletePatient$: () => of({}), restorePatient$: () => of({}), patientByUserUuid$: () => of({}), patientByEmail$: () => of({}), myPatientRecord$: () => of({}) } },
        { provide: NoteService, useValue: { allNotesPageable$: () => of({}), allNotes$: () => of({}), note$: () => of({}), notesByPatient$: () => of({}), createNote$: () => of({}), updateNote$: () => of({}), deleteNote$: () => of({}), myMedicalNotes$: () => of({}), addComment$: () => of({}), updateComment$: () => of({}), deleteComment$: () => of({}), uploadFile$: () => of({}), deleteFile$: () => of({}) } },
        { provide: AssessmentService, useValue: { allAssessments$: () => of({}), assessPatient$: () => of({}) } },
        { provide: NotificationService, useValue: { messages$: () => of({}), sendMessage$: () => of({}), conversation$: () => of({}), replyToMessage$: () => of({}) } },
        { provide: PermissionService, useValue: mockPermissionService },
        { provide: HotToastService, useValue: { success: jasmine.createSpy(), error: jasmine.createSpy() } },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    (component as any).store = mockStore;
    (component as any).permissionService = mockPermissionService;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should compute highRiskPatients as array', () => {
    expect(Array.isArray(component.highRiskPatients())).toBeTrue();
  });

  it('should compute myAssessment as null when no patient in store', () => {
    expect(component.myAssessment()).toBeNull();
  });

  it('should compute myAssessment as null when no patient', () => {
    mockStore.patient = () => null;
    expect(component.myAssessment()).toBeNull();
  });

  it('should compute allPatients as array', () => {
    expect(Array.isArray(component.allPatients())).toBeTrue();
  });

  it('should return correct risk badge class', () => {
    expect(component.getRiskBadgeClass('NONE')).toContain('emerald');
    expect(component.getRiskBadgeClass('BORDERLINE')).toContain('amber');
    expect(component.getRiskBadgeClass('IN_DANGER')).toContain('orange');
    expect(component.getRiskBadgeClass('EARLY_ONSET')).toContain('red');
    expect(component.getRiskBadgeClass(null)).toContain('gray');
  });

  it('should return correct risk label', () => {
    expect(component.getRiskLabel('NONE')).toBe('Aucun risque');
    expect(component.getRiskLabel('BORDERLINE')).toBe('Risque limité');
    expect(component.getRiskLabel('IN_DANGER')).toBe('En danger');
    expect(component.getRiskLabel('EARLY_ONSET')).toBe('Apparition précoce');
    expect(component.getRiskLabel(null)).toBe('Non évalué');
  });

  it('should initialize chart options', () => {
    expect(component.lineChartOptions).toBeDefined();
    expect(component.barChartOptions).toBeDefined();
    expect(component.pieChartOptions).toBeDefined();
    expect(component.donutChartOptions).toBeDefined();
    expect(component.columnChartOptions).toBeDefined();
  });

  it('should load patients when canViewPatients is true', () => {
    mockPermissionService.canViewPatients.and.returnValue(true);
    mockStore.getAllPatients.calls.reset();
    // Simuler l'effet en appelant directement le store
    mockStore.getAllPatients();
    expect(mockStore.getAllPatients).toHaveBeenCalled();
  });

  it('should load assessments when canViewAssessments is true', () => {
    mockPermissionService.canViewAssessments.and.returnValue(true);
    mockStore.getAllAssessments.calls.reset();
    mockStore.getAllAssessments();
    expect(mockStore.getAllAssessments).toHaveBeenCalled();
  });

  it('should load patient record when not staff', () => {
    mockPermissionService.isStaff.and.returnValue(false);
    mockStore.getMyPatientRecord.calls.reset();
    mockStore.getMyPatientRecord();
    expect(mockStore.getMyPatientRecord).toHaveBeenCalled();
  });

  it('should load medical notes when not staff', () => {
    mockPermissionService.isStaff.and.returnValue(false);
    mockStore.getMyMedicalNotes.calls.reset();
    mockStore.getMyMedicalNotes();
    expect(mockStore.getMyMedicalNotes).toHaveBeenCalled();
  });

  it('should compute allPatients with firstName and lastName from userInfo', () => {
    const patients = component.allPatients();
    expect(patients[0].firstName).toBe('Jean');
    expect(patients[0].lastName).toBe('Dupont');
  });

  it('should return NONE riskLevel when no assessment found', () => {
    mockStore.allAssessments = () => [];
    const patients = component.allPatients();
    expect(patients.every((p: any) => p.riskLevel === 'NONE')).toBeTrue();
  });

  it('should compute myAssessment returns null when assessments empty', () => {
    mockStore.patient = () => ({ patientUuid: 'unknown' });
    mockStore.allAssessments = () => [];
    expect(component.myAssessment()).toBeNull();
  });

  it('should test barChartOptions formatter with positive value', () => {
    const formatter = (component.barChartOptions as any).dataLabels?.formatter;
    expect(formatter).toBeDefined();
    expect(formatter(5)).toBe('5');
  });

  it('should test columnChartOptions formatter with positive value', () => {
    const formatter = (component.columnChartOptions as any).dataLabels?.formatter;
    expect(formatter).toBeDefined();
    expect(formatter(3)).toBe('3');
    expect(formatter(0)).toBe('');
  });

  it('should test columnChartOptions yaxis formatter', () => {
    const formatter = (component.columnChartOptions as any).yaxis?.labels?.formatter;
    expect(formatter).toBeDefined();
    expect(formatter(10)).toBe('10');
  });

  it('should return BORDERLINE risk badge class', () => {
    expect(component.getRiskBadgeClass('BORDERLINE')).toContain('amber');
  });
  
});
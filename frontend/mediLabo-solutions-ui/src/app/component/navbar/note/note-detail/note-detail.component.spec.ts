import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoteDetailComponent } from './note-detail.component';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { UserService } from '../../../../service/user.service';
import { StorageService } from '../../../../service/storage.service';
import { PatientService } from '../../../../service/patient.service';
import { NoteService } from '../../../../service/note.service';
import { AssessmentService } from '../../../../service/assessment.service';
import { NotificationService } from '../../../../service/notification.service';
import { PermissionService } from '../../../../service/permission.service';
import { HotToastService } from '@ngxpert/hot-toast';
import { of } from 'rxjs';

describe('NoteDetailComponent', () => {
  let component: NoteDetailComponent;
  let fixture: ComponentFixture<NoteDetailComponent>;

  const mockNote = {
    noteUuid: 'note-1',
    patientUuid: 'patient-1',
    content: 'Contenu de la note',
    comments: [],
    files: []
  };

  const mockStore: any = {
    loading: () => false,
    noteDetail: () => mockNote,
    allPatients: () => [{ patientUuid: 'patient-1', userInfo: { firstName: 'Jean', lastName: 'Dupont', imageUrl: null } }],
    profile: () => ({ firstName: 'Dr', lastName: 'Martin', imageUrl: null }),
    getNote: jasmine.createSpy('getNote'),
    getAllPatients: jasmine.createSpy('getAllPatients'),
    updateNote: jasmine.createSpy('updateNote'),
    addComment: jasmine.createSpy('addComment'),
    updateComment: jasmine.createSpy('updateComment'),
    deleteComment: jasmine.createSpy('deleteComment'),
    uploadFile: jasmine.createSpy('uploadFile'),
    deleteFile: jasmine.createSpy('deleteFile'),
  };

  const mockPermissionService: any = {
    isStaff: jasmine.createSpy().and.returnValue(true),
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
    downloadFile$: () => of({ body: new Blob() }),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoteDetailComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { params: { noteUuid: 'note-1' } }, queryParamMap: of({}) } },
        { provide: UserService, useValue: { isAuthenticated: () => true, getToken: () => 'tok', getUserRole: () => 'ADMIN', connectedUser: () => ({}), usersPageable$: () => of({}), users$: () => of({}), user$: () => of({}), profile$: () => of({}), update$: () => of({}), updatePassword$: () => of({}), updateImage$: () => of({}), updateRole$: () => of({}), updateRoleByUuid$: () => of({}), toggleAccountLocked$: () => of({}), toggleAccountExpired$: () => of({}), toggleAccountEnabled$: () => of({}), toggleAccountLockedByUuid$: () => of({}), toggleAccountExpiredByUuid$: () => of({}), toggleAccountEnabledByUuid$: () => of({}), enableMfa$: () => of({}), disableMfa$: () => of({}), enableMfaByUuid$: () => of({}), disableMfaByUuid$: () => of({}) } },
        { provide: StorageService, useValue: { getToken: () => 'tok', setToken: jasmine.createSpy(), clear: jasmine.createSpy(), getRedirectUrl: () => null, setRedirectUrl: jasmine.createSpy() } },
        { provide: PatientService, useValue: { allPatientsPageable$: () => of({}), allPatients$: () => of({}), patient$: () => of({}), createPatient$: () => of({}), updatePatient$: () => of({}), deletePatient$: () => of({}), restorePatient$: () => of({}), patientByUserUuid$: () => of({}), patientByEmail$: () => of({}), myPatientRecord$: () => of({}) } },
        { provide: NoteService, useValue: mockNoteService },
        { provide: AssessmentService, useValue: { allAssessments$: () => of({}), assessPatient$: () => of({}) } },
        { provide: NotificationService, useValue: { messages$: () => of({}), sendMessage$: () => of({}), conversation$: () => of({}), replyToMessage$: () => of({}) } },
        { provide: PermissionService, useValue: mockPermissionService },
        { provide: HotToastService, useValue: { success: jasmine.createSpy(), error: jasmine.createSpy() } },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NoteDetailComponent);
    component = fixture.componentInstance;
    (component as any).store = mockStore;
    (component as any).permissionService = mockPermissionService;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call store on ngOnInit', () => {
    component.ngOnInit();
    expect(mockStore.getNote).toHaveBeenCalledWith('note-1');
  });

  it('should start editing', () => {
    component.ngOnInit();
    component.startEditing();
    expect(component.isEditing()).toBeTrue();
  });

  it('should cancel editing', () => {
    component.isEditing.set(true);
    component.cancelEditing();
    expect(component.isEditing()).toBeFalse();
    expect(component.editContent()).toBe('');
  });

  it('should save note with content', () => {
    component.ngOnInit();
    component.editContent.set('Nouveau contenu');
    component.saveNote();
    expect(mockStore.updateNote).toHaveBeenCalled();
    expect(component.isEditing()).toBeFalse();
  });

  it('should not save note when content empty', () => {
    mockStore.updateNote.calls.reset();
    component.editContent.set('   ');
    component.saveNote();
    expect(mockStore.updateNote).not.toHaveBeenCalled();
  });

  it('should add comment when content non empty', () => {
    component.newComment.set('Mon commentaire');
    component.addComment();
    expect(mockStore.addComment).toHaveBeenCalled();
    expect(component.newComment()).toBe('');
  });

  it('should not add comment when content empty', () => {
    mockStore.addComment.calls.reset();
    component.newComment.set('');
    component.addComment();
    expect(mockStore.addComment).not.toHaveBeenCalled();
  });

  it('should start edit comment', () => {
    component.startEditComment('comment-1', 'Contenu du commentaire');
    expect(component.editingCommentUuid()).toBe('comment-1');
    expect(component.editCommentContent()).toBe('Contenu du commentaire');
  });

  it('should cancel edit comment', () => {
    component.editingCommentUuid.set('comment-1');
    component.cancelEditComment();
    expect(component.editingCommentUuid()).toBeNull();
    expect(component.editCommentContent()).toBe('');
  });

  it('should save comment', () => {
    component.editCommentContent.set('Commentaire modifié');
    component.saveComment('comment-1');
    expect(mockStore.updateComment).toHaveBeenCalled();
  });

  it('should not save comment when empty', () => {
    mockStore.updateComment.calls.reset();
    component.editCommentContent.set('   ');
    component.saveComment('comment-1');
    expect(mockStore.updateComment).not.toHaveBeenCalled();
  });

  it('should close preview', () => {
    component.previewFile.set({ fileName: 'test.pdf', fileUuid: 'file-1', fileType: 'pdf', url: null });
    component.closePreview();
    expect(component.previewFile()).toBeNull();
  });

  it('should detect image type', () => {
    expect(component.isImage('image/jpeg')).toBeTrue();
    expect(component.isImage('application/pdf')).toBeFalse();
  });

  it('should detect image file type', () => {
    expect(component.isImageFile('jpg')).toBeTrue();
    expect(component.isImageFile('png')).toBeTrue();
    expect(component.isImageFile('pdf')).toBeFalse();
  });

  it('should return correct file icon', () => {
    expect(component.getFileIcon('doc.pdf')).toContain('red');
    expect(component.getFileIcon('file.docx')).toContain('blue');
    expect(component.getFileIcon('photo.jpg')).toContain('emerald');
    expect(component.getFileIcon('file.txt')).toContain('gray');
  });

  it('should getSafeUrl return null for null input', () => {
    expect(component.getSafeUrl(null)).toBeNull();
  });

  it('should getSafeUrl return SafeResourceUrl for valid url', () => {
    const result = component.getSafeUrl('http://example.com/file.pdf');
    expect(result).toBeTruthy();
  });

 
  // note computed — branche patient (non staff)
  it('should compute note for non-staff using profile', () => {
    mockPermissionService.isStaff.and.returnValue(false);
    const note = component.note();
    expect(note?.patientName).toBe('Dr Martin');
  });

  it('should return null when no noteDetail', () => {
    mockStore.noteDetail = () => null;
    expect(component.note()).toBeNull();
  });

  // ngOnInit — non staff
  it('should not call getAllPatients if not staff', () => {
    mockPermissionService.isStaff.and.returnValue(false);
    mockStore.getAllPatients.calls.reset();
    component.ngOnInit();
    expect(mockStore.getAllPatients).not.toHaveBeenCalled();
  });

  // onFileChange
  it('should handle file change with image file', () => {
    const file = new File(['content'], 'photo.jpg', { type: 'image/jpeg' });
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [file] });
    const event = { target: input } as any;

    component.onFileChange(event);

    expect(component.pendingFiles().length).toBe(1);
    expect(component.pendingFiles()[0].name).toBe('photo.jpg');
    expect(component.filesToUpload.length).toBe(1);
  });

  it('should handle file change with non-previewable file', () => {
    const file = new File(['content'], 'document.txt', { type: 'text/plain' });
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [file] });
    const event = { target: input } as any;

    component.onFileChange(event);

    expect(component.pendingFiles()[0].previewUrl).toBeNull();
  });

  it('should do nothing when no files selected', () => {
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: null });
    const event = { target: input } as any;

    component.onFileChange(event);

    expect(component.pendingFiles().length).toBe(0);
  });

  // removePendingFile
  it('should remove pending file', () => {
    component.pendingFiles.set([
      { name: 'photo.jpg', size: '10 KB', type: 'image/jpeg', previewUrl: null },
      { name: 'doc.pdf', size: '20 KB', type: 'application/pdf', previewUrl: null },
    ]);

    component.removePendingFile({ name: 'photo.jpg', size: '10 KB', type: 'image/jpeg', previewUrl: null });

    expect(component.pendingFiles().length).toBe(1);
    expect(component.pendingFiles()[0].name).toBe('doc.pdf');
  });

  // uploadFiles
  it('should upload pending files and clear state', () => {
    const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
    component.filesToUpload = [file];
    component.pendingFiles.set([{ name: 'test.pdf', size: '10 KB', type: 'application/pdf', previewUrl: null }]);

    component.uploadFiles();

    expect(mockStore.uploadFile).toHaveBeenCalled();
    expect(component.filesToUpload.length).toBe(0);
    expect(component.pendingFiles().length).toBe(0);
  });

  // downloadFile
  it('should download file', () => {
    const mockBlob = new Blob(['content'], { type: 'application/pdf' });
    (mockNoteService as any).downloadFile$ = jasmine.createSpy().and.returnValue(of({ body: mockBlob }));
    (component as any).noteService = mockNoteService;
    component.noteUuid = 'note-1';

    spyOn(document, 'createElement').and.callThrough();

    expect(() => component.downloadFile('file-1', 'test.pdf')).not.toThrow();
  });

  // openPreview — fichier non prévisualisable → download
  it('should call downloadFile for non-previewable file', () => {
    spyOn(component, 'downloadFile');
    const file = { name: 'archive.zip', fileUuid: 'f-1', contentType: 'application/zip' };
    component.openPreview(file);
    expect(component.downloadFile).toHaveBeenCalled();
  });

  it('should not open preview if already loading', () => {
    component.previewLoading.set(true);
    const file = { name: 'photo.jpg', fileUuid: 'f-1', contentType: 'image/jpeg' };
    spyOn(component, 'downloadFile');
    component.openPreview(file);
    expect(mockStore.uploadFile).not.toHaveBeenCalled();
  });

  it('should open preview for pdf file', () => {
    const mockBlob = new Blob(['pdf'], { type: 'application/pdf' });
    (mockNoteService as any).downloadFile$ = jasmine.createSpy().and.returnValue(of({ body: mockBlob }));
    (component as any).noteService = mockNoteService;
    component.noteUuid = 'note-1';

    const file = { name: 'document.pdf', fileUuid: 'f-1', contentType: 'application/pdf' };
    component.openPreview(file);

    expect(component.previewLoading()).toBeFalse();
    expect(component.previewFile()).toBeTruthy();
  });

});
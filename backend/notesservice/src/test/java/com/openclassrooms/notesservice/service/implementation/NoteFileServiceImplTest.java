package com.openclassrooms.notesservice.service.implementation;

import com.openclassrooms.notesservice.config.FileStorageConfig;
import com.openclassrooms.notesservice.dto.PatientInfo;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.model.FileAttachment;
import com.openclassrooms.notesservice.model.Note;
import com.openclassrooms.notesservice.repository.NoteRepository;
import com.openclassrooms.notesservice.service.FileStorageService;
import com.openclassrooms.notesservice.service.PatientServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoteFileServiceImpl Unit Tests")
class NoteFileServiceImplTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileStorageConfig fileStorageConfig;

    @Mock
    private PatientServiceClient patientServiceClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NoteFileServiceImpl noteFileService;

    private Note testNote;
    private FileAttachment testFileAttachment;
    private Jwt mockJwt;
    private PatientInfo patientInfo;
    private MultipartFile mockFile;

    private static final String NOTE_UUID = "note-uuid-123";
    private static final String FILE_UUID = "file-uuid-456";
    private static final String UPLOADER_UUID = "uploader-uuid-789";
    private static final String PATIENT_UUID = "patient-uuid-000";
    private static final String PRACTITIONER_UUID = "practitioner-uuid-111";

    @BeforeEach
    void setUp() {
        // Setup JWT mock
        mockJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(UPLOADER_UUID)
                .claim("firstName", "Jean")
                .claim("lastName", "Dupont")
                .claim("authorities", java.util.List.of("ROLE_PRACTITIONER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // Setup file attachment
        testFileAttachment = FileAttachment.builder()
                .fileUuid(FILE_UUID)
                .originalName("test-document.pdf")
                .storedName(FILE_UUID + ".pdf")
                .extension("pdf")
                .contentType("application/pdf")
                .size(1024L)
                .formattedSize("1 KB")
                .uri("notes/" + NOTE_UUID + "/" + FILE_UUID + ".pdf")
                .uploadedByUuid(UPLOADER_UUID)
                .uploadedByName("Jean Dupont")
                .uploadedByRole("PRACTITIONER")
                .uploadedAt(LocalDateTime.now())
                .build();

        // Setup note
        testNote = Note.builder()
                .id("mongo-id")
                .noteUuid(NOTE_UUID)
                .patientUuid(PATIENT_UUID)
                .practitionerUuid(PRACTITIONER_UUID)
                .practitionerName("Dr. Martin")
                .content("Note de test")
                .active(true)
                .files(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup patient info
        patientInfo = PatientInfo.builder()
                .patientUuid(PATIENT_UUID)
                .userInfo(new PatientInfo.UserInfo(
                        "Patient",
                        "Test",
                        "patient@test.com",
                        null,
                        null,
                        null
                ))
                .build();

        // Setup mock file
        mockFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "test content".getBytes()
        );
    }

    private void setupPatientServiceClientMock() {
        when(patientServiceClient.getPatientContactInfo(anyString()))
                .thenReturn(Mono.just(patientInfo));
    }

    @Nested
    @DisplayName("uploadFile() Tests")
    class UploadFileTests {

        @Test
        @DisplayName("Should upload file successfully")
        void shouldUploadFileSuccessfully() {
            // Given
            setupPatientServiceClientMock();

            FileStorageService.StoredFileInfo storedInfo = FileStorageService.StoredFileInfo.builder()
                    .fileUuid(FILE_UUID)
                    .originalName("test-document.pdf")
                    .storedName(FILE_UUID + ".pdf")
                    .extension("pdf")
                    .contentType("application/pdf")
                    .size(1024L)
                    .formattedSize("1 KB")
                    .relativePath("notes/" + NOTE_UUID + "/" + FILE_UUID + ".pdf")
                    .build();

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));
            when(fileStorageService.storeFile(anyString(), any(MultipartFile.class)))
                    .thenReturn(storedInfo);
            when(noteRepository.save(any(Note.class))).thenReturn(testNote);
            when(fileStorageConfig.getBaseDownloadUrl()).thenReturn("/api/notes");

            // When & Then
            StepVerifier.create(noteFileService.uploadFile(NOTE_UUID, mockFile, mockJwt))
                    .expectNextMatches(response ->
                            response.getFileUuid().equals(FILE_UUID) &&
                                    response.getName().equals("test-document.pdf"))
                    .verifyComplete();

            verify(fileStorageService).storeFile(anyString(), any(MultipartFile.class));
            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("Should error when note not found")
        void shouldErrorWhenNoteNotFound() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(anyString()))
                    .thenReturn(Optional.empty());

            // When & Then
            StepVerifier.create(noteFileService.uploadFile("unknown-uuid", mockFile, mockJwt))
                    .expectErrorMatches(err -> err instanceof ApiException &&
                            err.getMessage().contains("Note non trouvée"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("getFiles() Tests")
    class GetFilesTests {

        @Test
        @DisplayName("Should return files for note")
        void shouldReturnFilesForNote() {
            // Given
            testNote.getFiles().add(testFileAttachment);

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));
            when(fileStorageConfig.getBaseDownloadUrl()).thenReturn("/api/notes");

            // When & Then
            StepVerifier.create(noteFileService.getFiles(NOTE_UUID))
                    .expectNextMatches(response ->
                            response.getFileUuid().equals(FILE_UUID))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty flux when no files")
        void shouldReturnEmptyFluxWhenNoFiles() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));

            // When & Then
            StepVerifier.create(noteFileService.getFiles(NOTE_UUID))
                    .expectNextCount(0)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("downloadFile() Tests")
    class DownloadFileTests {

        @Test
        @DisplayName("Should download file successfully")
        void shouldDownloadFileSuccessfully() {
            // Given
            testNote.getFiles().add(testFileAttachment);
            Resource mockResource = new ByteArrayResource("test content".getBytes());

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));
            when(fileStorageService.loadFileAsResource(NOTE_UUID, FILE_UUID, "pdf"))
                    .thenReturn(mockResource);

            // When & Then
            StepVerifier.create(noteFileService.downloadFile(NOTE_UUID, FILE_UUID))
                    .expectNextMatches(download ->
                            download.getFilename().equals("test-document.pdf") &&
                                    download.getContentType().equals("application/pdf"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should error when file not found")
        void shouldErrorWhenFileNotFound() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));

            // When & Then
            StepVerifier.create(noteFileService.downloadFile(NOTE_UUID, "unknown-file"))
                    .expectErrorMatches(err -> err instanceof ApiException &&
                            err.getMessage().contains("Fichier non trouvé"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("deleteFile() Tests")
    class DeleteFileTests {

        @Test
        @DisplayName("Should delete file when uploader")
        void shouldDeleteFileWhenUploader() {
            // Given
            setupPatientServiceClientMock();
            testNote.getFiles().add(testFileAttachment);

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));
            when(fileStorageService.deleteFile(NOTE_UUID, FILE_UUID, "pdf"))
                    .thenReturn(true);
            when(noteRepository.save(any(Note.class))).thenReturn(testNote);

            // When & Then
            StepVerifier.create(noteFileService.deleteFile(NOTE_UUID, FILE_UUID, mockJwt))
                    .verifyComplete();

            verify(fileStorageService).deleteFile(NOTE_UUID, FILE_UUID, "pdf");
            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("Should delete file when note practitioner")
        void shouldDeleteFileWhenNotePractitioner() {
            // Given
            setupPatientServiceClientMock();

            FileAttachment otherFile = testFileAttachment.toBuilder()
                    .uploadedByUuid("other-uploader")
                    .build();
            testNote.getFiles().add(otherFile);

            Jwt practitionerJwt = Jwt.withTokenValue("token")
                    .header("alg", "RS256")
                    .subject(PRACTITIONER_UUID)
                    .claim("firstName", "Dr")
                    .claim("lastName", "Martin")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));
            when(fileStorageService.deleteFile(NOTE_UUID, FILE_UUID, "pdf"))
                    .thenReturn(true);
            when(noteRepository.save(any(Note.class))).thenReturn(testNote);

            // When & Then
            StepVerifier.create(noteFileService.deleteFile(NOTE_UUID, FILE_UUID, practitionerJwt))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should error when not authorized")
        void shouldErrorWhenNotAuthorized() {
            // Given
            FileAttachment otherFile = testFileAttachment.toBuilder()
                    .uploadedByUuid("other-uploader")
                    .build();
            testNote.getFiles().add(otherFile);

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));

            // When & Then
            StepVerifier.create(noteFileService.deleteFile(NOTE_UUID, FILE_UUID, mockJwt))
                    .expectErrorMatches(err -> err instanceof ApiException &&
                            err.getMessage().contains("Non autorisé"))
                    .verify();
        }
    }
}
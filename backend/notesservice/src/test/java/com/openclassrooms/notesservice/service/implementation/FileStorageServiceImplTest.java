package com.openclassrooms.notesservice.service.implementation;

import com.openclassrooms.notesservice.config.FileStorageConfig;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FileStorageServiceImpl Unit Tests")
class FileStorageServiceImplTest {

    @Mock
    private FileStorageConfig config;

    private FileStorageServiceImpl fileStorageService;

    @TempDir
    Path tempDir;

    private static final String NOTE_UUID = "note-uuid-123";

    @BeforeEach
    void setUp() {
        // Configuration de base - tous les mocks en mode lenient
        lenient().when(config.getUploadDir()).thenReturn(tempDir.toString());
        lenient().when(config.getMaxFileSize()).thenReturn(10 * 1024 * 1024L); // 10MB
        lenient().when(config.getAllowedExtensions()).thenReturn(List.of("pdf", "jpg", "png", "doc", "docx"));
        lenient().when(config.getAllowedContentTypes()).thenReturn(List.of(
                "application/pdf",
                "image/jpeg",
                "image/png",
                "application/msword"
        ));

        fileStorageService = new FileStorageServiceImpl(config);
        fileStorageService.init();
    }

    @Nested
    @DisplayName("storeFile() Tests")
    class StoreFileTests {

        @Test
        @DisplayName("Should store file successfully")
        void shouldStoreFileSuccessfully() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-document.pdf",
                    "application/pdf",
                    "test content".getBytes()
            );

            // When
            FileStorageService.StoredFileInfo result = fileStorageService.storeFile(NOTE_UUID, file);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalName()).isEqualTo("test-document.pdf");
            assertThat(result.getExtension()).isEqualTo("pdf");
            assertThat(result.getContentType()).isEqualTo("application/pdf");
            assertThat(result.getFileUuid()).isNotBlank();
            assertThat(result.getSize()).isEqualTo("test content".getBytes().length);

            // Verify file exists
            Path storedFile = tempDir.resolve("notes").resolve(NOTE_UUID).resolve(result.getStoredName());
            assertThat(Files.exists(storedFile)).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when file is empty")
        void shouldThrowExceptionWhenFileIsEmpty() {
            // Given
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.pdf",
                    "application/pdf",
                    new byte[0]
            );

            // When & Then
            assertThatThrownBy(() -> fileStorageService.storeFile(NOTE_UUID, emptyFile))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("vide");
        }

        @Test
        @DisplayName("Should throw exception when extension not allowed")
        void shouldThrowExceptionWhenExtensionNotAllowed() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "malware.exe",
                    "application/octet-stream",
                    "malicious content".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> fileStorageService.storeFile(NOTE_UUID, file))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Extension non autorisée");
        }

        @Test
        @DisplayName("Should throw exception when file too large")
        void shouldThrowExceptionWhenFileTooLarge() {
            // Given - Override le mock pour ce test spécifique
            when(config.getMaxFileSize()).thenReturn(10L); // 10 bytes max

            byte[] largeContent = new byte[100];
            MockMultipartFile largeFile = new MockMultipartFile(
                    "file",
                    "large.pdf",
                    "application/pdf",
                    largeContent
            );

            // When & Then
            assertThatThrownBy(() -> fileStorageService.storeFile(NOTE_UUID, largeFile))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("trop volumineux");
        }

        @Test
        @DisplayName("Should throw exception when filename contains path traversal")
        void shouldThrowExceptionWhenFilenameContainsPathTraversal() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "../../../etc/passwd",
                    "application/pdf",
                    "content".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> fileStorageService.storeFile(NOTE_UUID, file))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("invalide");
        }
    }

    @Nested
    @DisplayName("loadFileAsResource() Tests")
    class LoadFileAsResourceTests {

        @Test
        @DisplayName("Should load file as resource")
        void shouldLoadFileAsResource() {
            // Given - First store a file
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.pdf",
                    "application/pdf",
                    "test content".getBytes()
            );
            FileStorageService.StoredFileInfo storedInfo = fileStorageService.storeFile(NOTE_UUID, file);

            // When
            Resource resource = fileStorageService.loadFileAsResource(
                    NOTE_UUID,
                    storedInfo.getFileUuid(),
                    "pdf"
            );

            // Then
            assertThat(resource).isNotNull();
            assertThat(resource.exists()).isTrue();
            assertThat(resource.isReadable()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when file not found")
        void shouldThrowExceptionWhenFileNotFound() {
            // When & Then
            assertThatThrownBy(() -> fileStorageService.loadFileAsResource(
                    NOTE_UUID,
                    "unknown-file-uuid",
                    "pdf"
            ))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("non trouvé");
        }
    }

    @Nested
    @DisplayName("deleteFile() Tests")
    class DeleteFileTests {

        @Test
        @DisplayName("Should delete file successfully")
        void shouldDeleteFileSuccessfully() {
            // Given - First store a file
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.pdf",
                    "application/pdf",
                    "test content".getBytes()
            );
            FileStorageService.StoredFileInfo storedInfo = fileStorageService.storeFile(NOTE_UUID, file);

            Path storedFile = tempDir.resolve("notes").resolve(NOTE_UUID).resolve(storedInfo.getStoredName());
            assertThat(Files.exists(storedFile)).isTrue();

            // When
            boolean deleted = fileStorageService.deleteFile(NOTE_UUID, storedInfo.getFileUuid(), "pdf");

            // Then
            assertThat(deleted).isTrue();
            assertThat(Files.exists(storedFile)).isFalse();
        }

        @Test
        @DisplayName("Should return false when file does not exist")
        void shouldReturnFalseWhenFileDoesNotExist() {
            // When
            boolean deleted = fileStorageService.deleteFile(NOTE_UUID, "unknown-uuid", "pdf");

            // Then
            assertThat(deleted).isFalse();
        }
    }

    @Nested
    @DisplayName("formatFileSize() Tests")
    class FormatFileSizeTests {

        @Test
        @DisplayName("Should format bytes correctly")
        void shouldFormatBytesCorrectly() {
            assertThat(FileStorageServiceImpl.formatFileSize(500)).isEqualTo("500 B");
        }

        @Test
        @DisplayName("Should format megabytes correctly")
        void shouldFormatMegabytesCorrectly() {
            assertThat(FileStorageServiceImpl.formatFileSize(1024 * 1024)).isEqualTo("1 MB");
            assertThat(FileStorageServiceImpl.formatFileSize(5 * 1024 * 1024)).isEqualTo("5 MB");
        }

        @Test
        @DisplayName("Should handle zero size")
        void shouldHandleZeroSize() {
            assertThat(FileStorageServiceImpl.formatFileSize(0)).isEqualTo("0 B");
        }
    }
}
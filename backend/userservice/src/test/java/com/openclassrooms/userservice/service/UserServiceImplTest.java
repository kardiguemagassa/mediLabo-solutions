package com.openclassrooms.userservice.service;

import com.openclassrooms.userservice.event.Event;
import com.openclassrooms.userservice.exception.ApiException;
import com.openclassrooms.userservice.model.User;
import com.openclassrooms.userservice.model.AccountToken;
import com.openclassrooms.userservice.model.Credential;
import com.openclassrooms.userservice.model.Device;
import com.openclassrooms.userservice.model.PasswordToken;
import com.openclassrooms.userservice.repository.UserRepository;
import com.openclassrooms.userservice.enumeration.EventType;
import com.openclassrooms.userservice.service.impl.UserServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder encoder;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private UserServiceImpl userService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "photoDirectory", tempDir.toString() + "/");
    }

    @Test
    @DisplayName("createUser doit encoder le mot de passe et publier un événement")
    void createUser_ShouldEncodePasswordAndPublishEvent() {
        // GIVEN
        String email = "john@example.com";
        String password = "plainPassword";
        String encodedPassword = "encodedPassword";
        String token = "activation-token";

        when(encoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.createUser(anyString(), anyString(), eq(email), anyString(), eq(encodedPassword)))
                .thenReturn(token);

        // WHEN
        userService.createUser("John", "Doe", email, "john123", password);

        // THEN
        verify(encoder).encode(password);
        verify(userRepository).createUser(anyString(), anyString(), eq(email), anyString(), eq(encodedPassword));
        verify(publisher).publishEvent(any(Event.class));
    }

    @Test
    @DisplayName("getUserByUuid doit retourner l'utilisateur si présent")
    void getUserByUuid_ShouldReturnUser() {
        // GIVEN
        User mockUser = new User();
        mockUser.setUserUuid("uuid-123");
        when(userRepository.getUserByUuid("uuid-123")).thenReturn(mockUser);

        // WHEN
        User result = userService.getUserByUuid("uuid-123");

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getUserUuid()).isEqualTo("uuid-123");
    }

    @Test
    @DisplayName("updatePassword doit lever une exception si les mots de passe ne matchent pas")
    void updatePassword_ShouldThrowException_WhenPasswordsDoNotMatch() {
        // WHEN & THEN
        assertThatThrownBy(() ->
                userService.updatePassword("uuid", "current", "newPass", "wrongConfirm")
        ).isInstanceOf(ApiException.class)
                .hasMessageContaining("Les mots de passe ne correspondent pas");
    }

    @Test
    @DisplayName("updatePassword doit fonctionner si le mot de passe actuel est correct")
    void updatePassword_ShouldWork_WhenCurrentPasswordIsCorrect() {
        // GIVEN
        String userUuid = "uuid-123";
        String currentPlain = "oldPass";
        String currentEncoded = "encodedOldPass";
        String newPlain = "newPass";

        when(userRepository.getPassword(userUuid)).thenReturn(currentEncoded);
        when(encoder.matches(currentPlain, currentEncoded)).thenReturn(true);
        when(encoder.encode(newPlain)).thenReturn("encodedNewPass");

        // WHEN
        userService.updatePassword(userUuid, currentPlain, newPlain, newPlain);

        // THEN
        verify(userRepository).updatePassword(eq(userUuid), anyString());
    }

    @Test
    @DisplayName("userExistsByUuid doit retourner false en cas d'ApiException")
    void userExistsByUuid_ShouldReturnFalse_WhenNotFound() {
        // GIVEN
        when(userRepository.getUserByUuid("invalid-uuid")).thenThrow(new ApiException("Not found"));

        // WHEN
        boolean exists = userService.userExistsByUuid("invalid-uuid");

        // THEN
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("getUserByEmail doit retourner l'utilisateur correspondant à l'email")
    void getUserByEmail_ShouldReturnUser_WhenEmailExists() {
        // GIVEN
        String email = "test@example.com";
        User mockUser = new User();
        mockUser.setEmail(email);
        mockUser.setFirstName("John");

        when(userRepository.getUserByEmail(email)).thenReturn(mockUser);

        // WHEN
        User result = userService.getUserByEmail(email);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        verify(userRepository, times(1)).getUserByEmail(email);
    }

    @Test
    @DisplayName("toggleAccountEnabled doit appeler le repository")
    void toggleAccountEnabled_ShouldInvokeRepository() {
        // GIVEN
        String uuid = "uuid-123";
        when(userRepository.toggleAccountEnabled(uuid)).thenReturn(new User());

        // WHEN
        userService.toggleAccountEnabled(uuid);

        // THEN
        verify(userRepository).toggleAccountEnabled(uuid);
    }

    @Test
    @DisplayName("updateUser doit appeler le repository et retourner l'utilisateur mis à jour")
    void updateUser_ShouldReturnUpdatedUser() {
        // GIVEN
        String uuid = "uuid-123";
        String firstName = "John";
        String lastName = "Doe";
        String email = "john@example.com";
        String phone = "0102030405";
        String bio = "Développeur Java";
        String address = "123 Rue de Paris";

        User updatedUser = new User();
        updatedUser.setUserUuid(uuid);
        updatedUser.setFirstName(firstName);

        when(userRepository.updateUser(uuid, firstName, lastName, email, phone, bio, address))
                .thenReturn(updatedUser);

        // WHEN
        User result = userService.updateUser(uuid, firstName, lastName, email, phone, bio, address);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("John");
        verify(userRepository, times(1)).updateUser(uuid, firstName, lastName, email, phone, bio, address);
    }

    @Test
    @DisplayName("verifyAccount doit lever une exception si le token n'existe pas")
    void verifyAccount_ShouldThrowException_WhenTokenNotFound() {
        // GIVEN
        String token = "unknown-token";
        when(userRepository.getAccountToken(token)).thenReturn(null);

        // WHEN & THEN
        assertThatThrownBy(() -> userService.verifyAccount(token))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Lien invalide");

        verify(userRepository, never()).updateAccountSettings(any());
    }

    @Test
    @DisplayName("verifyAccount doit supprimer le token et lever une exception s'il est expiré")
    void verifyAccount_ShouldDeleteTokenAndThrow_WhenTokenExpired() {
        // GIVEN
        String token = "expired-token";
        // On simule un token expiré (on suppose qu'il y a un mock ou un objet réel AccountToken)
        var mockToken = mock(AccountToken.class);
        when(mockToken.isExpired()).thenReturn(true);
        when(userRepository.getAccountToken(token)).thenReturn(mockToken);

        // WHEN & THEN
        assertThatThrownBy(() -> userService.verifyAccount(token))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Ce lien a expiré");

        verify(userRepository).deleteAccountToken(token);
        verify(userRepository, never()).updateAccountSettings(any());
    }

    @Test
    @DisplayName("verifyAccount doit activer le compte et supprimer le token en cas de succès")
    void verifyAccount_ShouldVerifyAndCleanup_WhenTokenIsValid() {
        // GIVEN
        String token = "valid-token";
        Long userId = 100L;
        var mockToken = mock(AccountToken.class);
        when(mockToken.isExpired()).thenReturn(false);
        when(mockToken.getUserId()).thenReturn(userId);
        when(userRepository.getAccountToken(token)).thenReturn(mockToken);

        // WHEN
        userService.verifyAccount(token);

        // THEN
        verify(userRepository).updateAccountSettings(userId);
        verify(userRepository).deleteAccountToken(token);
    }

    @Test
    @DisplayName("verifyPasswordToken doit lever une exception si le token est introuvable")
    void verifyPasswordToken_ShouldThrowException_WhenTokenNotFound() {
        when(userRepository.getPasswordToken("invalid")).thenReturn(null);

        assertThatThrownBy(() -> userService.verifyPasswordToken("invalid"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Lien invalide");
    }

    @Test
    @DisplayName("verifyPasswordToken doit supprimer le token et lever une exception si expiré")
    void verifyPasswordToken_ShouldDeleteAndThrow_WhenExpired() {
        var mockToken = mock(PasswordToken.class);
        when(mockToken.isExpired()).thenReturn(true);
        when(userRepository.getPasswordToken("expired")).thenReturn(mockToken);

        assertThatThrownBy(() -> userService.verifyPasswordToken("expired"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Le lien a expiré");

        verify(userRepository).deletePasswordToken("expired");
    }

    @Test
    @DisplayName("verifyPasswordToken doit retourner l'utilisateur si le token est valide")
    void verifyPasswordToken_ShouldReturnUser_WhenValid() {
        var mockToken = mock(PasswordToken.class);
        User mockUser = new User();
        mockUser.setUserId(1L);

        when(mockToken.isExpired()).thenReturn(false);
        when(mockToken.getUserId()).thenReturn(1L);
        when(userRepository.getPasswordToken("valid")).thenReturn(mockToken);
        when(userRepository.getUserById(1L)).thenReturn(mockUser);

        User result = userService.verifyPasswordToken("valid");

        assertThat(result).isEqualTo(mockUser);
    }

    @Test
    @DisplayName("enableMfa doit appeler le repository")
    void enableMfa_ShouldInvokeRepository() {
        userService.enableMfa("uuid");
        verify(userRepository).enableMfa("uuid");
    }

    @Test
    @DisplayName("disableMfa doit appeler le repository")
    void disableMfa_ShouldInvokeRepository() {
        userService.disableMfa("uuid");
        verify(userRepository).disableMfa("uuid");
    }

    @Test
    @DisplayName("uploadPhoto doit mettre à jour l'image et ajouter un timestamp")
    void uploadPhoto_ShouldUpdateUrlAndReturnUser() throws IOException {
        try (MockedStatic<ServletUriComponentsBuilder> mockedBuilder = mockStatic(ServletUriComponentsBuilder.class)) {
            var mockUriBuilder = mock(ServletUriComponentsBuilder.class);
            mockedBuilder.when(ServletUriComponentsBuilder::fromCurrentContextPath).thenReturn(mockUriBuilder);
            when(mockUriBuilder.path(anyString())).thenReturn(mockUriBuilder);
            when(mockUriBuilder.toUriString()).thenReturn("http://localhost/images/photo.png");

            String uuid = "uuid-123";
            MultipartFile mockFile = mock(MultipartFile.class);
            User mockUser = new User();
            mockUser.setImageUrl("old-url/image.png");

            when(userRepository.getUserByUuid(uuid)).thenReturn(mockUser);
            when(mockFile.getOriginalFilename()).thenReturn("new-photo.png");
            when(mockFile.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));

            User result = userService.uploadPhoto(uuid, mockFile);

            assertThat(result.getImageUrl()).contains("?timestamp=");
            verify(userRepository).updateImageUrl(eq(uuid), anyString());
        }
    }

    @Test
    @DisplayName("toggleAccountExpired doit appeler le repository et retourner l'utilisateur")
    void toggleAccountExpired_ShouldInvokeRepository() {
        // GIVEN
        String uuid = "uuid-123";
        User mockUser = new User();
        when(userRepository.toggleAccountExpired(uuid)).thenReturn(mockUser);

        // WHEN
        User result = userService.toggleAccountExpired(uuid);

        // THEN
        assertThat(result).isEqualTo(mockUser);
        verify(userRepository).toggleAccountExpired(uuid);
    }

    @Test
    @DisplayName("toggleAccountLocked doit appeler le repository et retourner l'utilisateur")
    void toggleAccountLocked_ShouldInvokeRepository() {
        // GIVEN
        String uuid = "uuid-123";
        User mockUser = new User();
        when(userRepository.toggleAccountLocked(uuid)).thenReturn(mockUser);

        // WHEN
        User result = userService.toggleAccountLocked(uuid);

        // THEN
        assertThat(result).isEqualTo(mockUser);
        verify(userRepository).toggleAccountLocked(uuid);
    }

    @Test
    @DisplayName("toggleCredentialsExpired doit retourner null (comportement actuel)")
    void toggleCredentialsExpired_ShouldReturnNull() {
        // WHEN
        User result = userService.toggleCredentialsExpired("uuid-123");

        // THEN
        assertThat(result).isNull();
        // On vérifie qu'on n'appelle rien sur le repository puisque le code fait "return null"
        verifyNoInteractions(userRepository);
    }



    @Test
    @DisplayName("updatePassword doit lever une exception si la confirmation est différente du nouveau mot de passe")
    void updatePassword_ShouldThrowException_WhenConfirmationDoesNotMatch() {
        // WHEN & THEN
        assertThatThrownBy(() ->
                userService.updatePassword("uuid", "oldPass", "newPass", "differentPass")
        )
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Les mots de passe ne correspondent pas");

        verifyNoInteractions(encoder);
        verify(userRepository, never()).updatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("updatePassword doit lever une exception si le mot de passe actuel est incorrect")
    void updatePassword_ShouldThrowException_WhenCurrentPasswordIsWrong() {
        // GIVEN
        String uuid = "uuid-123";
        String currentRaw = "wrong-old-pass";
        String currentEncoded = "encoded-old-pass";

        when(userRepository.getPassword(uuid)).thenReturn(currentEncoded);
        // On simule que BCrypt renvoie false
        when(encoder.matches(currentRaw, currentEncoded)).thenReturn(false);

        // WHEN & THEN
        assertThatThrownBy(() ->
                userService.updatePassword(uuid, currentRaw, "newPass", "newPass")
        )
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Le mot de passe actuel est incorrect");

        verify(userRepository, never()).updatePassword(eq(uuid), anyString());
    }

    @Test
    @DisplayName("updatePassword doit encoder et sauvegarder le mot de passe si tout est valide")
    void updatePassword_ShouldSuccess_WhenAllInputsAreValid() {
        // GIVEN
        String uuid = "uuid-123";
        String currentRaw = "correct-old-pass";
        String currentEncoded = "encoded-old-pass";
        String newRaw = "brand-new-pass";
        String newEncoded = "encoded-new-pass";

        when(userRepository.getPassword(uuid)).thenReturn(currentEncoded);
        when(encoder.matches(currentRaw, currentEncoded)).thenReturn(true);
        when(encoder.encode(newRaw)).thenReturn(newEncoded);

        // WHEN
        userService.updatePassword(uuid, currentRaw, newRaw, newRaw);

        // THEN
        verify(encoder).encode(newRaw);
        verify(userRepository).updatePassword(uuid, newEncoded);
    }

    @Test
    @DisplayName("updateRole doit mettre à jour le rôle de l'utilisateur et retourner l'objet mis à jour")
    void updateRole_ShouldInvokeRepositoryAndReturnUser() {
        // GIVEN
        String uuid = "uuid-123";
        String newRole = "ROLE_ADMIN";
        User mockUser = new User();
        mockUser.setUserUuid(uuid);
        mockUser.setRole(newRole);

        when(userRepository.updateRole(uuid, newRole)).thenReturn(mockUser);

        // WHEN
        User result = userService.updateRole(uuid, newRole);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(newRole);
        verify(userRepository, times(1)).updateRole(uuid, newRole);
    }

    @Test
    @DisplayName("resetPassword doit créer un nouveau token si aucun n'existe")
    void resetPassword_ShouldCreateNewToken_WhenNoneExists() {
        // GIVEN
        String email = "kara@example.com";
        User mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setFirstName("kara");

        when(userRepository.getUserByEmail(email)).thenReturn(mockUser);
        when(userRepository.getPasswordToken(1L)).thenReturn(null);
        when(userRepository.createPasswordToken(1L)).thenReturn("new-token-123");

        // WHEN
        userService.resetPassword(email);

        // THEN
        verify(userRepository).createPasswordToken(1L);
        verify(publisher).publishEvent(any(Event.class));
    }

    @Test
    @DisplayName("resetPassword doit supprimer l'ancien token s'il est expiré et en créer un nouveau")
    void resetPassword_ShouldReplaceToken_WhenExpired() {
        // GIVEN
        String email = "john@example.com";
        User mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setFirstName("John");

        var expiredToken = mock(PasswordToken.class);
        when(expiredToken.isExpired()).thenReturn(true);

        when(userRepository.getUserByEmail(email)).thenReturn(mockUser);
        when(userRepository.getPasswordToken(1L)).thenReturn(expiredToken);
        when(userRepository.createPasswordToken(1L)).thenReturn("refreshed-token");

        // WHEN
        userService.resetPassword(email);

        // THEN
        verify(userRepository).deletePasswordToken(1L);
        verify(userRepository).createPasswordToken(1L);
        verify(publisher).publishEvent(any(Event.class));
    }

    @Test
    @DisplayName("resetPassword doit réutiliser le token existant s'il est encore valide")
    void resetPassword_ShouldReuseToken_WhenStillValid() {
        // GIVEN
        String email = "john@example.com";
        Long userId = 1L;
        String existingToken = "current-valid-token";

        User mockUser = new User();
        mockUser.setUserId(userId);
        mockUser.setFirstName("John");

        // On mock un token qui n'est PAS expiré
        var validToken = mock(PasswordToken.class);
        when(validToken.isExpired()).thenReturn(false);
        when(validToken.getToken()).thenReturn(existingToken);

        when(userRepository.getUserByEmail(email)).thenReturn(mockUser);
        when(userRepository.getPasswordToken(userId)).thenReturn(validToken);

        // Initialisation du capteur pour vérifier l'événement
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // WHEN
        userService.resetPassword(email);

        // THEN
        // 1. On vérifie qu'on ne crée PAS de nouveau token (important pour la couverture)
        verify(userRepository, never()).createPasswordToken(anyLong());
        verify(userRepository, never()).deletePasswordToken(anyLong());

        // 2. On vérifie que l'événement a été publié
        verify(publisher).publishEvent(eventCaptor.capture());

        // 3. On inspecte les données de l'événement capturé
        Event sentEvent = eventCaptor.getValue();

        // Vérification du type d'événement
        assertThat(sentEvent.getEventType()).isEqualTo(EventType.PASSWORD_RESET);

        // Vérification de la Map 'data' (et non 'values')
        assertThat(sentEvent.getData().get("token")).isEqualTo(existingToken);
        assertThat(sentEvent.getData().get("email")).isEqualTo(email);
        assertThat(sentEvent.getData().get("name")).isEqualTo("John");
    }

    @Test
    @DisplayName("doResetPassword doit lever une exception si les mots de passe ne correspondent pas")
    void doResetPassword_ShouldThrowException_WhenPasswordsDoNotMatch() {
        // WHEN & THEN
        assertThatThrownBy(() ->
                userService.doResetPassword("uuid", "token", "newPass", "wrongConfirm")
        )
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Les mots de passe ne correspondent pas");
    }

    @Test
    @DisplayName("doResetPassword doit lever une exception si le token n'appartient pas à l'utilisateur")
    void doResetPassword_ShouldThrowException_WhenTokenDoesNotMatchUser() {
        // GIVEN
        String uuid = "user-uuid";
        String token = "valid-token";

        User mockUser = new User();
        mockUser.setUserId(1L);

        var mockToken = mock(PasswordToken.class);
        when(mockToken.getUserId()).thenReturn(2L); // ID différent !

        when(userRepository.getUserByUuid(uuid)).thenReturn(mockUser);
        when(userRepository.getPasswordToken(token)).thenReturn(mockToken);

        // WHEN & THEN
        assertThatThrownBy(() ->
                userService.doResetPassword(uuid, token, "pass", "pass")
        )
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Lien invalide");
    }

    @Test
    @DisplayName("doResetPassword doit mettre à jour le mot de passe et supprimer le token en cas de succès")
    void doResetPassword_ShouldSuccess_WhenInputsAreValid() {
        // GIVEN
        String uuid = "user-uuid";
        String token = "valid-token";
        String newPass = "newPass123";
        String encodedPass = "encodedPass123";

        User mockUser = new User();
        mockUser.setUserId(1L);

        var mockToken = mock(PasswordToken.class);
        when(mockToken.getUserId()).thenReturn(1L);

        when(userRepository.getUserByUuid(uuid)).thenReturn(mockUser);
        when(userRepository.getPasswordToken(token)).thenReturn(mockToken);
        when(encoder.encode(newPass)).thenReturn(encodedPass);

        // WHEN
        userService.doResetPassword(uuid, token, newPass, newPass);

        // THEN
        verify(userRepository).updatePassword(uuid, encodedPass);
        verify(userRepository).deletePasswordToken(1L);
    }

    @Test
    @DisplayName("getUsers doit retourner la liste complète des utilisateurs")
    void getUsers_ShouldReturnList() {
        // GIVEN
        when(userRepository.getUsers()).thenReturn(List.of(new User(), new User()));

        // WHEN
        List<User> result = userService.getUsers();

        // THEN
        assertThat(result).hasSize(2);
        verify(userRepository).getUsers();
    }

    @Test
    @DisplayName("getCredential doit retourner les identifiants de l'utilisateur")
    void getCredential_ShouldInvokeRepository() {
        // GIVEN
        String uuid = "uuid";
        when(userRepository.getCredential(uuid)).thenReturn(new Credential());

        // WHEN
        userService.getCredential(uuid);

        // THEN
        verify(userRepository).getCredential(uuid);
    }

    @Test
    @DisplayName("getMediLaboSupports doit retourner la liste des supports")
    void getMediLaboSupports_ShouldInvokeRepository() {
        // WHEN
        userService.getMediLaboSupports();

        // THEN
        verify(userRepository).getMediLaboSupports();
    }

    @Test
    @DisplayName("getAssignee doit retourner l'utilisateur assigné au ticket")
    void getAssignee_ShouldInvokeRepository() {
        // GIVEN
        String ticketUuid = "ticket-789";
        User mockUser = new User();
        when(userRepository.getAssignee(ticketUuid)).thenReturn(mockUser);

        // WHEN
        User result = userService.getAssignee(ticketUuid);

        // THEN
        assertThat(result).isEqualTo(mockUser);
        verify(userRepository).getAssignee(ticketUuid);
    }

    @Test
    @DisplayName("getDevices doit retourner la liste des appareils de l'utilisateur")
    void getDevices_ShouldReturnDeviceList() {
        // GIVEN
        String userUuid = "user-uuid-123";
        List<Device> mockDevices =
                List.of(new Device());

        when(userRepository.getDevices(userUuid)).thenReturn(mockDevices);

        // WHEN
        var result = userService.getDevices(userUuid);

        // THEN
        assertThat(result).hasSize(1);
        verify(userRepository).getDevices(userUuid);
    }

    @Test
    @DisplayName("getPatientUser doit retourner l'utilisateur associé au patient")
    void getPatientUser_ShouldInvokeRepository() {
        // GIVEN
        String patientUuid = "patient-xyz";
        User mockUser = new User();
        when(userRepository.getPatientUser(patientUuid)).thenReturn(mockUser);

        // WHEN
        User result = userService.getPatientUser(patientUuid);

        // THEN
        assertThat(result).isEqualTo(mockUser);
        verify(userRepository).getPatientUser(patientUuid);
    }

    @Test
    void uploadPhoto_ShouldThrowApiException_WhenFileOperationFails() throws IOException {
        String uuid = "uuid-123";
        MultipartFile mockFile = mock(MultipartFile.class);
        User mockUser = new User();
        mockUser.setImageUrl("http://localhost/images/old.png");

        when(userRepository.getUserByUuid(uuid)).thenReturn(mockUser);
        when(mockFile.getOriginalFilename()).thenReturn("photo.png");
        when(mockFile.getInputStream()).thenThrow(new IOException("Disk Full"));

        assertThatThrownBy(() -> userService.uploadPhoto(uuid, mockFile))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Impossible de sauvegardé l'image");
    }

    @Test
    @DisplayName("uploadPhoto doit créer le répertoire s'il n'existe pas")
    void uploadPhoto_ShouldCreateDirectory_WhenNotExists() throws IOException {
        // Sous-dossier qui n'existe pas encore
        Path subDir = tempDir.resolve("new-upload-dir");
        ReflectionTestUtils.setField(userService, "photoDirectory", subDir.toString() + "/");

        try (MockedStatic<ServletUriComponentsBuilder> uriMock = mockStatic(ServletUriComponentsBuilder.class)) {
            setupUriMock(uriMock);

            User user = new User();
            user.setImageUrl("http://api.com/img.png");
            when(userRepository.getUserByUuid(anyString())).thenReturn(user);

            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("test.png");
            when(file.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));

            userService.uploadPhoto("uuid", file);

            assertThat(Files.exists(subDir)).isTrue();
        }
    }

    @Test
    @DisplayName("uploadPhoto doit supprimer l'ancienne image si elle existe")
    void uploadPhoto_ShouldDeleteOldImage_WhenExists() throws IOException {
        // Créer une vraie ancienne image dans tempDir
        Path oldImage = tempDir.resolve("old-image.png");
        Files.write(oldImage, "old-content".getBytes());

        try (MockedStatic<ServletUriComponentsBuilder> uriMock = mockStatic(ServletUriComponentsBuilder.class)) {
            setupUriMock(uriMock);

            User user = new User();
            user.setImageUrl("http://api.com/old-image.png"); // dernier segment = "old-image.png"
            when(userRepository.getUserByUuid(anyString())).thenReturn(user);

            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("new.png");
            when(file.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));

            userService.uploadPhoto("uuid", file);

            assertThat(Files.exists(oldImage)).isFalse();
        }
    }

    // Petite méthode utilitaire pour alléger les tests
    private void setupUriMock(MockedStatic<ServletUriComponentsBuilder> uriMock) {
        ServletUriComponentsBuilder builder = mock(ServletUriComponentsBuilder.class);
        uriMock.when(ServletUriComponentsBuilder::fromCurrentContextPath).thenReturn(builder);
        when(builder.path(anyString())).thenReturn(builder);
        when(builder.toUriString()).thenReturn("http://api.com/new.png");
    }

    @Test
    @DisplayName("userExistsByUuid doit retourner true si l'utilisateur est trouvé")
    void userExistsByUuid_ShouldReturnTrue_WhenUserExists() {
        // GIVEN
        String uuid = "exist-uuid";
        when(userRepository.getUserByUuid(uuid)).thenReturn(new User());

        // WHEN
        boolean exists = userService.userExistsByUuid(uuid);

        // THEN
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("userExistsByUuid doit retourner false si le repository lève une ApiException")
    void userExistsByUuid_ShouldReturnFalse_WhenUserNotFound() {
        // GIVEN
        String uuid = "not-found-uuid";
        // On simule le comportement du repository qui ne trouve rien
        when(userRepository.getUserByUuid(uuid)).thenThrow(new ApiException("User not found"));

        // WHEN
        boolean exists = userService.userExistsByUuid(uuid);

        // THEN
        assertThat(exists).isFalse();
    }

}
package com.openclassrooms.authorizationserverservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.authorizationserverservice.dtorequest.*;
import com.openclassrooms.authorizationserverservice.model.Credential;
import com.openclassrooms.authorizationserverservice.model.Device;
import com.openclassrooms.authorizationserverservice.model.User;
import com.openclassrooms.authorizationserverservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository; // AJOUT
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.openclassrooms.authorizationserverservice.dtorequest.UserRequest;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@WebMvcTest(UserResourceController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // MOCKS POUR RÉSOUDRE L'ERREUR DE CONTEXTE
    @MockitoBean
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /register - Succès")
    void register_ShouldReturnCreated() throws Exception {
        UserRequest userRequest = new UserRequest();
        userRequest.setFirstName("John");
        userRequest.setLastName("Doe");
        userRequest.setEmail("jhon@test.com");
        userRequest.setUsername("John");
        userRequest.setPassword("password123");

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(containsString("Compte créé avec succès")));

        verify(userService).createUser(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /verify/account - Succès")
    void verifyAccount_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/user/verify/account")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Compte vérifié")));

        verify(userService).verifyAccount("valid-token");
    }

    @Test
    @DisplayName("PATCH /mfa/enable - Succès")
    void enableMfa_ShouldReturnOk() throws Exception {
        // Utilise un format UUID standard au cas où il y aurait une validation Regex
        String uuid = "123e4567-e89b-12d3-a456-426614174000";

        User mockUserEntity = new User();
        mockUserEntity.setUserUuid(uuid);
        mockUserEntity.setEmail("test@test.com");

        when(userService.enableMfa(anyString())).thenReturn(mockUserEntity);

        mockMvc.perform(patch("/user/mfa/enable")
                        .principal(new UsernamePasswordAuthenticationToken("123e4567-e89b-12d3-a456-426614174000", null))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /mfa/disable - Succès")
    void disableMfa_ShouldReturnOk() throws Exception {
        // 1. Préparation des données
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        User mockUserEntity = new User();
        mockUserEntity.setUserUuid(uuid);
        mockUserEntity.setMfa(false); // On simule la désactivation

        // 2. Mock du service
        // On s'assure que le service retourne bien l'utilisateur désactivé
        when(userService.disableMfa(eq(uuid))).thenReturn(mockUserEntity);

        // 3. Exécution et vérification
        mockMvc.perform(patch("/user/mfa/disable")
                        .principal(new UsernamePasswordAuthenticationToken(uuid, null))
                        .with(csrf()))
                .andExpect(status().isOk())
                // On vérifie que le message de succès est bien présent dans la réponse
                .andExpect(jsonPath("$.message").value("L'authentification 2FA a été désactivée avec succès"))
                .andExpect(jsonPath("$.data.user.userUuid").value(uuid))
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    @DisplayName("GET /profile - Succès")
    void getProfile_ShouldReturnUserAndDevices() throws Exception {
        String uuid = "123e4567-e89b-12d3-a456-426614174000";

        // Mock User
        User mockUser = new User();
        mockUser.setUserUuid(uuid);
        mockUser.setEmail("kara@test.com");

        // Mock Devices avec setters
        Device d = new Device();
        // Ajuste les noms des méthodes Device réelle
        d.setDevice("MacBook Pro");
        List<Device> mockDevices = List.of(d);

        when(userService.getUserByUuid(uuid)).thenReturn(mockUser);
        when(userService.getDevices(uuid)).thenReturn(mockDevices);

        mockMvc.perform(get("/user/profile")
                        .principal(new UsernamePasswordAuthenticationToken(uuid, null))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil récupéré"))
                .andExpect(jsonPath("$.data.user.userUuid").value(uuid))
                .andExpect(jsonPath("$.data.devices[0]").exists());
    }

    @Test
    @DisplayName("GET /{userUuid} - Succès")
    void getUserByUuid_ShouldReturnUser() throws Exception {
        String uuid = "123e4567-e89b-12d3-a456-426614174000";

        User mockUser = new User();
        mockUser.setUserUuid(uuid);
        mockUser.setEmail("search@test.com");

        // On mocke l'appel au service avec l'UUID passé en paramètre
        when(userService.getUserByUuid(uuid)).thenReturn(mockUser);

        mockMvc.perform(get("/user/{userUuid}", uuid) // On passe l'UUID dans l'URL
                        .principal(new UsernamePasswordAuthenticationToken(uuid, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil récupéré"))
                .andExpect(jsonPath("$.data.user.userUuid").value(uuid));
    }

    @Test
    @DisplayName("GET /assignee/{patientUuid} - Succès")
    void getAssigneeByUuid_ShouldReturnAssignee() throws Exception {
        String patientUuid = "patient-uuid-999";
        String userUuid = "user-uuid-111";

        User assignee = new User();
        assignee.setUserUuid(userUuid);
        assignee.setFirstName("Jean");

        // Le service doit retourner l'utilisateur assigné au patient
        when(userService.getAssignee(patientUuid)).thenReturn(assignee);

        mockMvc.perform(get("/user/assignee/{patientUuid}", patientUuid)
                        .principal(new UsernamePasswordAuthenticationToken(userUuid, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil récupéré"))
                .andExpect(jsonPath("$.data.user.firstName").value("Jean"));
    }

    @Test
    @DisplayName("GET /patient/{patientUuid} - Succès")
    void getPatientUser_ShouldReturnUser() throws Exception {
        String patientUuid = "p-123";
        User mockUser = new User();
        mockUser.setEmail("patient@test.com");

        when(userService.getPatientUser(patientUuid)).thenReturn(mockUser);

        mockMvc.perform(get("/user/patient/{patientUuid}", patientUuid)
                        .principal(new UsernamePasswordAuthenticationToken("me", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("patient@test.com"));
    }

    @Test
    @DisplayName("GET /medilabosupports - Succès")
    void getMediLaboSupports_ShouldReturnList() throws Exception {
        User support1 = new User();
        support1.setFirstName("Alice");
        User support2 = new User();
        support2.setFirstName("Bob");

        // On mocke le retour d'une liste
        when(userService.getMediLaboSupports()).thenReturn(List.of(support1, support2));

        mockMvc.perform(get("/user/medilabosupports")
                        .principal(new UsernamePasswordAuthenticationToken("me", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil récupéré"))
                // On vérifie que c'est bien une liste dans la map "data"
                .andExpect(jsonPath("$.data.medilaboSupports").isArray())
                .andExpect(jsonPath("$.data.medilaboSupports[0].firstName").value("Alice"))
                .andExpect(jsonPath("$.data.medilaboSupports[1].firstName").value("Bob"));
    }

    @Test
    @DisplayName("GET /user/{email} - Succès")
    void getUserByEmail_ShouldReturnUser() throws Exception {
        String email = "kara@example.com";
        User mockUser = new User();
        mockUser.setEmail(email);

        when(userService.getUserByEmail(email)).thenReturn(mockUser);

        mockMvc.perform(get("/user/user/{email}", email)
                        .principal(new UsernamePasswordAuthenticationToken("me", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value(email));
    }

    @Test
    @DisplayName("GET /credential/{userUuid} - Succès")
    void getCredential_ShouldReturnCredential() throws Exception {
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        Credential mockCred = new Credential(); // Ou utilise ton entité réelle

        when(userService.getCredential(uuid)).thenReturn(mockCred);

        mockMvc.perform(get("/user/credential/{userUuid}", uuid)
                        .principal(new UsernamePasswordAuthenticationToken(uuid, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.credential").exists());
    }

    @Test
    @DisplayName("PATCH /update - Succès")
    void updateUser_ShouldReturnUpdatedUser() throws Exception {
        String uuid = "123e4567-e89b-12d3-a456-426614174000";

        // 1. Préparation de la requête avec seulement quelques champs
        UserRequest request = new UserRequest();
        request.setFirstName("John");
        request.setEmail("john@test.com");
        // Les autres champs (lastName, phone, bio, address) restent NULL

        User updatedUser = new User();
        updatedUser.setUserUuid(uuid);
        updatedUser.setFirstName("John");

        // 2. Mock du service : Utilise nullable String.class
        // pour les champs qui peuvent être nuls dans la requête.
        when(userService.updateUser(
                eq(uuid),           // ID utilisateur (non nul)
                anyString(),        // firstName
                nullable(String.class), // lastName (peut être nul)
                anyString(),        // email
                nullable(String.class), // phone (peut être nul)
                nullable(String.class), // bio (peut être nul)
                nullable(String.class)  // address (peut être nul)
        )).thenReturn(updatedUser);

        // 3. Exécution
        mockMvc.perform(patch("/user/update")
                        .with(csrf())
                        .principal(new UsernamePasswordAuthenticationToken(uuid, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utilisateur mis à jour avec succès"))
                .andExpect(jsonPath("$.data.user.firstName").value("John"));
    }

    @Test
    @DisplayName("PATCH /updaterole - Succès avec ADMIN")
    void updateRole_ShouldWorkForAdmin() throws Exception {
        String uuid = "admin-uuid";
        RoleRequest roleReq = new RoleRequest();
        roleReq.setRole("MANAGER");

        User updatedUser = new User();

        when(userService.updateRole(eq(uuid), eq("MANAGER"))).thenReturn(updatedUser);

        mockMvc.perform(patch("/user/updaterole")
                        .with(csrf())
                        // On simule un utilisateur avec l'autorité ADMIN
                        .principal(new UsernamePasswordAuthenticationToken(uuid, null, List.of(new SimpleGrantedAuthority("ADMIN"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(roleReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /toggleaccountexpired - Succès")
    void toggleAccountExpired_ShouldReturnOk() throws Exception {
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        User mockUser = new User();

        when(userService.toggleAccountExpired(uuid)).thenReturn(mockUser);

        mockMvc.perform(patch("/user/toggleaccountexpired")
                        .with(csrf())
                        .principal(new UsernamePasswordAuthenticationToken(uuid, null, List.of(new SimpleGrantedAuthority("ADMIN")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utilisateur mis à jour avec succès"));
    }

    @Test
    @DisplayName("PATCH /toggleaccountlocked - Succès avec MANAGER")
    void toggleAccountLocked_ShouldReturnOk() throws Exception {
        String uuid = "user-uuid";
        User mockUser = new User();
        mockUser.setAccountNonLocked(false);

        when(userService.toggleAccountLocked(uuid)).thenReturn(mockUser);

        mockMvc.perform(patch("/user/toggleaccountlocked")
                        .with(csrf())
                        // On simule un rôle autorisé
                        .principal(new UsernamePasswordAuthenticationToken(uuid, null,
                                List.of(new SimpleGrantedAuthority("MANAGER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utilisateur mis à jour avec succès"));
    }

    @Test
    @DisplayName("PATCH /toggleaccountenabled - Succès avec ADMIN")
    void toggleAccountEnabled_ShouldReturnOk() throws Exception {
        String uuid = "user-uuid";
        User mockUser = new User();
        mockUser.setEnabled(true);

        when(userService.toggleAccountEnabled(uuid)).thenReturn(mockUser);

        mockMvc.perform(patch("/user/toggleaccountenabled")
                        .with(csrf())
                        .principal(new UsernamePasswordAuthenticationToken(uuid, null,
                                List.of(new SimpleGrantedAuthority("ADMIN")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utilisateur mis à jour avec succès"));
    }

    @Test
    @DisplayName("PATCH /updatepassword - Succès")
    void updatePassword_ShouldReturnOk() throws Exception {
        String uuid = "user-uuid";
        PasswordRequest req = new PasswordRequest();
        req.setCurrentPassword("oldPass123!");
        req.setNewPassword("newPass456!");
        req.setConfirmNewPassword("newPass456!");

        // La méthode est void, donc on utilise doNothing()
        doNothing().when(userService).updatePassword(eq(uuid), anyString(), anyString(), anyString());

        mockMvc.perform(patch("/user/updatepassword")
                        .with(csrf())
                        .principal(new UsernamePasswordAuthenticationToken(uuid, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mot de passe mis à jour avec succès"));
    }

    @Test
    @DisplayName("POST /resetpassword - Succès")
    void resetPassword_ShouldReturnOk() throws Exception {
        String email = "test@example.com";

        doNothing().when(userService).resetPassword(email);

        mockMvc.perform(post("/user/resetpassword")
                        .with(csrf())
                        .param("email", email) // Utilisation de .param() pour @RequestParam
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Nous vous avons envoyé un email pour réinitialiser votre mot de passe."));
    }

    @Test
    @DisplayName("GET /verify/password - Succès")
    void verifyPassword_ShouldReturnUser() throws Exception {
        String token = "secret-token-123";
        User mockUser = new User();
        mockUser.setEmail("kara@test.com");

        when(userService.verifyPasswordToken(token)).thenReturn(mockUser);

        mockMvc.perform(get("/user/verify/password")
                        .param("token", token)) // @RequestParam
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Saisissez votre nouveau mot de passe"))
                .andExpect(jsonPath("$.data.user.email").value("kara@test.com"));
    }

    @Test
    @DisplayName("POST /resetpassword/reset - Succès")
    void doResetPassword_ShouldReturnOk() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setUserUuid("uuid-123");
        req.setToken("token-123");
        req.setPassword("NewPass123!");
        req.setConfirmPassword("NewPass123!");

        doNothing().when(userService).doResetPassword(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/user/resetpassword/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("réinitialisé avec succès")));
    }

    @Test
    @DisplayName("GET /list - Succès pour ADMIN")
    void getUsers_ShouldReturnListForAdmin() throws Exception {
        when(userService.getUsers()).thenReturn(List.of(new User(), new User()));

        mockMvc.perform(get("/user/list")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null,
                                List.of(new SimpleGrantedAuthority("ADMIN")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.users").isArray());
    }

    @Test
    @DisplayName("PATCH /photo - Succès Upload")
    void uploadPhoto_ShouldReturnUser() throws Exception {
        String uuid = "user-uuid";
        // Création d'un faux fichier image
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                "content".getBytes()
        );

        User mockUser = new User();
        mockUser.setUserUuid(uuid);

        when(userService.uploadPhoto(eq(uuid), any(MultipartFile.class))).thenReturn(mockUser);

        mockMvc.perform(multipart("/user/photo")
                        .file(file)
                        .with(csrf())
                        .with(request -> { request.setMethod("PATCH"); return request; }) // Transforme POST en PATCH
                        .principal(new UsernamePasswordAuthenticationToken(uuid, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Photo mise à jour avec succès"));
    }

    @Test
    @DisplayName("GET /image/{filename} - Succès")
    void getPhoto_ShouldReturnByteArray() throws Exception {
        String filename = "test-image.png";
        byte[] mockImageContent = "fake-image-content".getBytes();

        // 1. Définir le chemin COMPLET tel qu'attendu par ton code
        Path directoryPath = Paths.get("/Users/kara/Downloads/uploads/");
        Path filePath = directoryPath.resolve(filename);

        // 2. Créer physiquement le dossier et le fichier pour le test
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }
        Files.write(filePath, mockImageContent);

        try {
            // 3. Exécuter la requête
            mockMvc.perform(get("/user/image/{filename}", filename))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
                    .andExpect(content().bytes(mockImageContent));

        } finally {
            // 4. Nettoyage : on supprime le fichier de test pour ne pas polluer ton dossier Downloads
            Files.deleteIfExists(filePath);
        }
    }

}
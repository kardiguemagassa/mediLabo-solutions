package com.openclassrooms.authorizationserverservice.controller;

import com.openclassrooms.authorizationserverservice.exception.ApiException;
import com.openclassrooms.authorizationserverservice.model.User;
import com.openclassrooms.authorizationserverservice.security.MfaAuthentication;
import com.openclassrooms.authorizationserverservice.service.UserService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoginController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @MockitoBean
    private RegisteredClientRepository registeredClientRepository;

    @MockitoBean
    private AuthorizationServerSettings authorizationServerSettings;

    @Autowired
    private WebApplicationContext context;

    private User mockUser;

    @Test
    @DisplayName("GET /login - Devrait retourner la vue login")
    void login_ShouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/login").with(csrf())).andExpect(status().isOk()).andExpect(view().name("login"));
    }

    @Test
    @DisplayName("GET /error - Redirection login sur ApiException")
    void handleError_WithApiException() throws Exception {
        mockMvc.perform(get("/error")
                        .with(user("admin").roles("USER"))
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new ApiException("Erreur API")))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void mfa_Success() throws Exception {
        User user = User.builder().email("test@example.com").mfa(true).build();

        // Crée l'authentification et le contexte
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

        // définit le contexte de sécurité global pour ce thread
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            // Exécute la requête
            mockMvc.perform(get("/mfa")
                            .principal(auth))
                    .andExpect(status().isOk())
                    .andExpect(view().name("mfa"))
                    .andExpect(model().attribute("email", "test@example.com"));
        } finally {
            // IMPORTANT : Toujours nettoyer le contexte après le test pour éviter de polluer les autres tests
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("POST /mfa - Succès")
    void validateCode_Success() throws Exception {
        String userUuidStr = UUID.randomUUID().toString();
        User user = User.builder().userUuid(userUuidStr).email("test@example.com").build();

        MfaAuthentication auth = mock(MfaAuthentication.class);
        Authentication primaryAuth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(auth.getPrimaryAuthentication()).thenReturn(primaryAuth);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userService.verifyQrCode(eq(userUuidStr), eq("123456"))).thenReturn(true);

        // Utilisation de ArgumentMatchers.<Type>any() pour forcer Java 21
        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.sendRedirect("/home");
            return null;
        }).when(authenticationSuccessHandler).onAuthenticationSuccess(
                ArgumentMatchers.<HttpServletRequest>any(),
                ArgumentMatchers.<HttpServletResponse>any(),
                ArgumentMatchers.<Authentication>any()
        );

        try {
            mockMvc.perform(post("/mfa")
                            .param("code", "123456")
                            .principal(auth)
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection());

            // Pour le verify, si any() plante encore, utilise le même format
            verify(authenticationSuccessHandler).onAuthenticationSuccess(
                    ArgumentMatchers.<HttpServletRequest>any(),
                    ArgumentMatchers.<HttpServletResponse>any(),
                    ArgumentMatchers.<Authentication>any()
            );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("POST /mfa - Échec de validation (Code incorrect)")
    void validateCode_Failure() throws Exception {
        // 1. Setup
        String userUuidStr = UUID.randomUUID().toString();
        User user = User.builder()
                .userUuid(userUuidStr)
                .email("test@example.com")
                .build();

        MfaAuthentication auth = mock(MfaAuthentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // FALSE pour simuler un mauvais code
        when(userService.verifyQrCode(eq(userUuidStr), eq("000000"))).thenReturn(false);

        try {
            //  Action
            mockMvc.perform(post("/mfa")
                            .param("code", "000000")
                            .principal(auth)
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection());

            // Verification du service
            verify(userService).verifyQrCode(userUuidStr, "000000");

            // Verification que le succès n'a JAMAIS été appelé (Syntaxe sécurisée)
            verify(authenticationSuccessHandler, never()).onAuthenticationSuccess(
                    ArgumentMatchers.<HttpServletRequest>any(),
                    ArgumentMatchers.<HttpServletResponse>any(),
                    ArgumentMatchers.<Authentication>any()
            );

        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("GET /error - Affichage de la page d'erreur générique")
    void handleError_GenericError() throws Exception {
        // On simule une exception quelconque (pas une auth exception)
        Exception genericException = new RuntimeException("Erreur système");

        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, genericException))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("message"));
        // 'message' est ajouté via getMessage(request) dans ton contrôleur
    }

    @Test
    @DisplayName("GET /error - Redirection vers login pour BadCredentialsException")
    void handleError_AuthError() throws Exception {
        // On simule une erreur d'authentification
        Exception authException = new BadCredentialsException("Identifiants invalides");

        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, authException))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
        // Pas de redirection 3xx ici car le contrôleur fait un 'return "login"' (forward interne Thymeleaf)
    }

    @Test
    @DisplayName("GET /logout - Affichage de la page de déconnexion")
    void logout_ShouldReturnLogoutView() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().isOk())
                .andExpect(view().name("logout"));
    }

}
package com.openclassrooms.authorizationserverservice.event;

import com.openclassrooms.authorizationserverservice.service.UserService;
import com.openclassrooms.authorizationserverservice.util.UserAgentUtils;
import com.openclassrooms.authorizationserverservice.util.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ApiAuthenticationEventListenerTest {

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ApiAuthenticationEventListener listener;

    @Test
    @DisplayName("Success : Devrait mettre à jour les infos utilisateur lors d'une connexion réussie")
    void onAuthenticationSuccess_ShouldUpdateUserAndDeviceInfos() {
        try (MockedStatic<UserUtils> mockedUserUtils = mockStatic(UserUtils.class);
             MockedStatic<UserAgentUtils> mockedUAUtils = mockStatic(UserAgentUtils.class)) {

            // 1. Mocks de sécurité
            Authentication auth = mock(Authentication.class);
            // On utilise une vraie instance de token pour que l' "instanceof" dans ton code soit TRUE
            UsernamePasswordAuthenticationToken principal = mock(UsernamePasswordAuthenticationToken.class);
            AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(auth);

            // Mockito classique (SANS le cast Publisher)
            when(auth.getPrincipal()).thenReturn(principal);

            // 2. Mock du retour de UserUtils.getUser
            // Note : Remplace "Object" par ton type réel (ex: UserDTO ou UserEntity)
            var mockUser = mock(com.openclassrooms.authorizationserverservice.model.User.class);
            when(mockUser.getUserId()).thenReturn(1L);
            when(mockUser.getUserUuid()).thenReturn("uuid-123");

            // 3. Configuration des stubs statiques
            mockedUserUtils.when(() -> UserUtils.getUser(auth)).thenReturn(mockUser);
            mockedUAUtils.when(() -> UserAgentUtils.getDevice(request)).thenReturn("Galaxy S21");
            mockedUAUtils.when(() -> UserAgentUtils.getClient(request)).thenReturn("Chrome");
            mockedUAUtils.when(() -> UserAgentUtils.getIpAddress(request)).thenReturn("127.0.0.1");

            // 4. EXECUTION
            listener.onAuthenticationSuccess(event);

            // 5. VERIFICATIONS
            verify(userService).setLastLogin(1L);
            verify(userService).resetLoginAttempts("uuid-123");
            verify(userService).addLoginDevice(1L, "Galaxy S21", "Chrome", "127.0.0.1");
        }
    }

    @Test
    @DisplayName("Failure : Devrait incrémenter les tentatives en cas de mauvais identifiants")
    void onAuthenticationFailure_ShouldUpdateAttempts_WhenBadCredentials() {
        // 1. Préparation des mocks
        Authentication auth = mock(Authentication.class);
        // On simule que le principal est l'email de l'utilisateur
        String userEmail = "test@example.com";
        when(auth.getPrincipal()).thenReturn(userEmail);

        // 2. Création de l'exception et de l'événement
        BadCredentialsException exception = new BadCredentialsException("Invalid password");

        // AbstractAuthenticationFailureEvent est abstraite, on utilise une implémentation concrète de Spring
        AbstractAuthenticationFailureEvent event = new org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent(auth, exception);

        // 3. EXECUTION
        listener.onAuthenticationFailure(event);

        // 4. VERIFICATION
        verify(userService).updateLoginAttempts(userEmail);
    }

    @Test
    @DisplayName("Failure : Ne devrait rien faire si l'exception n'est pas BadCredentials")
    void onAuthenticationFailure_ShouldDoNothing_WhenOtherException() {
        // 1. Préparation avec une autre exception (ex: compte verrouillé)
        Authentication auth = mock(Authentication.class);
        var otherException = new org.springframework.security.authentication.LockedException("Locked");
        var event = new org.springframework.security.authentication.event.AuthenticationFailureLockedEvent(auth, otherException);

        // 2. EXECUTION
        listener.onAuthenticationFailure(event);

        // 3. VERIFICATION : updateLoginAttempts ne doit JAMAIS être appelé
        verify(userService, never()).updateLoginAttempts(anyString());
    }
}
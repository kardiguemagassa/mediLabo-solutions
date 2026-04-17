package com.openclassrooms.authorizationserverservice.security;

import com.openclassrooms.authorizationserverservice.model.User;
import com.openclassrooms.authorizationserverservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationProviderTest {

    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserAuthenticationProvider provider;
    private User testUser;

    @BeforeEach
    void setUp() {
        provider = new UserAuthenticationProvider(userService, passwordEncoder);

        // Utilisateur par défaut valide pour les tests
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole("ROLE_USER");
        testUser.setAuthorities("READ_PRIVILEGE");
        testUser.setAccountNonLocked(true);
        testUser.setEnabled(true);
        testUser.setAccountNonExpired(true);
        testUser.setLoginAttempts(0);
    }

    @Test
    @DisplayName("Authenticate : Succès nominal")
    void authenticate_Success() {
        Authentication authRequest = new UsernamePasswordAuthenticationToken("test@example.com", "rawPassword");

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);

        Authentication result = provider.authenticate(authRequest);

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(((User) result.getPrincipal()).getEmail()).isEqualTo("test@example.com");
        assertThat(result.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "READ_PRIVILEGE");
    }

    @Test
    @DisplayName("Authenticate : Mauvais mot de passe (incrémente les tentatives)")
    void authenticate_WrongPassword() {
        Authentication authRequest = new UsernamePasswordAuthenticationToken("test@example.com", "wrongPassword");

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> provider.authenticate(authRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Email ou mot de passe incorrect");

        verify(userService).updateLoginAttempts("test@example.com");
    }

    @Test
    @DisplayName("Validate : Compte verrouillé manuellement")
    void validate_AccountLocked() {
        testUser.setAccountNonLocked(false);
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);

        Authentication auth = new UsernamePasswordAuthenticationToken("test@example.com", "pass");

        assertThatThrownBy(() -> provider.authenticate(auth))
                .isInstanceOf(LockedException.class)
                .hasMessageContaining("Compte actuellement verrouillé");
    }

    @Test
    @DisplayName("Validate : Compte verrouillé par trop de tentatives")
    void validate_TooManyAttempts() {
        testUser.setLoginAttempts(5);
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);

        Authentication auth = new UsernamePasswordAuthenticationToken("test@example.com", "pass");

        assertThatThrownBy(() -> provider.authenticate(auth))
                .isInstanceOf(LockedException.class)
                .hasMessageContaining("Compte verrouillé après 5 tentatives");
    }

    @Test
    @DisplayName("Validate : Compte désactivé")
    void validate_Disabled() {
        testUser.setEnabled(false);
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);

        Authentication auth = new UsernamePasswordAuthenticationToken("test@example.com", "pass");

        assertThatThrownBy(() -> provider.authenticate(auth))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    @DisplayName("Validate : Compte expiré")
    void validate_Expired() {
        testUser.setAccountNonExpired(false);
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);

        Authentication auth = new UsernamePasswordAuthenticationToken("test@example.com", "pass");

        assertThatThrownBy(() -> provider.authenticate(auth))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("expiré");
    }

    @Test
    @DisplayName("Authenticate : Erreur générique transformée en BadCredentials")
    void authenticate_GenericException() {
        when(userService.getUserByEmail(anyString())).thenThrow(new RuntimeException("DB Down"));

        Authentication auth = new UsernamePasswordAuthenticationToken("error@test.com", "pass");

        assertThatThrownBy(() -> provider.authenticate(auth))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("DB Down");
    }

    @Test
    @DisplayName("Supports : Vérification du type de token")
    void supports_Check() {
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(String.class)).isFalse();
    }
}
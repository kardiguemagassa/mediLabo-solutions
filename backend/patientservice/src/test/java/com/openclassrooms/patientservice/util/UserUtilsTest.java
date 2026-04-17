package com.openclassrooms.patientservice.util;

import com.openclassrooms.patientservice.constant.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour la classe {@link UserUtils}.
 *  pour la classe Role avec USER.
 */
@ExtendWith(MockitoExtension.class)
class UserUtilsTest {

    private MockedStatic<SecurityContextHolder> securityContextHolderMock;
    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Mock de SecurityContextHolder
        securityContextHolderMock = Mockito.mockStatic(SecurityContextHolder.class);
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);

        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }

    @AfterEach
    void tearDown() {
        if (securityContextHolderMock != null) {
            securityContextHolderMock.close();
        }
    }

    // Tests pour hasElevatedPermissions

    @ParameterizedTest
    @MethodSource("provideElevatedRoles")
    void hasElevatedPermissions_shouldReturnTrue_forElevatedRoles(String role) {
        // Given
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When
        boolean result = UserUtils.hasElevatedPermissions();

        // Then
        assertThat(result).isTrue();
    }

    private static Stream<Arguments> provideElevatedRoles() {
        return Stream.of(
                Arguments.of(Role.SUPER_ADMIN),
                Arguments.of(Role.ADMIN),
                Arguments.of(Role.ORGANIZER),
                Arguments.of(Role.PRACTITIONER)
        );
    }

    @Test
    void hasElevatedPermissions_shouldReturnFalse_forUserRole() {
        // Given
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.USER));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When
        boolean result = UserUtils.hasElevatedPermissions();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasElevatedPermissions_shouldReturnFalse_whenAuthenticationIsNull() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        boolean result = UserUtils.hasElevatedPermissions();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasElevatedPermissions_shouldReturnFalse_whenNotAuthenticated() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        boolean result = UserUtils.hasElevatedPermissions();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasElevatedPermissions_shouldReturnFalse_whenNoAuthorities() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(inv -> List.of());

        // When
        boolean result = UserUtils.hasElevatedPermissions();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasElevatedPermissions_shouldReturnTrue_whenMultipleAuthoritiesIncludingElevated() {
        // Given
        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(Role.USER),
                new SimpleGrantedAuthority(Role.ADMIN)
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When
        boolean result = UserUtils.hasElevatedPermissions();

        // Then
        assertThat(result).isTrue();
    }

    // Tests pour hasRole

    @Test
    void hasRole_shouldReturnTrue_whenUserHasRole() {
        // Given
        String targetRole = Role.ADMIN;
        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(Role.USER),
                new SimpleGrantedAuthority(Role.ADMIN)
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When
        boolean result = UserUtils.hasRole(targetRole);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasRole_shouldReturnFalse_whenUserDoesNotHaveRole() {
        // Given
        String targetRole = Role.ADMIN;
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.USER));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When
        boolean result = UserUtils.hasRole(targetRole);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasRole_shouldReturnFalse_whenAuthenticationIsNull() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        boolean result = UserUtils.hasRole(Role.ADMIN);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasRole_shouldReturnFalse_whenNotAuthenticated() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        boolean result = UserUtils.hasRole(Role.ADMIN);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasRole_shouldBeCaseSensitive() {
        // Given
        String targetRole = "ADMIN";  // Majuscules
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("admin"));  // minuscules
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When
        boolean result = UserUtils.hasRole(targetRole);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasRole_shouldWorkWithAllDefinedRoles() {
        // Test avec tous les rôles définis dans Role.java
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);

        // Test chaque rôle
        for (String role : getAllDefinedRoles()) {
            Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
            when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

            boolean result = UserUtils.hasRole(role);
            assertThat(result)
                    .as("should return true for role: " + role)
                    .isTrue();
        }
    }

    private List<String> getAllDefinedRoles() {
        return List.of(Role.USER, Role.ADMIN, Role.PRACTITIONER, Role.ORGANIZER, Role.SUPER_ADMIN);
    }

    @Test
    void getCurrentUserUuid_shouldReturnUsername_whenAuthenticated() {
        // Given
        String expectedUuid = "user-123";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(expectedUuid);

        // When
        String result = UserUtils.getCurrentUserUuid();

        // Then
        assertThat(result).isEqualTo(expectedUuid);
    }

    @Test
    void getCurrentUserUuid_shouldReturnNull_whenAuthenticationIsNull() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        String result = UserUtils.getCurrentUserUuid();

        // Then
        assertThat(result).isNull();
    }

    @Test
    void getCurrentUserUuid_shouldReturnNull_whenNotAuthenticated() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        String result = UserUtils.getCurrentUserUuid();

        // Then
        assertThat(result).isNull();
    }

    @Test
    void getCurrentUserUuid_shouldReturnEmptyString_ifNameIsEmpty() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("");

        // When
        String result = UserUtils.getCurrentUserUuid();

        // Then
        assertThat(result).isEmpty();
    }

    // Tests pour isOwner

    @Test
    void isOwner_shouldReturnTrue_whenUuidMatches() {
        // Given
        String resourceOwnerUuid = "user-123";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(resourceOwnerUuid);

        // When
        boolean result = UserUtils.isOwner(resourceOwnerUuid);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isOwner_shouldReturnFalse_whenUuidDoesNotMatch() {
        // Given
        String resourceOwnerUuid = "user-123";
        String currentUserUuid = "user-456";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(currentUserUuid);

        // When
        boolean result = UserUtils.isOwner(resourceOwnerUuid);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isOwner_shouldReturnFalse_whenCurrentUserUuidIsNull() {
        // Given
        String resourceOwnerUuid = "user-123";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(null);

        // When
        boolean result = UserUtils.isOwner(resourceOwnerUuid);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isOwner_shouldReturnFalse_whenNotAuthenticated() {
        // Given
        String resourceOwnerUuid = "user-123";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        boolean result = UserUtils.isOwner(resourceOwnerUuid);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isOwner_shouldReturnFalse_whenAuthenticationIsNull() {
        // Given
        String resourceOwnerUuid = "user-123";
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        boolean result = UserUtils.isOwner(resourceOwnerUuid);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void canAccess_shouldReturnTrue_whenUserHasElevatedPermissions() {
        // Given
        String resourceOwnerUuid = "user-123";
        String currentUserUuid = "admin-456";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.ADMIN));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(currentUserUuid);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When
        boolean result = UserUtils.canAccess(resourceOwnerUuid);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void canAccess_shouldReturnFalse_whenNotOwnerAndNoElevatedPermissions() {
        // Given
        String resourceOwnerUuid = "user-123";
        String currentUserUuid = "user-456";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.USER));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(currentUserUuid);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When
        boolean result = UserUtils.canAccess(resourceOwnerUuid);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void canAccess_shouldReturnFalse_whenNotAuthenticated() {
        // Given
        String resourceOwnerUuid = "user-123";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        boolean result = UserUtils.canAccess(resourceOwnerUuid);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void canAccess_shouldReturnFalse_whenAuthenticationIsNull() {
        // Given
        String resourceOwnerUuid = "user-123";
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        boolean result = UserUtils.canAccess(resourceOwnerUuid);

        // Then
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @MethodSource("provideAllElevatedRolesForCanAccess")
    void canAccess_shouldReturnTrue_forAllElevatedRoles(String role) {
        // Given: Un utilisateur avec un rôle élevé qui n'est pas le propriétaire
        String resourceOwnerUuid = "owner-123";
        String currentUserUuid = role.toLowerCase() + "-456";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(currentUserUuid);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When
        boolean result = UserUtils.canAccess(resourceOwnerUuid);

        // Then
        assertThat(result)
                .as("should return true for elevated role: " + role)
                .isTrue();
    }

    private static Stream<Arguments> provideAllElevatedRolesForCanAccess() {
        return Stream.of(Arguments.of(Role.SUPER_ADMIN), Arguments.of(Role.ADMIN), Arguments.of(Role.ORGANIZER), Arguments.of(Role.PRACTITIONER));
    }

    @Test
    void canAccess_shouldReturnFalse_forUserRoleWhenNotOwner() {
        // Given: Un USER qui n'est pas le propriétaire
        String resourceOwnerUuid = "owner-123";
        String currentUserUuid = "user-456";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.USER));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(currentUserUuid);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When
        boolean result = UserUtils.canAccess(resourceOwnerUuid);

        // Then
        assertThat(result).isFalse();
    }


    @Test
    void constructor_shouldBePrivate() throws Exception {
        // Given
        var constructor = UserUtils.class.getDeclaredConstructor();

        // Then
        assertThat(constructor.canAccess(null)).isFalse();

        // When
        constructor.setAccessible(true);

        // Then
        assertThat(constructor.newInstance()).isNotNull();
    }

    @Test
    void methods_shouldWorkTogetherCorrectly_forAdmin() {
        // Given: Un admin qui n'est pas le propriétaire
        String ownerUuid = "owner-123";
        String adminUuid = "admin-456";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.ADMIN));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(adminUuid);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When & Then
        assertThat(UserUtils.getCurrentUserUuid()).isEqualTo(adminUuid);
        assertThat(UserUtils.hasRole(Role.ADMIN)).isTrue();
        assertThat(UserUtils.hasElevatedPermissions()).isTrue();
        assertThat(UserUtils.isOwner(ownerUuid)).isFalse();
        assertThat(UserUtils.canAccess(ownerUuid)).isTrue(); // Car admin a des permissions élevées
    }

    @Test
    void methods_shouldWorkForUserOwner() {
        // Given: Un USER qui est le propriétaire
        String userUuid = "user-123";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.USER));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(userUuid);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When & Then
        assertThat(UserUtils.getCurrentUserUuid()).isEqualTo(userUuid);
        assertThat(UserUtils.hasRole(Role.USER)).isTrue();
        assertThat(UserUtils.hasElevatedPermissions()).isFalse();
        assertThat(UserUtils.isOwner(userUuid)).isTrue();
        assertThat(UserUtils.canAccess(userUuid)).isTrue(); // Car il est le propriétaire
    }

    @Test
    void methods_shouldWorkForPractitioner() {
        // Given: Un PRACTITIONER qui n'est pas le propriétaire
        String ownerUuid = "user-123";
        String practitionerUuid = "practitioner-456";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.PRACTITIONER));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(practitionerUuid);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When & Then
        assertThat(UserUtils.getCurrentUserUuid()).isEqualTo(practitionerUuid);
        assertThat(UserUtils.hasRole(Role.PRACTITIONER)).isTrue();
        assertThat(UserUtils.hasElevatedPermissions()).isTrue();
        assertThat(UserUtils.isOwner(ownerUuid)).isFalse();
        assertThat(UserUtils.canAccess(ownerUuid)).isTrue(); // Car PRACTITIONER a des permissions élevées
    }

    @Test
    void methods_shouldWorkForSuperAdmin() {
        // Given: Un SUPER_ADMIN
        String anyUuid = "any-123";
        String superAdminUuid = "superadmin-456";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.SUPER_ADMIN));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(superAdminUuid);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When & Then
        assertThat(UserUtils.getCurrentUserUuid()).isEqualTo(superAdminUuid);
        assertThat(UserUtils.hasRole(Role.SUPER_ADMIN)).isTrue();
        assertThat(UserUtils.hasElevatedPermissions()).isTrue();
        assertThat(UserUtils.isOwner(anyUuid)).isFalse();
        assertThat(UserUtils.canAccess(anyUuid)).isTrue(); // Car SUPER_ADMIN a des permissions élevées
    }

    @Test
    void methods_shouldWorkForOrganizer() {
        // Given: Un ORGANIZER
        String ownerUuid = "user-123";
        String organizerUuid = "organizer-456";
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.ORGANIZER));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(organizerUuid);
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);

        // When & Then
        assertThat(UserUtils.getCurrentUserUuid()).isEqualTo(organizerUuid);
        assertThat(UserUtils.hasRole(Role.ORGANIZER)).isTrue();
        assertThat(UserUtils.hasElevatedPermissions()).isTrue();
        assertThat(UserUtils.isOwner(ownerUuid)).isFalse();
        assertThat(UserUtils.canAccess(ownerUuid)).isTrue(); // Car ORGANIZER a des permissions élevées
    }
}
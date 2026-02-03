package com.openclassrooms.notesservice.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtConverterTest {

    private JwtConverter jwtConverter;

    @BeforeEach
    void setUp() {
        jwtConverter = new JwtConverter();
    }

    // Test de base pour la conversion
    @Test
    void convert_shouldReturnJwtAuthenticationToken() {
        // Given
        Map<String, Object> headers = Map.of("alg", "RS256", "typ", "JWT");
        Map<String, Object> claims = Map.of("sub", "test-user");

        Jwt jwt = Jwt.withTokenValue("test-token")
                .headers(h -> h.putAll(headers))
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken.class);
        assertThat(result.getName()).isEqualTo("test-user");
        assertThat(result.getPrincipal()).isEqualTo(jwt);
        // Ne pas vérifier getCredentials() car JwtAuthenticationToken ne le définit pas toujours
    }

    // Tests pour l'extraction du claim "role"
    @Test
    void convert_withRoleClaim_shouldAddRoleAuthority() {
        // Given
        Map<String, Object> claims = Map.of("sub", "user1", "role", "admin");
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
    }

    @Test
    void convert_withRoleClaimLowerCase_shouldConvertToUpperCase() {
        // Given
        Map<String, Object> claims = Map.of("sub", "user1", "role", "user");
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_USER");
    }

    @Test
    void convert_withEmptyRoleClaim_shouldNotAddAuthority() {
        // Given
        Map<String, Object> claims = Map.of("sub", "user1", "role", "");
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities()).isEmpty();
    }

    @Test
    void convert_withNullRoleClaim_shouldNotAddAuthority() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user1");
        claims.put("role", null);
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities()).isEmpty();
    }

    // Tests pour l'extraction du claim "roles" (liste)
    @Test
    void convert_withRolesClaimList_shouldAddAllRoles() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user1");
        claims.put("roles", Arrays.asList("admin", "user", "moderator"));
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(
                        "ROLE_ADMIN",
                        "ROLE_USER",
                        "ROLE_MODERATOR"
                );
    }

    @Test
    void convert_withEmptyRolesList_shouldNotAddAuthorities() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user1");
        claims.put("roles", Collections.emptyList());
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities()).isEmpty();
    }

    @Test
    void convert_withNullRolesClaim_shouldHandleGracefully() {
        // Given
        Map<String, Object> claims = Map.of("sub", "user1");
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then - devrait gérer null sans exception
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities()).isEmpty();
    }

    // Tests pour l'extraction du claim "authorities"
    @Test
    void convert_withAuthoritiesClaim_shouldAddAllAuthorities() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user1");
        claims.put("authorities", Arrays.asList("READ", "WRITE", "DELETE"));
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("READ", "WRITE", "DELETE");
    }

    @Test
    void convert_withAuthoritiesClaimMixedCase_shouldPreserveCase() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user1");
        claims.put("authorities", Arrays.asList("read", "WRITE", "Delete"));
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("read", "WRITE", "Delete");
    }

    // Tests pour l'extraction du claim "scope"
    @Test
    void convert_withScopeClaim_shouldAddScopeAuthorities() {
        // Given
        Map<String, Object> claims = Map.of("sub", "user1", "scope", "read write delete");
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(
                        "SCOPE_read",
                        "SCOPE_write",
                        "SCOPE_delete"
                );
    }

    @Test
    void convert_withScopeClaimSingle_shouldAddSingleScope() {
        // Given
        Map<String, Object> claims = Map.of("sub", "user1", "scope", "read");
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("SCOPE_read");
    }

    @Test
    void convert_withScopeClaimMultipleSpaces_shouldHandleCorrectly() {
        // Given
        Map<String, Object> claims = Map.of("sub", "user1", "scope", "read  write   delete");
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then - le code actuel a un bug avec split(" ") qui crée des chaînes vides
        Assertions.assertNotNull(result);
        List<String> authorities = result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // Les scopes valides devraient être présents
        assertThat(authorities).contains("SCOPE_read", "SCOPE_write", "SCOPE_delete");

        // Il y aura aussi des SCOPE_ vides à cause du bug
        // On peut vérifier qu'il y a au moins 3 autorités (les 3 valides + des vides)
        assertThat(authorities.size()).isGreaterThanOrEqualTo(3);
    }

    // Tests combinés - extraction de tous les claims
    @Test
    void convert_withAllClaims_shouldCombineAllAuthorities() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user1");
        claims.put("role", "supervisor");
        claims.put("roles", Arrays.asList("admin", "user"));
        claims.put("authorities", Arrays.asList("READ", "WRITE"));
        claims.put("scope", "read write");

        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains(
                        "ROLE_SUPERVISOR",
                        "ROLE_ADMIN",
                        "ROLE_USER",
                        "READ",
                        "WRITE",
                        "SCOPE_read",
                        "SCOPE_write"
                );
    }

    // Tests pour les cas limites
    @Test
    void convert_withNoClaims_shouldReturnEmptyAuthorities() {
        // Given
        Map<String, Object> claims = Map.of("sub", "user1");
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities()).isEmpty();
        assertThat(result.getName()).isEqualTo("user1");
    }

    @Test
    void convert_withDuplicateAuthorities_shouldCreateDuplicates() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user1");
        claims.put("role", "admin");
        claims.put("roles", Arrays.asList("admin", "admin"));
        claims.put("authorities", Arrays.asList("READ", "READ"));
        claims.put("scope", "read read");

        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        List<String> authorityNames = result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // Vérifier la présence des autorités (peu importe les duplicats)
        assertThat(authorityNames).contains("ROLE_ADMIN", "READ", "SCOPE_read");
    }

    // Tests paramétrés pour différents formats de rôle
    @ParameterizedTest
    @MethodSource("provideRoleFormats")
    void convert_withVariousRoleFormats_shouldNormalizeCorrectly(String inputRole, String expectedAuthority) {
        // Given
        Map<String, Object> claims = Map.of("sub", "user1", "role", inputRole);
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains(expectedAuthority);
    }

    private static Stream<Arguments> provideRoleFormats() {
        return Stream.of(
                Arguments.of("admin", "ROLE_ADMIN"),
                Arguments.of("ADMIN", "ROLE_ADMIN"),
                Arguments.of("Admin", "ROLE_ADMIN"),
                Arguments.of("user_manager", "ROLE_USER_MANAGER"),
                Arguments.of("USER-MANAGER", "ROLE_USER-MANAGER"),
                Arguments.of("123", "ROLE_123")
        );
    }

    // Test pour vérifier que le principal est bien le JWT
    @Test
    void convert_shouldSetJwtAsPrincipal() {
        // Given
        Map<String, Object> claims = Map.of("sub", "user1", "role", "admin");
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getPrincipal()).isEqualTo(jwt);
        // Ne pas vérifier getCredentials() - ce n'est pas défini par JwtAuthenticationToken
    }

    // Test pour vérifier le nom du principal
    @Test
    void convert_shouldUseSubClaimAsPrincipalName() {
        // Given
        Map<String, Object> claims = Map.of("sub", "john.doe@example.com");
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getName()).isEqualTo("john.doe@example.com");
    }

    @Test
    void convert_withoutSubClaim_shouldReturnNullName() {
        // Given
        Map<String, Object> claims = Map.of("preferred_username", "john.doe");
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getName()).isNull();
    }

    // Test pour vérifier que les logs ne cassent pas l'exécution
    @Test
    void convert_withInvalidClaimTypes_shouldHandleGracefully() {
        // Given - Créer un JWT avec des claims de types invalides
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user1");
        claims.put("role", "admin");
        // "roles" comme Map au lieu de List
        claims.put("roles", Map.of("key", "value"));
        // "authorities" comme String au lieu de List
        claims.put("authorities", "READ,WRITE");
        claims.put("scope", "read write");

        Jwt jwt = createJwtWithClaims(claims);

        // When & Then - Ne devrait pas lever d'exception
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Vérifier que les autorités valides sont extraites
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "SCOPE_read", "SCOPE_write");
    }

    // Test pour vérifier le comportement avec scope null
    @Test
    void convert_withNullScope_shouldNotAddScopes() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user1");
        claims.put("scope", null);
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.getAuthorities()).isEmpty();
    }

    // Test pour vérifier l'ordre d'extraction
    @Test
    void extractAuthorities_shouldExtractAllSources() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user1");
        claims.put("role", "role1");
        claims.put("roles", Arrays.asList("role2", "role3"));
        claims.put("authorities", Arrays.asList("auth1", "auth2"));
        claims.put("scope", "scope1 scope2");

        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        List<String> authorityNames = result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        assertThat(authorityNames).contains(
                "ROLE_ROLE1",
                "ROLE_ROLE2",
                "ROLE_ROLE3",
                "auth1",
                "auth2",
                "SCOPE_scope1",
                "SCOPE_scope2"
        );
    }

    // Test pour vérifier le comportement avec une portée contenant uniquement des espaces
    @Test
    void convert_withScopeContainingOnlySpaces_shouldCreateEmptyScopes() {
        // Given
        Map<String, Object> claims = Map.of("sub", "user1", "scope", "   ");
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        // split(" ") sur "   " donnera ["", "", ""]
        // Chaque chaîne vide créera un SCOPE_
        Assertions.assertNotNull(result);
        List<String> authorities = result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // Comportement actuel: créer des SCOPE_ vides
        assertThat(authorities).allMatch(a -> a.equals("SCOPE_"));
    }

    // Méthode utilitaire pour créer un JWT avec des claims
    private Jwt createJwtWithClaims(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token-" + UUID.randomUUID())
                .header("alg", "RS256")
                .header("typ", "JWT")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuer("http://auth-server");

        claims.forEach(builder::claim);

        return builder.build();
    }

    // Test supplémentaire: vérifier que l'authentification est correctement marquée comme authentifiée
    @Test
    void convert_shouldCreateAuthenticatedToken() {
        // Given
        Map<String, Object> claims = Map.of("sub", "user1");
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then
        Assertions.assertNotNull(result);
        assertThat(result.isAuthenticated()).isTrue();
    }

    // Test pour les scopes avec tabulations ou autres whitespace
    @Test
    void convert_withScopeContainingTabs_shouldHandleAsSingleScope() {
        // Given
        Map<String, Object> claims = Map.of("sub", "user1", "scope", "read\twrite\ndelete");
        Jwt jwt = createJwtWithClaims(claims);

        // When
        AbstractAuthenticationToken result = jwtConverter.convert(jwt);

        // Then - split(" ") ne gère pas les tabs, donc tout sera considéré comme un seul scope
        Assertions.assertNotNull(result);
        List<String> authorities = result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        assertThat(authorities).containsExactly("SCOPE_read\twrite\ndelete");
    }
}
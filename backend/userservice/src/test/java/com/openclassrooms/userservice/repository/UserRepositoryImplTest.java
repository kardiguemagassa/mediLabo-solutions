package com.openclassrooms.userservice.repository;

import com.openclassrooms.userservice.exception.ApiException;
import com.openclassrooms.userservice.model.*;
import com.openclassrooms.userservice.repository.impl.UserRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.function.Supplier;

import static com.openclassrooms.userservice.query.UserQuery.CREATE_USER_STORED_PROCEDURE;
import static java.sql.Types.VARCHAR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JdbcClient jdbcClient;

    private UserRepositoryImpl userRepository;

    @Mock
    private Supplier<String> randomUUUID;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepositoryImpl(jdbcClient);
    }

    @Nested
    @DisplayName("getUsersPageable Tests")
    class GetUsersPageableTests {

        @Test
        @DisplayName("Should return paginated users list")
        void shouldReturnPaginatedUsers() {
            List<User> expectedUsers = List.of(new User(), new User());

            when(jdbcClient.sql(anyString()).params(anyMap()).query(User.class).list())
                    .thenReturn(expectedUsers);

            List<User> result = userRepository.getUsersPageable(10, 0);

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Should return empty list when no users on page")
        void shouldReturnEmptyList_WhenNoUsersOnPage() {
            when(jdbcClient.sql(anyString()).params(anyMap()).query(User.class).list())
                    .thenReturn(List.of());

            List<User> result = userRepository.getUsersPageable(10, 100);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should throw ApiException on database error")
        void shouldThrowApiException_OnDatabaseError() {
            when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("DB error"));

            ApiException ex = assertThrows(ApiException.class,
                    () -> userRepository.getUsersPageable(10, 0));

            assertEquals("Une erreur s'est produite. Veuillez réessayer.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("countUsers Tests")
    class CountUsersTests {

        @Test
        @DisplayName("Should return total user count")
        void shouldReturnTotalCount() {
            when(jdbcClient.sql(anyString()).query(Long.class).single())
                    .thenReturn(25L);

            long count = userRepository.countUsers();

            assertEquals(25L, count);
        }

        @Test
        @DisplayName("Should return 0 on database error")
        void shouldReturnZero_OnDatabaseError() {
            when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("DB error"));

            long count = userRepository.countUsers();

            assertEquals(0L, count);
        }
    }

    @Test
    void getUserByUuid_Success() {
        // GIVEN
        String uuid = "some-uuid";
        User expectedUser = new User();

        // On mocke les interfaces intermédiaires de JdbcClient
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);

        // C'est le .single() qui retourne l'objet final
        when(querySpec.single()).thenReturn(expectedUser);

        // WHEN
        User actualUser = userRepository.getUserByUuid(uuid);

        // THEN
        assertNotNull(actualUser);
        assertEquals(expectedUser, actualUser);
    }

    @Test
    void getUserById_Success() {
        // GIVEN
        Long id = 1L;
        User expectedUser = new User();

        // CORRECTION : On utilise le SETTER pour définir l'ID, pas le getter.
        // Si ton champ dans User s'appelle 'id', c'est setId(id).
        expectedUser.setUserId(id);

        // Simulation de l'interface fluide du JdbcClient
        when(jdbcClient.sql(anyString())
                .param(eq("id"), eq(id))
                .query(User.class)
                .single())
                .thenReturn(expectedUser);

        // WHEN
        User result = userRepository.getUserById(id);

        // THEN
        assertNotNull(result);
        // CORRECTION : On compare l'ID retourné avec l'ID envoyé
        assertEquals(id, result.getUserId());
    }

    @Test
    void getUserById_GenericException_ShouldThrowApiException() {
        // GIVEN : On simule une erreur imprévue (ex: problème de connexion DB)
        // On peut jeter l'exception dès le premier appel .sql()
        when(jdbcClient.sql(anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.getUserById(1L));

        // Vérifie que c'est le message du SECOND bloc catch qui est renvoyé
        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void getUserByUuid_NotFound_ShouldThrowApiException() {
        // GIVEN
        when(jdbcClient.sql(anyString())
                .param(anyString(), any())
                .query(User.class)
                .single())
                .thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.getUserByUuid("unknown"));

        assertTrue(exception.getMessage().contains("Aucun utilisateur trouvé"));
    }

    @Test
    void getUserByUuid_GenericException_ShouldThrowApiException() {
        // GIVEN
        when(jdbcClient.sql(anyString())
                .param(anyString(), any())
                .query(User.class)
                .single())
                .thenThrow(new RuntimeException("DB Down"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.getUserByUuid("any"));

        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void validateCode_ShouldReturnTrue() {
        // WHEN
        boolean result = userRepository.validateCode("any-uuid", "123456");

        // THEN
        assertTrue(result, "La méthode devrait retourner true pour le moment");
    }

//    @Test
//    void resetLoginAttempts_GenericException_ShouldThrowApiException() {
//        // GIVEN : On prépare le mock pour la chaîne fluide
//        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
//
//        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
//        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
//
//        // On force l'échec sur l'appel final .update() avec une exception générique
//        when(statementSpec.update()).thenThrow(new RuntimeException("Database timeout"));
//
//        // WHEN & THEN
//        ApiException exception = assertThrows(ApiException.class,
//                () -> userRepository.resetLoginAttempts("some-uuid"));
//
//        // On vérifie que le message est celui du second catch
//        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
//    }
//
//    @Test
//    void addLoginDevice_UserNotFound_ShouldThrowApiException() {
//        // GIVEN
//        Long userId = 1L;
//        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
//
//        // On simule l'enchaînement fluide : sql -> params
//        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
//        when(statementSpec.params(anyMap())).thenReturn(statementSpec);
//
//        // On force l'exception spécifique sur l'appel final .update()
//        when(statementSpec.update()).thenThrow(new EmptyResultDataAccessException(1));
//
//        // WHEN & THEN
//        ApiException exception = assertThrows(ApiException.class,
//                () -> userRepository.addLoginDevice(userId, "Phone", "Chrome", "127.0.0.1"));
//
//        // Vérification du message formatté
//        assertTrue(exception.getMessage().contains("Aucun utilisateur trouvé avec l'ID " + userId));
//    }

    @Test
    void getAccountToken_GenericException_ShouldThrowApiException() {
        // GIVEN
        String token = "invalid-token-123";

        // On simule une erreur brute de la base de données (RuntimeException)
        // Cela permet de bypasser le premier catch spécifique
        when(jdbcClient.sql(anyString()))
                .thenThrow(new RuntimeException("Database is unreachable"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.getAccountToken(token));

        // On vérifie que le message correspond bien au SECOND catch
        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void verifyPasswordToken_ShouldReturnNull() {
        // GIVEN
        String token = "some-token";

        // WHEN
        User result = userRepository.verifyPasswordToken(token);

        // THEN
        assertNull(result, "La méthode devrait retourner null pour le moment");
    }

    @Test
    void enableMfa_UserNotFound_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.paramSource(any())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);

        // On force l'exception sur l'appel final
        when(querySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.enableMfa("uuid-test"));

        assertEquals("Utilisateur introuvable. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void disableMfa_UserNotFound_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);

        // On force l'exception "Pas de résultat"
        when(querySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        assertThrows(ApiException.class, () -> userRepository.disableMfa("uuid-test"));
    }

    @Test
    void disableMfa_GenericException_ShouldThrowApiException() {
        // GIVEN : On simule une erreur SQL brute
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("DB Error"));

        // WHEN & THEN
        assertThrows(ApiException.class, () -> userRepository.disableMfa("uuid-test"));
    }

    @Test
    void enableMfa_GenericException_ShouldThrowApiException() {
        // GIVEN
        String uuid = "test-uuid";

        // On simule une erreur brute (ex: perte de connexion DB)
        // Cela permet de bypasser le premier catch et d'entrer dans le second
        when(jdbcClient.sql(anyString()))
                .thenThrow(new RuntimeException("Erreur critique base de données"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.enableMfa(uuid));

        // On vérifie que le message correspond au catch générique
        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void toggleAccountExpired_UserNotFound_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);

        when(querySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class, () -> userRepository.toggleAccountExpired("uuid"));
        assertEquals("Utilisateur introuvable. Veuillez réessayer.", ex.getMessage());
    }

    @Test
    void toggleAccountExpired_GenericException_ShouldThrowApiException() {
        // GIVEN
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Fatal Error"));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class, () -> userRepository.toggleAccountExpired("uuid"));
        assertEquals("Une erreur s'est produite. Veuillez réessayer.", ex.getMessage());
    }

    @Test
    void disableMfa_Success_ShouldReturnUser() {
        // GIVEN
        String uuid = "user-uuid";
        User expectedUser = new User();
        expectedUser.setMfa(false);

        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        // On définit l'enchaînement complet
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);

        // TRÈS IMPORTANT : single() doit retourner un objet pour valider la ligne du return
        when(querySpec.single()).thenReturn(expectedUser);

        // WHEN
        User actualUser = userRepository.disableMfa(uuid);

        // THEN
        assertNotNull(actualUser);
        assertEquals(expectedUser, actualUser);
    }

    @Test
    void toggleAccountExpired_Success_ShouldReturnUser() {
        // GIVEN
        String uuid = "user-uuid";
        User expectedUser = new User();
        // On définit explicitement que le compte n'est pas expiré
        expectedUser.setAccountNonExpired(true);

        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);
        when(querySpec.single()).thenReturn(expectedUser);

        // WHEN
        User actualUser = userRepository.toggleAccountExpired(uuid);

        // THEN
        assertNotNull(actualUser);
        // CORRECTION ICI : On vérifie AccountNonExpired et non CredentialsNonExpired
        assertTrue(actualUser.isAccountNonExpired(), "Le compte devrait être marqué comme non expiré");
    }

    @Test
    void toggleAccountLocked_UserNotFound_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);

        // On simule l'exception "Pas de résultat" jetée par .single()
        when(querySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.toggleAccountLocked("uuid-inconnu"));

        assertEquals("Utilisateur introuvable. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void toggleAccountLocked_GenericException_ShouldThrowApiException() {
        // GIVEN
        // On jette une exception générique dès le début de la chaîne d'appels
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Database error"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.toggleAccountLocked("any-uuid"));

        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void toggleAccountEnabled_UserNotFound_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);

        // Simule le premier catch (EmptyResultDataAccessException)
        when(querySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class, () -> userRepository.toggleAccountEnabled("uuid"));
        assertEquals("Utilisateur introuvable. Veuillez réessayer.", ex.getMessage());
    }

    @Test
    void toggleAccountEnabled_GenericException_ShouldThrowApiException() {
        // GIVEN
        // Simule le second catch (Exception) en jetant une erreur dès le début
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Crash"));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class, () -> userRepository.toggleAccountEnabled("uuid"));
        assertEquals("Une erreur s'est produite. Veuillez réessayer.", ex.getMessage());
    }

    @Test
    void toggleAccountEnabled_Success_ShouldReturnUser() {
        // GIVEN
        String uuid = "user-uuid";
        User expectedUser = new User();
        expectedUser.setEnabled(true);

        // Mocks des interfaces fluides du JdbcClient
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        // On définit l'enchaînement complet
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(eq("userUuid"), eq(uuid))).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);

        // TRÈS IMPORTANT : single() doit retourner un objet pour que la ligne soit marquée comme finie
        when(querySpec.single()).thenReturn(expectedUser);

        // WHEN
        User actualUser = userRepository.toggleAccountEnabled(uuid);

        // THEN
        assertNotNull(actualUser);
        assertTrue(actualUser.isEnabled());
    }

    @Test
    void toggleCredentialsExpired_ShouldReturnNull() {
        // WHEN
        User result = userRepository.toggleCredentialsExpired("some-uuid");

        // THEN
        assertNull(result, "La méthode stub doit retourner null");
    }

    @Test
    void updateRole_Success_ShouldReturnUser() {
        // GIVEN
        String uuid = "user-123";
        String role = "ROLE_ADMIN";
        User expectedUser = new User();
        expectedUser.setRole(role);

        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.params(anyMap())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);
        when(querySpec.single()).thenReturn(expectedUser);

        // WHEN
        User actualUser = userRepository.updateRole(uuid, role);

        // THEN
        assertNotNull(actualUser);
        assertEquals(role, actualUser.getRole());
    }

    @Test
    void updatePassword_UserNotFound_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.params(anyMap())).thenReturn(statementSpec);

        // On force l'exception spécifique
        when(statementSpec.update()).thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.updatePassword("uuid-inconnu", "encoded-pass"));

        assertEquals("Utilisateur introuvable. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void updatePassword_GenericException_ShouldThrowApiException() {
        // GIVEN
        String uuid = "any-uuid";
        String pass = "encoded-pass";

        // On simule une erreur brute (ex: RuntimeException)
        // On peut l'attacher à l'appel .sql() ou à l'appel .update()
        when(jdbcClient.sql(anyString()))
                .thenThrow(new RuntimeException("Database error"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.updatePassword(uuid, pass));

        // On vérifie que le message est bien celui du SECOND catch
        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void getUsers_Success_ShouldReturnList() {
        // GIVEN
        User user = new User();
        List<User> expectedUsers = List.of(user);

        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);
        when(querySpec.list()).thenReturn(expectedUsers);

        // WHEN
        List<User> actualUsers = userRepository.getUsers();

        // THEN
        assertFalse(actualUsers.isEmpty());
        assertEquals(1, actualUsers.size());
    }

    @Test
    void getUsers_EmptyResult_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);
        when(querySpec.list()).thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class, () -> userRepository.getUsers());
        assertEquals("Utilisateur introuvable. Veuillez réessayer.", ex.getMessage());
    }

    @Test
    void getUsers_GenericException_ShouldThrowApiException() {
        // GIVEN
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("SQL Error"));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class, () -> userRepository.getUsers());
        assertEquals("Une erreur s'est produite. Veuillez réessayer.", ex.getMessage());
    }

    @Test
    void deleteAccountToken_Success() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(1); // Simule une ligne supprimée

        // WHEN & THEN
        assertDoesNotThrow(() -> userRepository.deleteAccountToken("token-123"));
    }

    @Test
    void deleteAccountToken_TokenNotFound_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);

        // On simule l'erreur de résultat vide
        when(statementSpec.update()).thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> userRepository.deleteAccountToken("invalid-token"));

        assertEquals("Token introuvable. Veuillez réessayer.", ex.getMessage());
    }

    @Test
    void deleteAccountToken_GenericException_ShouldThrowApiException() {
        // GIVEN
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("DB Connection Error"));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> userRepository.deleteAccountToken("token-123"));

        assertEquals("Une erreur s'est produite. Veuillez réessayer.", ex.getMessage());
    }

    @Test
    void deletePasswordToken_Success() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(1);

        // WHEN & THEN
        assertDoesNotThrow(() -> userRepository.deletePasswordToken("token-test"));
    }

    @Test
    void deletePasswordToken_NotFound_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);

        // On force l'exception spécifique à Spring JDBC
        when(statementSpec.update()).thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> userRepository.deletePasswordToken("token-absent"));

        assertEquals("Token introuvable. Veuillez réessayer.", ex.getMessage());
    }

    @Test
    void deletePasswordToken_GenericException_ShouldThrowApiException() {
        // GIVEN
        // On simule une erreur brute dès le début (ex: erreur de syntaxe ou DB down)
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Database error"));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> userRepository.deletePasswordToken("token-test"));

        assertEquals("Une erreur s'est produite. Veuillez réessayer", ex.getMessage());
    }

    @Test
    void deletePasswordToken_EmptyResult_ShouldBeCovered() {
        // GIVEN
        // On doit mocker chaque étape pour que l'exécution aille jusqu'au bout
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        // IMPORTANT : param() doit retourner le statementSpec lui-même (chaînage)
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);

        // On force l'exception sur l'appel final .update()
        when(statementSpec.update()).thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class, () -> userRepository.deletePasswordToken(1L));
        assertEquals("Token not found. Please try again.", ex.getMessage());
    }
    @Test
    void deletePasswordToken_GenericException_ShouldBeCovered() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);

        // On jette une exception qui n'est PAS une EmptyResultDataAccessException
        when(statementSpec.update()).thenThrow(new RuntimeException("Database crash"));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class, () -> userRepository.deletePasswordToken(1L));
        assertEquals("Une erreur s'est produite. Veuillez réessayer.", ex.getMessage());
    }

    @Test
    void getPatientUser_NotFound_ShouldThrowApiException() {
        // GIVEN
        String uuid = "patient-123";
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        // On mocke params() pour qu'il retourne le statementSpec (chaînage)
        when(statementSpec.params(anyMap())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);

        // On force l'exception sur le maillon final
        when(querySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.getPatientUser(uuid));

        assertTrue(exception.getMessage().contains("Aucun patient trouvé"));
    }

    @Test
    void getPatientUser_GenericException_ShouldThrowApiException() {
        // GIVEN
        // On jette l'erreur dès le premier appel .sql() pour s'assurer de tomber dans le catch(Exception)
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("DB Connection Lost"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.getPatientUser("any-uuid"));

        assertEquals("Une erreur s'est produite. Veuillez réessayer", exception.getMessage());
    }

    @Test
    void createPasswordToken_UserNotFound_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.params(anyMap())).thenReturn(statementSpec);

        // On simule l'échec de l'insertion (ex: FK violation ou contrainte)
        when(statementSpec.update()).thenThrow(new EmptyResultDataAccessException("User not found", 1));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> userRepository.createPasswordToken(1L));

        assertEquals("Utilisateur introuvable. Veuillez réessayer.", ex.getMessage());
    }

    @Test
    void getMediLaboSupports_Success_ShouldReturnList() {
        // GIVEN
        User user1 = new User(); // Supposez que vous avez un constructeur ou des setters
        user1.setUserId(1L);
        user1.setEmail("support@medilabo.com");

        List<User> expectedUsers = List.of(user1);

        // 1. Mock de la chaîne JdbcClient
        // On crée le mock pour l'étape .query(...) qui est de type MappedQuerySpec
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);

        // jdbc.sql(...) -> renvoie statementSpec
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);

        // statementSpec.query(User.class) -> renvoie querySpec
        when(statementSpec.query(User.class)).thenReturn(querySpec);

        // querySpec.list() -> renvoie notre liste finale
        when(querySpec.list()).thenReturn(expectedUsers);

        // WHEN
        List<User> result = userRepository.getMediLaboSupports();

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("support@medilabo.com", result.get(0).getEmail());

        // Vérification que la requête SQL a bien été appelée
        verify(jdbcClient).sql(anyString());
    }

    @Test
    void getMediLaboSupports_Exception_ShouldThrowApiException() {
        // GIVEN
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("DB Error"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () -> {
            userRepository.getMediLaboSupports();
        });

        assertEquals("Une erreur s'est produite. Veuillez réessayer", exception.getMessage());
    }

    @Test
    void getMediLaboSupports_EmptyResult_ShouldThrowSpecificApiException() {
        // GIVEN
        // On prépare les mocks intermédiaires
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);

        // On force l'exception sur le dernier appel de la chaîne (.list())
        when(querySpec.list()).thenThrow(new EmptyResultDataAccessException("No supports found", 1));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () -> {
            userRepository.getMediLaboSupports();
        });

        // On vérifie que c'est bien le message du PREMIER catch qui est renvoyé
        assertEquals("Utilisateur introuvable. Veuillez réessayer.", exception.getMessage());

        // On vérifie que le log a été appelé (optionnel si vous avez un logger mocké)
        verify(jdbcClient).sql(anyString());
    }

    @Test
    void getPassword_Success_ShouldReturnPassword() {
        // GIVEN
        String uuid = "user-123";
        String expectedPassword = "hashed_password";

        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<String> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(eq("userUuid"), eq(uuid))).thenReturn(statementSpec);
        when(statementSpec.query(String.class)).thenReturn(querySpec);
        when(querySpec.single()).thenReturn(expectedPassword);

        // WHEN
        String result = userRepository.getPassword(uuid);

        // THEN
        assertEquals(expectedPassword, result);
        verify(querySpec).single();
    }

    @Test
    void getPassword_NotFound_ShouldThrowApiException() {
        // GIVEN
        String uuid = "unknown-uuid";

        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<String> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(String.class)).thenReturn(querySpec);

        // On simule l'absence de résultat en base
        when(querySpec.single()).thenThrow(new EmptyResultDataAccessException("No user found", 1));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () -> {
            userRepository.getPassword(uuid);
        });

        assertEquals("Utilisateur introuvable. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void updateImageUrl_Success() {
        // GIVEN
        String uuid = "user-123";
        String url = "http://image.com/photo.jpg";

        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        // On simule la Map de paramètres. Note: Map.of est utilisé dans votre code (of)
        when(statementSpec.params(anyMap())).thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(1);

        // WHEN
        userRepository.updateImageUrl(uuid, url);

        // THEN
        verify(jdbcClient).sql(anyString());
        verify(statementSpec).update();
    }

    @Test
    void updateImageUrl_NotFound_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.params(anyMap())).thenReturn(statementSpec);

        // On force l'exception spécifique
        when(statementSpec.update()).thenThrow(new EmptyResultDataAccessException("User not found", 1));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () ->
                userRepository.updateImageUrl("uuid", "url")
        );

        assertEquals("Utilisateur introuvable. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void updateImageUrl_GenericError_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.params(anyMap())).thenReturn(statementSpec);

        // On simule une erreur de base de données (ex: connexion perdue)
        when(statementSpec.update()).thenThrow(new RuntimeException("Database down"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () ->
                userRepository.updateImageUrl("uuid", "url")
        );

        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void getPasswordToken_Success_ShouldReturnToken() {
        // GIVEN
        Long userId = 1L;
        PasswordToken expectedToken = new PasswordToken();
        expectedToken.setToken("some-token");

        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<PasswordToken> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.params(anyMap())).thenReturn(statementSpec);
        when(statementSpec.query(PasswordToken.class)).thenReturn(querySpec);
        when(querySpec.single()).thenReturn(expectedToken);

        // WHEN
        PasswordToken result = userRepository.getPasswordToken(userId);

        // THEN
        assertNotNull(result);
        assertEquals("some-token", result.getToken());
        verify(querySpec).single();
    }

    @Test
    void getPasswordToken_GenericException_ShouldThrowApiException() {
        // GIVEN
        Long userId = 1L;

        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);

        // On simule une erreur dès le passage des paramètres (ou n'importe où dans la chaîne)
        when(statementSpec.params(anyMap())).thenThrow(new RuntimeException("Database connection failed"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () -> {
            userRepository.getPasswordToken(userId);
        });

        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void getPasswordToken_ByToken_Success() {
        // GIVEN
        String tokenStr = "valid-token";
        PasswordToken expected = new PasswordToken();
        expected.setToken(tokenStr);

        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<PasswordToken> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(eq("token"), eq(tokenStr))).thenReturn(statementSpec);
        when(statementSpec.query(PasswordToken.class)).thenReturn(querySpec);
        when(querySpec.single()).thenReturn(expected);

        // WHEN
        PasswordToken result = userRepository.getPasswordToken(tokenStr);

        // THEN
        assertNotNull(result);
        assertEquals(tokenStr, result.getToken());
    }

    @Test
    void getPasswordToken_ByToken_InvalidLink_ShouldThrowApiException() {
        // GIVEN
        String tokenStr = "invalid-token";
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<PasswordToken> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(PasswordToken.class)).thenReturn(querySpec);

        // Simulation de l'absence de résultat
        when(querySpec.single()).thenThrow(new EmptyResultDataAccessException("No token found", 1));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () ->
                userRepository.getPasswordToken(tokenStr)
        );

        assertEquals("Lien invalide. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void getPasswordToken_ByToken_GenericError_ShouldThrowApiException() {
        // GIVEN
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Database error"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () ->
                userRepository.getPasswordToken("any-token")
        );

        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void updateAccountSettings_Success() {
        // GIVEN
        Long userId = 1L;
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(eq("userId"), eq(userId))).thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(1); // Simule 1 ligne mise à jour

        // WHEN
        userRepository.updateAccountSettings(userId);

        // THEN
        verify(jdbcClient).sql(anyString());
        verify(statementSpec).param("userId", userId);
        verify(statementSpec).update();
    }

    @Test
    void updateAccountSettings_NotFound_ShouldThrowApiException() {
        // GIVEN
        Long userId = 999L;
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);

        // On simule que l'utilisateur n'existe pas au moment de l'update
        when(statementSpec.update()).thenThrow(new EmptyResultDataAccessException("No user found", 1));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () ->
                userRepository.updateAccountSettings(userId)
        );

        assertEquals("Utilisateur introuvable. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void updateAccountSettings_GenericError_ShouldThrowApiException() {
        // GIVEN
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Database error"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () ->
                userRepository.updateAccountSettings(1L)
        );

        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void getAssignee_Success() {
        // GIVEN
        String patientUuid = "patient-123";
        User expectedUser = User.builder().userId(1L).email("doctor@test.com").build();

        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        // Chaque maillon doit renvoyer l'objet suivant
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(eq("patientUuid"), eq(patientUuid))).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);
        when(querySpec.single()).thenReturn(expectedUser);

        // WHEN
        User result = userRepository.getAssignee(patientUuid);

        // THEN
        assertNotNull(result);
        assertEquals("doctor@test.com", result.getEmail());
    }

    @Test
    void getAssignee_NotAssigned_ShouldReturnEmptyUser() {
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);
        when(querySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

        User result = userRepository.getAssignee("any");

        assertNotNull(result);
        assertNull(result.getUserId());
    }

    @Test
    void getAssignee_NotFound_ShouldReturnEmptyUser() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);

        // On force l'exception de résultat vide
        when(querySpec.single()).thenThrow(new EmptyResultDataAccessException("No assignee", 1));

        // WHEN
        User result = userRepository.getAssignee("patient-uuid");

        // THEN
        assertNotNull(result);
        // On vérifie que c'est un User vide (sans ID par exemple)
        assertNull(result.getUserId());
        verify(querySpec).single();
    }

    @Test
    void getAssignee_GenericException_ShouldLogAndThrowApiException() {
        // GIVEN
        String patientUuid = "patient-123";

        // On simule une erreur qui n'est PAS une EmptyResultDataAccessException
        // par exemple une RuntimeException ou une SQLException
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Connexion perdue"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () -> {
            userRepository.getAssignee(patientUuid);
        });

        // On vérifie le message d'erreur du deuxième catch
        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void getCredential_Success() {
        // GIVEN
        String uuid = "user-123";
        Credential expected = new Credential(); // Assurez-vous d'avoir un constructeur ou setters

        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<Credential> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(eq("userUuid"), eq(uuid))).thenReturn(statementSpec);
        when(statementSpec.query(Credential.class)).thenReturn(querySpec);
        when(querySpec.single()).thenReturn(expected);

        // WHEN
        Credential result = userRepository.getCredential(uuid);

        // THEN
        assertNotNull(result);
        verify(querySpec).single();
    }

    @Test
    void getCredential_NotFound_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<Credential> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(Credential.class)).thenReturn(querySpec);

        // Simulation de l'exception Spring pour un résultat unique non trouvé
        when(querySpec.single()).thenThrow(new EmptyResultDataAccessException("No credential found", 1));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () ->
                userRepository.getCredential("unknown-uuid")
        );

        assertEquals("Identifiants introuvables. Veuillez réessayer..", exception.getMessage());
    }

    @Test
    void getCredential_GenericError_ShouldThrowApiException() {
        // GIVEN
        // On casse la chaîne dès le début pour forcer le passage dans le catch générique
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Erreur de connexion base de données"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () ->
                userRepository.getCredential("any-uuid")
        );

        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void getDevices_Success() {
        // GIVEN
        String uuid = "user-123";
        List<Device> expectedDevices = List.of(new Device(), new Device());

        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<Device> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(eq("userUuid"), eq(uuid))).thenReturn(statementSpec);
        when(statementSpec.query(Device.class)).thenReturn(querySpec);
        when(querySpec.list()).thenReturn(expectedDevices);

        // WHEN
        List<Device> result = userRepository.getDevices(uuid);

        // THEN
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(querySpec).list();
    }

    @Test
    void getDevices_NotFound_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<Device> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(Device.class)).thenReturn(querySpec);

        // On force l'exception pour JaCoCo
        when(querySpec.list()).thenThrow(new EmptyResultDataAccessException("No devices found", 1));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () ->
                userRepository.getDevices("unknown-uuid")
        );

        assertEquals("Utilisateur introuvable. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void getDevices_GenericError_ShouldThrowApiException() {
        // GIVEN
        // On simule une erreur brute (ex: SQL syntax error ou timeout)
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("SQL execution failed"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () ->
                userRepository.getDevices("any-uuid")
        );

        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void createPasswordToken_GenericException_ShouldThrowApiException() {
        // GIVEN
        // On jette une erreur dès le début de la chaîne
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Database error"));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> userRepository.createPasswordToken(1L));

        assertEquals("Une erreur s'est produite. Veuillez réessayer", ex.getMessage());
    }

    @Test
    void updateRole_UserNotFound_ShouldThrowApiException() {
        // GIVEN
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<User> querySpec = mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.params(anyMap())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(querySpec);

        // On simule l'absence de résultat
        when(querySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class, () -> userRepository.updateRole("uuid", "role"));
        assertEquals("Utilisateur introuvable. Veuillez réessayer.", ex.getMessage());
    }

    @Test
    void updateRole_GenericException_ShouldThrowApiException() {
        // GIVEN : On jette l'erreur dès le premier appel
        when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Database down"));

        // WHEN & THEN
        ApiException ex = assertThrows(ApiException.class, () -> userRepository.updateRole("uuid", "role"));
        assertEquals("Une erreur s'est produite. Veuillez réessayer.", ex.getMessage());
    }

    @Test
    void updatePassword_Success() {
        // GIVEN
        when(jdbcClient.sql(anyString()).params(anyMap()).update()).thenReturn(1);

        // WHEN & THEN (ne doit pas lever d'exception)
        assertDoesNotThrow(() -> userRepository.updatePassword("uuid", "hashedPwd"));
    }

    @Test
    void createUser_Success() {
        // GIVEN
        String token = "generated-token";
        // On simule le comportement du JdbcClient pour un update
        when(jdbcClient.sql(anyString()).paramSource(any()).update()).thenReturn(1);

        // WHEN
        // Note: Dans ton code, createUser semble utiliser un helper statique pour le token
        String result = userRepository.createUser("John", "Doe", "john@doe.com", "jdoe", "hashed_pass");

        // THEN
        assertNotNull(result);
        verify(jdbcClient).sql(eq(CREATE_USER_STORED_PROCEDURE));
    }

    @Test
    void createUser_DuplicateKey_ShouldThrowApiException() {
        // GIVEN
        when(jdbcClient.sql(anyString()).paramSource(any()).update()).thenThrow(new org.springframework.dao.DuplicateKeyException("Email exists"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.createUser("John", "Doe", "john@doe.com", "jdoe", "pass"));

        assertTrue(exception.getMessage().contains("déjà utilisé"));
    }

    @Test
    void updateUser_Success() {
        // GIVEN
        User updatedUser = new User();
        updatedUser.setEmail("new@email.com");

        when(jdbcClient.sql(anyString()).paramSource(any()).query(User.class).single()).thenReturn(updatedUser);

        // WHEN
        User result = userRepository.updateUser("uuid", "John", "Doe", "new@email.com", "0123", "Bio", "Paris");

        // THEN
        assertEquals("new@email.com", result.getEmail());
    }

    @Test
    void getUserByEmail_Success() {
        // GIVEN
        String email = "test@example.com";
        User expectedUser = new User();
        expectedUser.setEmail(email);

        // Simulation du succès
        when(jdbcClient.sql(anyString()).param(eq("email"), eq(email)).query(User.class).single()).thenReturn(expectedUser);

        // WHEN
        User result = userRepository.getUserByEmail(email);

        // THEN
        assertNotNull(result);
        assertEquals(email, result.getEmail());
    }

    @Test
    void getUserByEmail_NotFound_ShouldThrowApiException() {
        // GIVEN
        String email = "notfound@example.com";

        // Simulation de l'exception Spring quand aucun résultat n'est retourné
        when(jdbcClient.sql(anyString()).param(anyString(), anyString()).query(User.class).single()).thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () -> userRepository.getUserByEmail(email));

        assertTrue(exception.getMessage().contains("Aucun utilisateur trouvé pour l'adresse e-mail"));
    }

    @Test
    void getUserByEmail_GenericException_ShouldThrowApiException() {
        // GIVEN
        // Simulation d'une erreur SQL ou perte de connexion
        when(jdbcClient.sql(anyString())
                .param(anyString(), anyString())
                .query(User.class)
                .single())
                .thenThrow(new RuntimeException("Database down"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.getUserByEmail("any@email.com"));

        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
    }

    @Test
    void getUserById_NotFound_ShouldThrowApiException() {
        // GIVEN
        Long id = 99L;
        // On force le mock à jeter l'exception spécifique attendue
        when(jdbcClient.sql(anyString()).param(eq("id"), eq(id)).query(User.class).single()).thenThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.getUserById(id));

        // Vérifie que le message formaté est correct
        assertEquals("Aucun utilisateur trouvé avec l'ID 99", exception.getMessage());
    }

//    @Test
//    void resetLoginAttempts_Success() {
//        String uuid = "test-uuid";
//        when(jdbcClient.sql(anyString()).param(anyString(), anyString()).update()).thenReturn(1);
//
//        assertDoesNotThrow(() -> userRepository.resetLoginAttempts(uuid));
//    }
//
//    @Test
//    void resetLoginAttempts_NotFound() {
//        String uuid = "unknown";
//        when(jdbcClient.sql(anyString()).param(anyString(), anyString()).update())
//                .thenThrow(new EmptyResultDataAccessException(1));
//
//        ApiException ex = assertThrows(ApiException.class, () -> userRepository.resetLoginAttempts(uuid));
//        assertTrue(ex.getMessage().contains("Aucun utilisateur trouvé"));
//    }
//
//    @Test
//    void setLastLogin_Success() {
//        Long id = 1L;
//        when(jdbcClient.sql(anyString()).param(anyString(), anyLong()).update()).thenReturn(1);
//
//        assertDoesNotThrow(() -> userRepository.setLastLogin(id));
//    }
//
//    @Test
//    void addLoginDevice_Success() {
//        Long userId = 1L;
//        when(jdbcClient.sql(anyString()).params(anyMap()).update()).thenReturn(1);
//
//        assertDoesNotThrow(() -> userRepository.addLoginDevice(userId, "PC", "Chrome", "127.0.0.1"));
//    }
//
//    @Test
//    void addLoginDevice_GenericException() {
//        when(jdbcClient.sql(anyString()).params(anyMap()).update())
//                .thenThrow(new RuntimeException("DB Error"));
//
//        assertThrows(ApiException.class, () -> userRepository.addLoginDevice(1L, "PC", "Chrome", "127.0.0.1"));
//    }

    @Test
    void updateUser_Exception_ShouldThrowApiException() {
        when(jdbcClient.sql(anyString())
                .paramSource(any()))
                .thenThrow(new RuntimeException("Erreur SQL"));

        assertThrows(ApiException.class, () ->
                userRepository.updateUser("uuid", "John", "Doe", "email", "tel", "bio", "addr"));
    }

    @Test
    void updateUser_NotFound() {
        when(jdbcClient.sql(anyString())
                .paramSource(any(SqlParameterSource.class))
                .query(User.class)
                .single())
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThrows(ApiException.class, () ->
                userRepository.updateUser("uuid", "N", "P", "e@e.com", "t", "b", "a"));
    }

    @Test
    void getAccountToken_Success() {
        AccountToken token = new AccountToken();
        when(jdbcClient.sql(anyString()).param(anyString(), anyString()).query(AccountToken.class).single())
                .thenReturn(token);

        assertNotNull(userRepository.getAccountToken("token-123"));
    }

    @Test
    void getAccountToken_NotFound_ShouldThrowApiException() {
        when(jdbcClient.sql(anyString()).param(anyString(), anyString()).query(AccountToken.class).single())
                .thenThrow(new EmptyResultDataAccessException(1));

        ApiException ex = assertThrows(ApiException.class, () -> userRepository.getAccountToken("bad-token"));
        assertTrue(ex.getMessage().contains("Lien invalide"));
    }

    @Test
    void getPasswordToken_ByUserId_ReturnsNullOnEmpty() {
        // Cas spécifique dans votre code : retourne null au lieu de throw
        when(jdbcClient.sql(anyString()).params(anyMap()).query(PasswordToken.class).single())
                .thenThrow(new EmptyResultDataAccessException(1));

        assertNull(userRepository.getPasswordToken(1L));
    }

    @Test
    void toggleAccountLocked_Success() {
        User user = new User();
        when(jdbcClient.sql(anyString()).param(eq("userUuid"), anyString()).query(User.class).single())
                .thenReturn(user);

        User result = userRepository.toggleAccountLocked("uuid");
        assertNotNull(result);
    }

    @Test
    void enableMfa_Success() {
        // Ce test couvre aussi la méthode privée getParamSource(String, String)
        User user = new User();
        when(jdbcClient.sql(anyString()).paramSource(any(SqlParameterSource.class)).query(User.class).single())
                .thenReturn(user);

        User result = userRepository.enableMfa("uuid");
        assertNotNull(result);
    }

    @Test
    void deletePasswordToken_ByUserId_Success() {
        when(jdbcClient.sql(anyString()).param(eq("userId"), anyLong()).update()).thenReturn(1);

        assertDoesNotThrow(() -> userRepository.deletePasswordToken(1L));
    }

    @Test
    void getAssignee_NotFound_ReturnsEmptyUser() {
        // Votre code fait un return User.builder().build() dans le catch
        when(jdbcClient.sql(anyString()).param(anyString(), anyString()).query(User.class).single())
                .thenThrow(new EmptyResultDataAccessException(1));

        User result = userRepository.getAssignee("patient-uuid");
        assertNotNull(result);
        assertNull(result.getUserId()); // Vérifie que l'user est vide
    }

    @Test
    void getPatientUser_Success() {
        User user = new User();
        when(jdbcClient.sql(anyString()).params(anyMap()).query(User.class).single())
                .thenReturn(user);

        assertNotNull(userRepository.getPatientUser("patient-uuid"));
    }

    @Test
    void anyMethod_GenericException_ShouldThrowGenericMessage() {
        when(jdbcClient.sql(anyString()).param(anyString(), anyString()).query((Class<Object>) any()).single())
                .thenThrow(new RuntimeException("Database error"));

        ApiException ex = assertThrows(ApiException.class, () -> userRepository.getPassword("uuid"));
        assertEquals("Une erreur s'est produite. Veuillez réessayer.", ex.getMessage());
    }

//    @Test
//    void updateLoginAttempts_Success() {
//        // GIVEN
//        String email = "test@example.com";
//
//        // On prépare les mocks intermédiaires pour éviter les NullPointerException
//        // et gérer l'enchaînement fluide
//        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
//
//        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
//        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
//        when(statementSpec.update()).thenReturn(1);
//
//        // WHEN
//        userRepository.updateLoginAttempts(email);
//
//        // THEN
//        // Au lieu de verify(jdbcClient).sql(...), on vérifie que update() a bien été appelé
//        // Cela prouve que tout l'enchaînement a eu lieu sans erreur
//        verify(statementSpec, times(1)).update();
//    }
//
//    @Test
//    void updateLoginAttempts_NotFound_ShouldThrowApiException() {
//        // 1. On crée le mock pour le StatementSpec (l'étape intermédiaire)
//        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
//
//        // 2. On définit l'enchaînement :
//        // sql(...) retourne le statementSpec
//        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
//
//        // param(...) doit AUSSI retourner le statementSpec (pour éviter le null)
//        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
//
//        // 3. C'est ici qu'on force l'exception sur l'appel final .update()
//        when(statementSpec.update()).thenThrow(new EmptyResultDataAccessException(1));
//
//        // WHEN & THEN
//        ApiException exception = assertThrows(ApiException.class,
//                () -> userRepository.updateLoginAttempts("unknown@test.com"));
//
//        assertTrue(exception.getMessage().contains("Aucun utilisateur trouvé"));
//    }
//
//    @Test
//    void updateLoginAttempts_GenericError_ShouldThrowApiException() {
//        // GIVEN : On simule une erreur SQL ou réseau imprévue (RuntimeException)
//        when(jdbcClient.sql(anyString())
//                .param(anyString(), any())
//                .update())
//                .thenThrow(new RuntimeException("Database is down"));
//
//        // WHEN & THEN
//        ApiException exception = assertThrows(ApiException.class,
//                () -> userRepository.updateLoginAttempts("email@test.com"));
//
//        // Vérifie que le message correspond à celui du second catch
//        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
//    }
//
//    @Test
//    void setLastLogin_UserNotFound_ShouldThrowApiException() {
//        // GIVEN
//        Long userId = 1L;
//        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
//
//        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
//        when(statementSpec.param(eq("userId"), eq(userId))).thenReturn(statementSpec);
//
//        // On simule que l'update ne trouve rien
//        when(statementSpec.update()).thenThrow(new EmptyResultDataAccessException(1));
//
//        // WHEN & THEN
//        ApiException exception = assertThrows(ApiException.class,
//                () -> userRepository.setLastLogin(userId));
//
//        assertTrue(exception.getMessage().contains("Aucun utilisateur trouvé avec l'ID"));
//    }
//
//    @Test
//    void setLastLogin_GenericError_ShouldThrowApiException() {
//        // GIVEN
//        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
//
//        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
//        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
//
//        // On simule une erreur de base de données (connexion perdue, etc.)
//        when(statementSpec.update()).thenThrow(new RuntimeException("DB Error"));
//
//        // WHEN & THEN
//        ApiException exception = assertThrows(ApiException.class,
//                () -> userRepository.setLastLogin(99L));
//
//        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());
//    }

    @Test
    void createUser_GenericException_ShouldThrowApiException() {
        // GIVEN
        // On mocke l'étape intermédiaire StatementSpec
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);

        // On simule l'enchaînement des appels
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.paramSource(any())).thenReturn(statementSpec);

        // On force update() à jeter une exception quelconque
        when(statementSpec.update()).thenThrow(new RuntimeException("Erreur inattendue"));

        // WHEN & THEN
        ApiException exception = assertThrows(ApiException.class, () ->
                userRepository.createUser("John", "Doe", "john@doe.com", "jdoe", "password")
        );

        // On vérifie que le message correspond bien à celui du second catch
        assertEquals("Une erreur s'est produite. Veuillez réessayer.", exception.getMessage());

        // Optionnel : vérifier que le logger a été appelé si tu as injecté un mock de log
    }

    //  méthode dans UserRepositoryImpl pour la mise à jour
    private SqlParameterSource getParamSource(String userUuid, String firstName, String lastName, String email, String phone, String bio, String address) {
        return new MapSqlParameterSource()
                .addValue("userUuid", userUuid, VARCHAR)
                .addValue("firstName", firstName, VARCHAR)
                .addValue("lastName", lastName, VARCHAR)
                .addValue("email", email.trim().toLowerCase(), VARCHAR)
                .addValue("phone", phone, VARCHAR)
                .addValue("bio", bio, VARCHAR)
                .addValue("address", address, VARCHAR);
    }

}
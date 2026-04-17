package com.openclassrooms.discoveryserverservice.repository;

import com.openclassrooms.discoveryserverservice.exception.ApiException;
import com.openclassrooms.discoveryserverservice.model.User;
import com.openclassrooms.discoveryserverservice.repository.impl.UserRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock
    private JdbcClient jdbc;

    @Mock
    private JdbcClient.StatementSpec statementSpec;
    @Mock
    private JdbcClient.MappedQuerySpec<User> mappedQuerySpec;

    @InjectMocks
    private UserRepositoryImpl userRepository;

    @Test
    void getUserByUsername_ShouldReturnUser_WhenUserExists() {
        String username = "testuser";
        User user = new User();
        user.setUsername(username);

        when(jdbc.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(mappedQuerySpec);
        when(mappedQuerySpec.single()).thenReturn(user);

        // Act
        User result = userRepository.getUserByUsername(username);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
    }

    @Test
    void getUserByUsername_ShouldThrowApiException_WhenUserNotFound() {
        // Arrange
        String username = "testuser";

        when(jdbc.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(mappedQuerySpec);

        when(mappedQuerySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.getUserByUsername(username));

        assertEquals(String.format("Utilisateur non trouvé %s", username), exception.getMessage());
        verify(mappedQuerySpec).single();
    }

    @Test
    void getUserByUsername_ShouldThrowApiException_WhenGenericExceptionOccurs() {
        // Arrange
        String username = "testuser";

        when(jdbc.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(User.class)).thenReturn(mappedQuerySpec);

        // On simule une erreur SQL générique (ex: connexion perdue)
        when(mappedQuerySpec.single()).thenThrow(new RuntimeException("Database down"));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> userRepository.getUserByUsername(username));

        assertEquals("Une erreur est survenue. Veuillez réessayer.", exception.getMessage());
    }
}
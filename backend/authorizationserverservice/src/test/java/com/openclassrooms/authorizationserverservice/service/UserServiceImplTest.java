package com.openclassrooms.authorizationserverservice.service;

import com.openclassrooms.authorizationserverservice.model.User;
import com.openclassrooms.authorizationserverservice.repository.UserRepository;
import com.openclassrooms.authorizationserverservice.service.impl.UserServiceImpl;
import com.openclassrooms.authorizationserverservice.util.UserUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("getUserByEmail doit retourner l'utilisateur correspondant")
    void getUserByEmail_ShouldReturnUser_WhenEmailExists() {
        String email = "test@example.com";
        User mockUser = new User();
        mockUser.setEmail(email);
        when(userRepository.getUserByEmail(email)).thenReturn(mockUser);

        User result = userService.getUserByEmail(email);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        verify(userRepository).getUserByEmail(email);
    }

    @Test
    @DisplayName("resetLoginAttempts doit appeler le repository")
    void resetLoginAttempts_ShouldInvokeRepository() {
        String userId = "user-id-456";
        userService.resetLoginAttempts(userId);
        verify(userRepository).resetLoginAttempts(userId);
    }

    @Test
    @DisplayName("updateLoginAttempts doit appeler le repository")
    void updateLoginAttempts_ShouldInvokeRepository() {
        String email = "kara@example.com";
        userService.updateLoginAttempts(email);
        verify(userRepository).updateLoginAttempts(email);
    }

    @Test
    @DisplayName("setLastLogin doit appeler le repository")
    void setLastLogin_ShouldInvokeRepository() {
        Long userId = 1L;
        userService.setLastLogin(userId);
        verify(userRepository).setLastLogin(userId);
    }

    @Test
    @DisplayName("addLoginDevice doit transmettre les infos au repository")
    void addLoginDevice_ShouldInvokeRepository() {
        userService.addLoginDevice(1L, "MacBook Pro", "Chrome", "192.168.1.1");
        verify(userRepository).addLoginDevice(1L, "MacBook Pro", "Chrome", "192.168.1.1");
    }

    @Test
    @DisplayName("verifyQrCode doit retourner true quand le code est valide")
    void verifyQrCode_ShouldReturnTrue_WhenCodeIsValid() {
        try (MockedStatic<UserUtils> mock = mockStatic(UserUtils.class)) {
            String userId = "uuid-123";
            String code = "123456";
            String secret = "SECRET_123";
            User mockUser = new User();
            mockUser.setQrCodeSecret(secret);

            when(userRepository.getUserByUuid(userId)).thenReturn(mockUser);
            mock.when(() -> UserUtils.verifyCode(secret, code)).thenReturn(true);

            assertThat(userService.verifyQrCode(userId, code)).isTrue();
            verify(userRepository).getUserByUuid(userId);
        }
    }

    @Test
    @DisplayName("verifyQrCode doit retourner false quand le code est invalide")
    void verifyQrCode_ShouldReturnFalse_WhenCodeIsInvalid() {
        try (MockedStatic<UserUtils> mock = mockStatic(UserUtils.class)) {
            String userId = "uuid-123";
            String secret = "SECRET_123";
            User mockUser = new User();
            mockUser.setQrCodeSecret(secret);

            when(userRepository.getUserByUuid(userId)).thenReturn(mockUser);
            mock.when(() -> UserUtils.verifyCode(secret, "000000")).thenReturn(false);

            assertThat(userService.verifyQrCode(userId, "000000")).isFalse();
        }
    }
}
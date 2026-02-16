package com.openclassrooms.discoveryserverservice.security;

import com.openclassrooms.discoveryserverservice.model.User;
import com.openclassrooms.discoveryserverservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DiscoveryUserDetailsService discoveryUserDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .password("password123")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .role("ROLE_USER")
                .authorities("READ_PRIVILEGES,WRITE_PRIVILEGES")
                .build();
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {
        // Arrange
        when(userRepository.getUserByUsername("testuser")).thenReturn(testUser);

        // Act
        UserDetails userDetails = discoveryUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("password123");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();


        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "READ_PRIVILEGES", "WRITE_PRIVILEGES");

        verify(userRepository, times(1)).getUserByUsername("testuser");
    }

    @Test
    void loadUserByUsername_ShouldHandleEmptyAuthorities() {
        // Arrange
        User userWithNoAuthorities = User.builder()
                .username("testuser2")
                .password("password456")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .role("")
                .authorities("")
                .build();

        when(userRepository.getUserByUsername("testuser2")).thenReturn(userWithNoAuthorities);

        // Act
        UserDetails userDetails = discoveryUserDetailsService.loadUserByUsername("testuser2");

        // Assert
        assertThat(userDetails.getAuthorities()).isEmpty();
        verify(userRepository, times(1)).getUserByUsername("testuser2");
    }

    @Test
    void loadUserByUsername_ShouldHandleUserWithDisabledAccount() {
        // Arrange
        User disabledUser = User.builder()
                .username("disableduser")
                .password("password")
                .enabled(false)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .role("ROLE_USER")
                .authorities("READ_PRIVILEGES")
                .build();

        when(userRepository.getUserByUsername("disableduser")).thenReturn(disabledUser);

        // Act
        UserDetails userDetails = discoveryUserDetailsService.loadUserByUsername("disableduser");

        // Assert
        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue(); // Hardcodé à true

        verify(userRepository, times(1)).getUserByUsername("disableduser");
    }

    @Test
    void loadUserByUsername_ShouldHandleExceptionFromRepository() {
        // Arrange
        when(userRepository.getUserByUsername("erroruser"))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThatThrownBy(() -> discoveryUserDetailsService.loadUserByUsername("erroruser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verify(userRepository, times(1)).getUserByUsername("erroruser");
    }
}
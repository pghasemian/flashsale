
package com.flashsale.identity.application;

import com.flashsale.exception.DuplicateResourceException;
import com.flashsale.identity.domain.Role;
import com.flashsale.identity.domain.User;
import com.flashsale.identity.dto.LoginRequest;
import com.flashsale.identity.dto.LoginResponse;
import com.flashsale.identity.dto.RegisterRequest;
import com.flashsale.identity.dto.UserResponse;
import com.flashsale.identity.persistence.RoleRepository;
import com.flashsale.identity.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEncoder jwtEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterUserSuccessfully() {
        // Arrange
        RegisterRequest request =
                new RegisterRequest("Parisa@gmail.com", "password123");

        when(userRepository.existsByEmail("parisa@gmail.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("hashedPassword");

        Role userRole = mock(Role.class);

        when(roleRepository.findByName("USER"))
                .thenReturn(Optional.of(userRole));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("parisa@gmail.com", response.email());

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("hashedPassword", savedUser.getPasswordHash());

        assertTrue(savedUser.getRoles().contains(userRole));

        verify(passwordEncoder).encode("password123");
        verify(roleRepository).findByName("USER");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        // Arrange
        RegisterRequest request =
                new RegisterRequest("parisa@gmail.com", "password123");

        when(userRepository.existsByEmail("parisa@gmail.com"))
                .thenReturn(true);

        // Act & Assert
        assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(request)
        );

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }


    @Test
    void shouldLoginSuccessfully() {
        // Arrange
        User user = mock(User.class);

        when(userRepository.findByEmail("parisa@gmail.com"))
                .thenReturn(Optional.of(user));

        when(user.getPasswordHash()).thenReturn("hashedPassword");

        when(passwordEncoder.matches("password123", "hashedPassword"))
                .thenReturn(true);

        when(user.isEnabled()).thenReturn(true);

        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("parisa@gmail.com");

        Role userRole = mock(Role.class);
        when(userRole.getName()).thenReturn("USER");

        when(user.getRoles()).thenReturn(Set.of(userRole));

        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("mock-jwt-token");

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwt);

        LoginRequest request =
                new LoginRequest("Parisa@gmail.com", "password123");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());

        verify(passwordEncoder).matches("password123", "hashedPassword");
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }


    @Test
    void shouldRejectLoginWithWrongPassword() {
        // Arrange
        User user = mock(User.class);

        when(userRepository.findByEmail("parisa@gmail.com"))
                .thenReturn(Optional.of(user));

        when(user.getPasswordHash()).thenReturn("hashedPassword");

        when(passwordEncoder.matches("wrongPassword", "hashedPassword"))
                .thenReturn(false);

        LoginRequest request =
                new LoginRequest("parisa@gmail.com", "wrongPassword");

        // Act & Assert
        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        verify(jwtEncoder, never())
                .encode(any(JwtEncoderParameters.class));
    }


    @Test
    void shouldRejectLoginForDisabledUser() {
        // Arrange
        User user = mock(User.class);

        when(userRepository.findByEmail("parisa@gmail.com"))
                .thenReturn(Optional.of(user));

        when(user.getPasswordHash()).thenReturn("hashedPassword");

        when(passwordEncoder.matches("password123", "hashedPassword"))
                .thenReturn(true);

        when(user.isEnabled()).thenReturn(false);

        LoginRequest request =
                new LoginRequest("parisa@gmail.com", "password123");

        // Act & Assert
        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        verify(jwtEncoder, never())
                .encode(any(JwtEncoderParameters.class));
    }


}

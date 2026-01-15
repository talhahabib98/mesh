package com.mesh.auth;

import com.mesh.auth.dto.LoginRequest;
import com.mesh.auth.dto.RegisterRequest;
import com.mesh.user.User;
import com.mesh.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerCreatesUserAndReturnsToken() {
        var request = new RegisterRequest("alice", "alice@example.com", "password123");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateToken(any(UUID.class), anyString())).thenReturn("signed-jwt");

        var response = authService.register(request);

        assertThat(response.token()).isEqualTo("signed-jwt");
        assertThat(response.username()).isEqualTo("alice");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerRejectsDuplicateUsername() {
        var request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("username already taken");
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        User existing = User.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hashed-password")
                .createdAt(Instant.now())
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(existing.getId(), "alice")).thenReturn("signed-jwt");

        var response = authService.login(new LoginRequest("alice", "password123"));

        assertThat(response.token()).isEqualTo("signed-jwt");
        assertThat(response.userId()).isEqualTo(existing.getId());
    }

    @Test
    void loginRejectsWrongPassword() {
        User existing = User.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .passwordHash("hashed-password")
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalid username or password");
    }

    @Test
    void loginRejectsUnknownUsername() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "whatever123")))
                .isInstanceOf(ResponseStatusException.class);
    }
}

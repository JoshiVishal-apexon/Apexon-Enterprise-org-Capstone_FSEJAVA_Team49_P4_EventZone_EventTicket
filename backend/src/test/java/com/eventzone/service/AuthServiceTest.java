package com.eventzone.service;

import com.eventzone.dto.auth.AuthResponse;
import com.eventzone.dto.auth.LoginRequest;
import com.eventzone.dto.auth.RegisterRequest;
import com.eventzone.dto.auth.UserResponse;
import com.eventzone.entity.User;
import com.eventzone.exception.ConflictException;
import com.eventzone.exception.UnauthorizedException;
import com.eventzone.repository.UserRepository;
import com.eventzone.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        jwtUtil = Mockito.mock(JwtUtil.class);
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void registerNormalisesEmailAndAssignsAttendeeRole() {
        Mockito.when(userRepository.existsByEmail("u@e.com")).thenReturn(false);
        Mockito.when(passwordEncoder.encode("password123")).thenReturn("hashed");
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = authService.register(new RegisterRequest("  U@E.com ", "password123", "Name", null));

        assertEquals("u@e.com", response.email());
        assertEquals("Name", response.name());
        assertEquals(AuthService.ROLE_ATTENDEE, response.role());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        Mockito.when(userRepository.existsByEmail("u@e.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> authService.register(new RegisterRequest("u@e.com", "password123", "Name", null)));
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = User.builder()
                .email("u@e.com")
                .passwordHash("hashed")
                .role("ATTENDEE")
                .name("Name")
                .build();
        Mockito.when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        Mockito.when(jwtUtil.generateToken("u@e.com", "ATTENDEE")).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("u@e.com", "password123"));

        assertEquals("jwt-token", response.token());
        assertEquals("u@e.com", response.email());
        assertEquals("ATTENDEE", response.role());
        assertEquals("Name", response.name());
    }

    @Test
    void loginRejectsUnknownEmail() {
        Mockito.when(userRepository.findByEmail("missing@e.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("missing@e.com", "password123")));
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = User.builder()
                .email("u@e.com")
                .passwordHash("hashed")
                .role("ATTENDEE")
                .name("Name")
                .build();
        Mockito.when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("u@e.com", "wrong")));
        Mockito.verify(jwtUtil, Mockito.never()).generateToken(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void registerCanSelfAssignOrganiserRole() {
        Mockito.when(userRepository.existsByEmail("org@e.com")).thenReturn(false);
        Mockito.when(passwordEncoder.encode("password123")).thenReturn("hashed");
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response =
                authService.register(new RegisterRequest("org@e.com", "password123", "Org", "ORGANISER"));

        assertEquals(AuthService.ROLE_ORGANISER, response.role());
    }

    @Test
    void registerAcceptsRoleCaseInsensitively() {
        Mockito.when(userRepository.existsByEmail("org2@e.com")).thenReturn(false);
        Mockito.when(passwordEncoder.encode("password123")).thenReturn("hashed");
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response =
                authService.register(new RegisterRequest("org2@e.com", "password123", "Org", " organiser "));

        assertEquals(AuthService.ROLE_ORGANISER, response.role());
    }

    @Test
    void registerDefaultsToAttendeeWhenRoleOmitted() {
        Mockito.when(userRepository.existsByEmail("a@e.com")).thenReturn(false);
        Mockito.when(passwordEncoder.encode("password123")).thenReturn("hashed");
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse blank =
                authService.register(new RegisterRequest("a@e.com", "password123", "A", "   "));

        assertEquals(AuthService.ROLE_ATTENDEE, blank.role());
    }

    @Test
    void registerRefusesToSelfAssignAdmin() {
        Mockito.when(userRepository.existsByEmail("sneaky@e.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                authService.register(new RegisterRequest("sneaky@e.com", "password123", "X", "ADMIN")));
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void registerRejectsUnknownRole() {
        Mockito.when(userRepository.existsByEmail("x@e.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                authService.register(new RegisterRequest("x@e.com", "password123", "X", "SUPERUSER")));
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }
}

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String ROLE_ATTENDEE = "ATTENDEE";
    public static final String ROLE_ORGANISER = "ORGANISER";
    public static final String ROLE_ADMIN = "ADMIN";

    /**
     * Roles a user may claim for themselves at registration. ADMIN is excluded
     * on purpose: it can manage every organiser's events and the category list,
     * so granting it must stay a deliberate act (seeded, or promoted by an
     * existing admin) rather than something any signup can ask for.
     */
    private static final Set<String> SELF_ASSIGNABLE_ROLES = Set.of(ROLE_ATTENDEE, ROLE_ORGANISER);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        log.info("Attempting registration for email={}", email);
        if (userRepository.existsByEmail(email)) {
            log.warn("Registration rejected because account already exists for email={}", email);
            throw new ConflictException("An account with this email already exists");
        }

        String role = resolveSelfAssignableRole(request.role());

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .name(request.name())
                .build();

        User saved = userRepository.save(user);
        log.info("Registration succeeded for email={} userId={} role={}",
                saved.getEmail(), saved.getId(), saved.getRole());
        return new UserResponse(saved.getId(), saved.getEmail(), saved.getName(), saved.getRole());
    }

    /**
     * Normalises and whitelists the requested role. Absent or blank means
     * ATTENDEE, which keeps clients that never send the field working unchanged.
     */
    private String resolveSelfAssignableRole(String requested) {
        if (requested == null || requested.isBlank()) {
            return ROLE_ATTENDEE;
        }
        String role = requested.trim().toUpperCase(Locale.ROOT);
        if (!SELF_ASSIGNABLE_ROLES.contains(role)) {
            log.warn("Registration rejected for unsupported role='{}'", requested);
            throw new IllegalArgumentException(
                    "Role must be either ATTENDEE or ORGANISER");
        }
        return role;
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        log.info("Login attempt received for email={}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login failed: email not found for email={}", email);
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password for email={}", email);
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        log.info("Login succeeded for email={} role={}", user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getName(), user.getRole(), user.getEmail());
    }
}

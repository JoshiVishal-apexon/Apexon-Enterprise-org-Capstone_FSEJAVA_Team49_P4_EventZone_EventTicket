package com.eventzone.service;

import com.eventzone.dto.auth.UserResponse;
import com.eventzone.entity.User;
import com.eventzone.exception.ConflictException;
import com.eventzone.exception.NotFoundException;
import com.eventzone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Admin user management. This is the spec's "ORGANISER accounts are seeded or
 * promoted by ADMIN" path, and the counterweight to self-service organiser
 * signup: an admin can demote an account that should not have been an organiser.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /** Unlike registration, an admin may assign any role, ADMIN included. */
    private static final Set<String> ASSIGNABLE_ROLES =
            Set.of(AuthService.ROLE_ATTENDEE, AuthService.ROLE_ORGANISER, AuthService.ROLE_ADMIN);

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getEmail(), u.getName(), u.getRole()))
                .toList();
    }

    @Transactional
    public UserResponse changeRole(UUID userId, String requestedRole, User actor) {
        String role = normalise(requestedRole);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // An admin demoting themselves would immediately lose access to this very
        // endpoint, potentially leaving the system with no usable admin.
        if (user.getId().equals(actor.getId()) && !AuthService.ROLE_ADMIN.equals(role)) {
            log.warn("Admin {} attempted to demote themselves", actor.getEmail());
            throw new ConflictException("You cannot change your own admin role");
        }

        String previous = user.getRole();
        user.setRole(role);
        User saved = userRepository.save(user);

        log.info("Role changed for email={} from {} to {} by admin={}",
                saved.getEmail(), previous, role, actor.getEmail());
        return new UserResponse(saved.getId(), saved.getEmail(), saved.getName(), saved.getRole());
    }

    private String normalise(String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        String role = requestedRole.trim().toUpperCase(Locale.ROOT);
        if (!ASSIGNABLE_ROLES.contains(role)) {
            throw new IllegalArgumentException("Role must be one of ATTENDEE, ORGANISER, ADMIN");
        }
        return role;
    }
}

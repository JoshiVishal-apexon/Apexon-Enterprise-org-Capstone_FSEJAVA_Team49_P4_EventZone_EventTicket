package com.eventzone.service;

import com.eventzone.dto.auth.UserResponse;
import com.eventzone.entity.User;
import com.eventzone.exception.ConflictException;
import com.eventzone.exception.NotFoundException;
import com.eventzone.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService service;

    private User admin;
    private User attendee;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        service = new UserService(userRepository);

        admin = User.builder().id(UUID.randomUUID()).email("admin@e.com")
                .name("Admin").role("ADMIN").passwordHash("h").build();
        attendee = User.builder().id(UUID.randomUUID()).email("u1@e.com")
                .name("U1").role("ATTENDEE").passwordHash("h").build();

        Mockito.when(userRepository.findById(attendee.getId())).thenReturn(Optional.of(attendee));
        Mockito.when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void adminCanPromoteAttendeeToOrganiser() {
        UserResponse response = service.changeRole(attendee.getId(), "ORGANISER", admin);

        assertEquals("ORGANISER", response.role());
    }

    @Test
    void adminCanDemoteOrganiserBackToAttendee() {
        attendee.setRole("ORGANISER");

        assertEquals("ATTENDEE", service.changeRole(attendee.getId(), "attendee", admin).role());
    }

    @Test
    void adminCanGrantAdmin() {
        assertEquals("ADMIN", service.changeRole(attendee.getId(), "ADMIN", admin).role());
    }

    @Test
    void adminCannotDemoteThemselves() {
        assertThrows(ConflictException.class, () -> service.changeRole(admin.getId(), "ATTENDEE", admin));
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void reassigningOwnAdminRoleIsHarmlessAndAllowed() {
        assertEquals("ADMIN", service.changeRole(admin.getId(), "ADMIN", admin).role());
    }

    @Test
    void unknownRoleIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.changeRole(attendee.getId(), "SUPERUSER", admin));
    }

    @Test
    void blankRoleIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.changeRole(attendee.getId(), "  ", admin));
    }

    @Test
    void unknownUserIsRejected() {
        Mockito.when(userRepository.findById(Mockito.any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.changeRole(UUID.randomUUID(), "ORGANISER", admin));
    }
}

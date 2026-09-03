package com.eventzone.controller;

import com.eventzone.dto.auth.RoleChangeRequest;
import com.eventzone.dto.auth.UserResponse;
import com.eventzone.security.SecurityUtils;
import com.eventzone.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Admin-only user administration. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> list() {
        return userService.list();
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeRole(@PathVariable UUID id,
                                    @Valid @RequestBody RoleChangeRequest request) {
        return userService.changeRole(id, request.role(), SecurityUtils.currentUser());
    }
}

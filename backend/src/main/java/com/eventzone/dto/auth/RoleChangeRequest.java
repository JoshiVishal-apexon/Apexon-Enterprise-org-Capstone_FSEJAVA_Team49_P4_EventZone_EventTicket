package com.eventzone.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RoleChangeRequest(
        @NotBlank(message = "Role is required") String role
) {
}

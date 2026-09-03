package com.eventzone.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
        @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,
        @NotBlank(message = "Name is required") String name,

        /**
         * Optional. "ATTENDEE" (the default when omitted, so existing clients keep
         * working) or "ORGANISER", case-insensitive. ADMIN is deliberately not
         * self-assignable -- see AuthService#resolveSelfAssignableRole.
         */
        String role
) {
}

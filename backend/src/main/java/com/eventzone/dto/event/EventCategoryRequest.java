package com.eventzone.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventCategoryRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 60, message = "Name must be 60 characters or fewer")
        String name
) {
}

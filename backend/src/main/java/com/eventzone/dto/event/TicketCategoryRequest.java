package com.eventzone.dto.event;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record TicketCategoryRequest(
        @NotBlank(message = "Name is required") String name,
        @PositiveOrZero(message = "Price must not be negative") double price,
        @Min(value = 1, message = "Total seats must be at least 1") int totalSeats
) {
}

package com.eventzone.dto.event;

import java.util.UUID;

public record TicketCategoryResponse(
        UUID id,
        String name,
        double price,
        int totalSeats,
        int availableSeats,
        /** Confirmed seats sold, for the organiser's "bookings per ticket category" view. */
        int bookedQuantity
) {
}

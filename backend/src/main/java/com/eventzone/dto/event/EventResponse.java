package com.eventzone.dto.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        LocalDateTime eventDate,
        String venue,
        String coverImageUrl,
        UUID organiserId,
        String organiserName,
        UUID categoryId,
        String categoryName,
        boolean active,
        List<TicketCategoryResponse> ticketCategories
) {
}

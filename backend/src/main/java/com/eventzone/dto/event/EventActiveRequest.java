package com.eventzone.dto.event;

import jakarta.validation.constraints.NotNull;

public record EventActiveRequest(
        @NotNull(message = "active is required") Boolean active
) {
}

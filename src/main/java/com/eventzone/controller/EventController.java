package com.eventzone.controller;

import com.eventzone.dto.common.ErrorResponse;
import com.eventzone.dto.event.EventCreateRequest;
import com.eventzone.dto.event.EventDetailResponse;
import com.eventzone.dto.event.EventSummaryResponse;
import com.eventzone.dto.event.EventUpdateRequest;
import com.eventzone.entity.User;
import com.eventzone.security.SecurityUtils;
import com.eventzone.service.EventService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful response"),
        @ApiResponse(responseCode = "400", description = "Validation or bad request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class EventController {

    private final EventService eventService;

    @GetMapping
    public List<EventSummaryResponse> list(@RequestParam(required = false) String category) {
        return eventService.listActive(category);
    }

    @GetMapping("/{id}")
    public EventDetailResponse getById(@PathVariable UUID id) {
        return eventService.getDetail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ORGANISER')")
    public EventDetailResponse create(@Valid @RequestBody EventCreateRequest request) {
        User currentUser = SecurityUtils.currentUser();
        return eventService.create(request, currentUser);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANISER') or hasRole('ADMIN')")
    public EventDetailResponse update(@PathVariable UUID id, @Valid @RequestBody EventUpdateRequest request) {
        User currentUser = SecurityUtils.currentUser();
        return eventService.update(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANISER') or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        User currentUser = SecurityUtils.currentUser();
        eventService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}

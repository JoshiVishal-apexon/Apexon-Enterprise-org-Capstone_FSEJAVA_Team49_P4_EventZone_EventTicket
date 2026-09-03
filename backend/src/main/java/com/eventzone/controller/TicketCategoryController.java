package com.eventzone.controller;

import com.eventzone.dto.event.TicketCategoryRequest;
import com.eventzone.dto.event.TicketCategoryResponse;
import com.eventzone.security.SecurityUtils;
import com.eventzone.service.TicketCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Ticket categories are created under their event but addressed directly for
 * update/delete, so this controller spans both paths rather than sitting under
 * a single @RequestMapping.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TicketCategoryController {

    private final TicketCategoryService ticketCategoryService;

    @GetMapping("/api/events/{eventId}/ticket-categories")
    public List<TicketCategoryResponse> list(@PathVariable UUID eventId) {
        return ticketCategoryService.listForEvent(eventId);
    }

    @PostMapping("/api/events/{eventId}/ticket-categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public TicketCategoryResponse add(@PathVariable UUID eventId,
                                       @Valid @RequestBody TicketCategoryRequest request) {
        return ticketCategoryService.add(eventId, request, SecurityUtils.currentUser());
    }

    @PutMapping("/api/ticket-categories/{id}")
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public TicketCategoryResponse update(@PathVariable UUID id,
                                          @Valid @RequestBody TicketCategoryRequest request) {
        return ticketCategoryService.update(id, request, SecurityUtils.currentUser());
    }

    @DeleteMapping("/api/ticket-categories/{id}")
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ticketCategoryService.delete(id, SecurityUtils.currentUser());
        return ResponseEntity.noContent().build();
    }
}

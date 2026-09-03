package com.eventzone.controller;

import com.eventzone.dto.event.EventActiveRequest;
import com.eventzone.dto.event.EventRequest;
import com.eventzone.dto.event.EventResponse;
import com.eventzone.security.SecurityUtils;
import com.eventzone.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // Public reads -----------------------------------------------------------

    @GetMapping
    public List<EventResponse> list(@RequestParam(required = false) String category) {
        return eventService.listAll(category);
    }

    /**
     * Declared before {@code /{id}} so the literal path wins; Spring matches
     * exact segments ahead of path variables, which also keeps "mine" from
     * being parsed as a UUID.
     */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public List<EventResponse> mine() {
        return eventService.listMine(SecurityUtils.currentUser());
    }

    /** Admin panel view: includes events that have been deactivated. */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<EventResponse> all() {
        return eventService.listAllIncludingInactive();
    }

    @GetMapping("/{id}")
    public EventResponse detail(@PathVariable UUID id) {
        return eventService.getById(id);
    }

    // Organiser writes -------------------------------------------------------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public EventResponse create(@Valid @RequestBody EventRequest request) {
        return eventService.create(request, SecurityUtils.currentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public EventResponse update(@PathVariable UUID id, @Valid @RequestBody EventRequest request) {
        return eventService.update(id, request, SecurityUtils.currentUser());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        eventService.delete(id, SecurityUtils.currentUser());
        return ResponseEntity.noContent().build();
    }

    // Admin ------------------------------------------------------------------

    /** Activate / deactivate an event without touching the rest of its fields. */
    @PutMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public EventResponse setActive(@PathVariable UUID id,
                                    @Valid @RequestBody EventActiveRequest request) {
        return eventService.setActive(id, request.active());
    }
}

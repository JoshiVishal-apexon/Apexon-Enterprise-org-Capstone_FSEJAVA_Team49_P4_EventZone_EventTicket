package com.eventzone.service;

import com.eventzone.dto.event.EventCategoryRequest;
import com.eventzone.dto.event.EventCategoryResponse;
import com.eventzone.exception.ConflictException;
import com.eventzone.exception.NotFoundException;
import com.eventzone.model.EventCategory;
import com.eventzone.repository.EventCategoryRepository;
import com.eventzone.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Admin category management. Reads are public (the organiser's event form needs
 * the list); writes are restricted to ADMIN by @PreAuthorize on the controller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final EventCategoryRepository eventCategoryRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<EventCategoryResponse> list() {
        return eventCategoryRepository.findAll().stream()
                .map(c -> new EventCategoryResponse(c.getId(), c.getName()))
                .toList();
    }

    @Transactional
    public EventCategoryResponse create(EventCategoryRequest request) {
        String name = request.name().trim();
        requireNameAvailable(name, null);

        EventCategory category = new EventCategory();
        category.setName(name);
        EventCategory saved = eventCategoryRepository.save(category);

        log.info("Event category created id={} name='{}'", saved.getId(), saved.getName());
        return new EventCategoryResponse(saved.getId(), saved.getName());
    }

    @Transactional
    public EventCategoryResponse rename(UUID id, EventCategoryRequest request) {
        EventCategory category = find(id);
        String name = request.name().trim();
        requireNameAvailable(name, id);

        category.setName(name);
        EventCategory saved = eventCategoryRepository.save(category);

        log.info("Event category renamed id={} name='{}'", id, saved.getName());
        return new EventCategoryResponse(saved.getId(), saved.getName());
    }

    @Transactional
    public void delete(UUID id) {
        EventCategory category = find(id);

        // Event.category is a required association on existing rows; removing a
        // category still in use would break those events, so refuse up front
        // rather than surfacing a constraint violation.
        if (eventRepository.existsByCategory_Id(id)) {
            log.warn("Refusing to delete category id={} because events still reference it", id);
            throw new ConflictException("This category is in use by one or more events");
        }

        eventCategoryRepository.delete(category);
        log.info("Event category deleted id={}", id);
    }

    private EventCategory find(UUID id) {
        return eventCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event category not found"));
    }

    /** The name column is unique; check explicitly so the caller gets a 409, not a 500. */
    private void requireNameAvailable(String name, UUID allowedId) {
        eventCategoryRepository.findByName(name).ifPresent(existing -> {
            if (allowedId == null || !existing.getId().equals(allowedId)) {
                throw new ConflictException("A category named '" + name + "' already exists");
            }
        });
    }
}

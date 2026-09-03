package com.eventzone.service;

import com.eventzone.dto.event.EventRequest;
import com.eventzone.dto.event.EventResponse;
import com.eventzone.dto.event.TicketCategoryResponse;
import com.eventzone.entity.User;
import com.eventzone.exception.ConflictException;
import com.eventzone.exception.ForbiddenException;
import com.eventzone.exception.NotFoundException;
import com.eventzone.model.Event;
import com.eventzone.model.EventCategory;
import com.eventzone.model.TicketCategory;
import com.eventzone.repository.BookingRepository;
import com.eventzone.repository.EventCategoryRepository;
import com.eventzone.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Organiser-facing event management. Read paths stay public; every write goes
 * through {@link #requireCanManage} so an organiser can only touch events they
 * own, while an ADMIN may manage any.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    public static final String ROLE_ADMIN = "ADMIN";

    private final EventRepository eventRepository;
    private final EventCategoryRepository eventCategoryRepository;
    private final BookingRepository bookingRepository;

    // Reads ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<EventResponse> listAll(String categoryName) {
        // Public browse: deactivated events are hidden. Organisers still see
        // their own via listMine, and admins via listAllIncludingInactive.
        List<Event> events = (categoryName == null || categoryName.isBlank())
                ? eventRepository.findByActiveTrue()
                : eventRepository.findByCategory_NameAndActiveTrue(categoryName);
        return events.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getById(UUID id) {
        return toResponse(findEvent(id));
    }

    /** Every event regardless of active flag, for the admin panel. */
    @Transactional(readOnly = true)
    public List<EventResponse> listAllIncludingInactive() {
        return eventRepository.findAll().stream().map(this::toResponse).toList();
    }

    /** Events owned by the caller, for the organiser dashboard. */
    @Transactional(readOnly = true)
    public List<EventResponse> listMine(User organiser) {
        return eventRepository.findByOrganiser_Id(organiser.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    // Writes -----------------------------------------------------------------

    @Transactional
    public EventResponse create(EventRequest request, User organiser) {
        EventCategory category = findCategory(request.categoryId());

        Event event = new Event();
        apply(event, request, category);
        event.setOrganiser(organiser);

        Event saved = eventRepository.save(event);
        log.info("Event created id={} title='{}' organiser={}",
                saved.getId(), saved.getTitle(), organiser.getEmail());
        return toResponse(saved);
    }

    @Transactional
    public EventResponse update(UUID id, EventRequest request, User actor) {
        Event event = findEvent(id);
        requireCanManage(event, actor);

        EventCategory category = findCategory(request.categoryId());
        apply(event, request, category);

        Event saved = eventRepository.save(event);
        log.info("Event updated id={} by={}", saved.getId(), actor.getEmail());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id, User actor) {
        Event event = findEvent(id);
        requireCanManage(event, actor);

        // Ticket categories cascade with the event, but bookings reference those
        // rows. Removing the event would orphan paid bookings, so refuse instead
        // of letting the delete fail deep in the persistence layer.
        if (bookingRepository.existsByTicketCategory_Event_Id(id)) {
            log.warn("Refusing to delete event id={} because bookings exist against it", id);
            throw new ConflictException("This event has bookings and cannot be deleted");
        }

        eventRepository.delete(event);
        log.info("Event deleted id={} by={}", id, actor.getEmail());
    }

    /**
     * Admin activate/deactivate. Deliberately separate from {@link #update} so
     * it needs no full event payload and is gated on ADMIN at the controller.
     */
    @Transactional
    public EventResponse setActive(UUID id, boolean active) {
        Event event = findEvent(id);
        event.setActive(active);
        Event saved = eventRepository.save(event);
        log.info("Event id={} {} by admin", id, active ? "activated" : "deactivated");
        return toResponse(saved);
    }

    // Helpers ----------------------------------------------------------------

    Event findEvent(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    /**
     * An organiser may manage only their own events; an ADMIN may manage any.
     * Throws rather than returning a boolean so callers cannot forget to check.
     */
    void requireCanManage(Event event, User actor) {
        if (ROLE_ADMIN.equals(actor.getRole())) {
            return;
        }
        User organiser = event.getOrganiser();
        if (organiser == null || !organiser.getId().equals(actor.getId())) {
            log.warn("User {} attempted to manage event id={} they do not own", actor.getEmail(), event.getId());
            throw new ForbiddenException("You can only manage your own events");
        }
    }

    private EventCategory findCategory(UUID categoryId) {
        return eventCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Event category not found"));
    }

    private void apply(Event event, EventRequest request, EventCategory category) {
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventDate(request.eventDate());
        event.setVenue(request.venue());
        event.setCoverImageUrl(request.coverImageUrl());
        event.setCategory(category);
    }

    EventResponse toResponse(Event event) {
        List<TicketCategory> tickets = event.getTicketCategories() == null
                ? Collections.emptyList()
                : event.getTicketCategories();

        List<TicketCategoryResponse> ticketResponses = tickets.stream()
                .map(t -> new TicketCategoryResponse(
                        t.getId(), t.getName(), t.getPrice(), t.getTotalSeats(), t.getAvailableSeats(),
                        bookingRepository.sumConfirmedQuantityByTicketCategoryId(t.getId())))
                .toList();

        User organiser = event.getOrganiser();
        EventCategory category = event.getCategory();

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getVenue(),
                event.getCoverImageUrl(),
                organiser == null ? null : organiser.getId(),
                organiser == null ? null : organiser.getName(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                event.isActive(),
                ticketResponses);
    }
}

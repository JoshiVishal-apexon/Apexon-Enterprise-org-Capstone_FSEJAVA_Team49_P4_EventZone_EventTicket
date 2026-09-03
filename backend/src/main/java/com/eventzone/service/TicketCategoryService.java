package com.eventzone.service;

import com.eventzone.dto.event.TicketCategoryRequest;
import com.eventzone.dto.event.TicketCategoryResponse;
import com.eventzone.entity.User;
import com.eventzone.exception.ConflictException;
import com.eventzone.exception.NotFoundException;
import com.eventzone.model.Event;
import com.eventzone.model.TicketCategory;
import com.eventzone.repository.BookingRepository;
import com.eventzone.repository.TicketCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Ticket categories always belong to an event, so every operation here
 * authorises against the parent event's owner via {@link EventService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCategoryService {

    private final TicketCategoryRepository ticketCategoryRepository;
    private final BookingRepository bookingRepository;
    private final EventService eventService;

    @Transactional(readOnly = true)
    public List<TicketCategoryResponse> listForEvent(UUID eventId) {
        eventService.findEvent(eventId); // 404 rather than an empty list for an unknown event
        return ticketCategoryRepository.findByEvent_Id(eventId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TicketCategoryResponse add(UUID eventId, TicketCategoryRequest request, User actor) {
        Event event = eventService.findEvent(eventId);
        eventService.requireCanManage(event, actor);

        TicketCategory ticket = new TicketCategory();
        ticket.setEvent(event);
        ticket.setName(request.name());
        ticket.setPrice(request.price());
        ticket.setTotalSeats(request.totalSeats());
        // Nothing sold yet, so the whole allocation is available.
        ticket.setAvailableSeats(request.totalSeats());

        TicketCategory saved = ticketCategoryRepository.save(ticket);
        log.info("Ticket category created id={} name='{}' event={} by={}",
                saved.getId(), saved.getName(), eventId, actor.getEmail());
        return toResponse(saved);
    }

    @Transactional
    public TicketCategoryResponse update(UUID id, TicketCategoryRequest request, User actor) {
        TicketCategory ticket = findTicket(id);
        eventService.requireCanManage(ticket.getEvent(), actor);

        // Seats already sold must survive a capacity change, so derive them from
        // the current state and re-apply against the new total. Writing
        // availableSeats = totalSeats here would silently resurrect sold seats.
        int sold = ticket.getTotalSeats() - ticket.getAvailableSeats();
        int newAvailable = request.totalSeats() - sold;
        if (newAvailable < 0) {
            log.warn("Rejecting capacity {} for ticket category id={}: {} seats already sold",
                    request.totalSeats(), id, sold);
            throw new ConflictException(
                    "Total seats cannot be less than the " + sold + " seat(s) already booked");
        }

        ticket.setName(request.name());
        ticket.setPrice(request.price());
        ticket.setTotalSeats(request.totalSeats());
        ticket.setAvailableSeats(newAvailable);

        TicketCategory saved = ticketCategoryRepository.save(ticket);
        log.info("Ticket category updated id={} by={}", id, actor.getEmail());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id, User actor) {
        TicketCategory ticket = findTicket(id);
        eventService.requireCanManage(ticket.getEvent(), actor);

        if (bookingRepository.existsByTicketCategory_Id(id)) {
            log.warn("Refusing to delete ticket category id={} because bookings reference it", id);
            throw new ConflictException("This ticket category has bookings and cannot be deleted");
        }

        ticketCategoryRepository.delete(ticket);
        log.info("Ticket category deleted id={} by={}", id, actor.getEmail());
    }

    private TicketCategory findTicket(UUID id) {
        return ticketCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket category not found"));
    }

    private TicketCategoryResponse toResponse(TicketCategory t) {
        return new TicketCategoryResponse(
                t.getId(), t.getName(), t.getPrice(), t.getTotalSeats(), t.getAvailableSeats(),
                bookingRepository.sumConfirmedQuantityByTicketCategoryId(t.getId()));
    }
}

package com.eventzone.service;

import com.eventzone.entity.User;
import com.eventzone.exception.ConflictException;
import com.eventzone.exception.ForbiddenException;
import com.eventzone.exception.NotFoundException;
import com.eventzone.model.Booking;
import com.eventzone.model.TicketCategory;
import com.eventzone.repository.BookingRepository;
import com.eventzone.repository.TicketCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Booking rules, moved out of the controller so seat accounting lives in one
 * transactional place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    /** Spec caps a single booking at 1-5 tickets. */
    private static final int MAX_QUANTITY = 5;

    private final BookingRepository bookingRepository;
    private final TicketCategoryRepository ticketCategoryRepository;

    @Transactional
    public Booking book(Map<String, Object> body, User user) {
        UUID ticketCategoryId = parseTicketCategoryId(body.get("ticketCategoryId"));
        int quantity = parseQuantity(body.get("quantity"));

        TicketCategory ticket = ticketCategoryRepository.findById(ticketCategoryId)
                .orElseThrow(() -> new NotFoundException("Ticket category not found"));

        if (ticket.getAvailableSeats() < quantity) {
            log.warn("Booking rejected: {} seats requested but {} available for ticket category {}",
                    quantity, ticket.getAvailableSeats(), ticketCategoryId);
            throw new ConflictException("Only " + ticket.getAvailableSeats() + " seat(s) remain");
        }

        ticket.setAvailableSeats(ticket.getAvailableSeats() - quantity);
        ticketCategoryRepository.save(ticket);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTicketCategory(ticket);
        booking.setQuantity(quantity);
        Booking saved = bookingRepository.save(booking);

        log.info("Booking created ref={} user={} ticketCategory={} qty={}",
                saved.getBookingRef(), user.getEmail(), ticketCategoryId, quantity);
        return saved;
    }

    @Transactional
    public void cancel(UUID bookingId, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getUser() == null || !booking.getUser().getId().equals(user.getId())) {
            log.warn("User {} attempted to cancel booking {} they do not own", user.getEmail(), bookingId);
            throw new ForbiddenException("You can only cancel your own bookings");
        }

        // Without this guard a repeated cancel would restore the seats again on
        // every call, pushing availableSeats above totalSeats.
        if (STATUS_CANCELLED.equals(booking.getStatus())) {
            log.warn("Booking {} is already cancelled; refusing to restore seats twice", bookingId);
            throw new ConflictException("This booking is already cancelled");
        }

        booking.setStatus(STATUS_CANCELLED);
        bookingRepository.save(booking);

        TicketCategory ticket = booking.getTicketCategory();
        ticket.setAvailableSeats(ticket.getAvailableSeats() + booking.getQuantity());
        ticketCategoryRepository.save(ticket);

        log.info("Booking cancelled ref={} user={} seats restored={}",
                booking.getBookingRef(), user.getEmail(), booking.getQuantity());
    }

    private UUID parseTicketCategoryId(Object raw) {
        if (raw == null) {
            throw new IllegalArgumentException("ticketCategoryId is required");
        }
        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid ticketCategoryId");
        }
    }

    private int parseQuantity(Object raw) {
        int quantity = raw == null ? 1 : Integer.parseInt(raw.toString());
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be between 1 and " + MAX_QUANTITY);
        }
        return quantity;
    }
}

package com.eventzone.service;

import com.eventzone.entity.User;
import com.eventzone.exception.ConflictException;
import com.eventzone.exception.ForbiddenException;
import com.eventzone.exception.NotFoundException;
import com.eventzone.model.Booking;
import com.eventzone.model.TicketCategory;
import com.eventzone.repository.BookingRepository;
import com.eventzone.repository.TicketCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BookingServiceTest {

    private BookingRepository bookingRepository;
    private TicketCategoryRepository ticketCategoryRepository;
    private BookingService service;

    private final UUID ticketId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();
    private User attendee;
    private User otherAttendee;

    @BeforeEach
    void setUp() {
        bookingRepository = Mockito.mock(BookingRepository.class);
        ticketCategoryRepository = Mockito.mock(TicketCategoryRepository.class);
        service = new BookingService(bookingRepository, ticketCategoryRepository);

        attendee = User.builder().id(UUID.randomUUID()).email("u1@e.com")
                .name("U1").role("ATTENDEE").passwordHash("h").build();
        otherAttendee = User.builder().id(UUID.randomUUID()).email("u2@e.com")
                .name("U2").role("ATTENDEE").passwordHash("h").build();

        Mockito.when(bookingRepository.save(Mockito.any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        Mockito.when(ticketCategoryRepository.save(Mockito.any(TicketCategory.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    private TicketCategory ticket(int total, int available) {
        TicketCategory t = new TicketCategory();
        t.setName("General");
        t.setTotalSeats(total);
        t.setAvailableSeats(available);
        Mockito.when(ticketCategoryRepository.findById(ticketId)).thenReturn(Optional.of(t));
        return t;
    }

    private Booking booking(TicketCategory t, User owner, int qty, String status) {
        Booking b = new Booking();
        b.setUser(owner);
        b.setTicketCategory(t);
        b.setQuantity(qty);
        b.setStatus(status);
        Mockito.when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));
        return b;
    }

    @Test
    void bookingDeductsSeats() {
        TicketCategory t = ticket(200, 200);

        service.book(Map.of("ticketCategoryId", ticketId.toString(), "quantity", 3), attendee);

        assertEquals(197, t.getAvailableSeats());
    }

    @Test
    void bookingDefaultsToOneSeat() {
        TicketCategory t = ticket(10, 10);

        service.book(Map.of("ticketCategoryId", ticketId.toString()), attendee);

        assertEquals(9, t.getAvailableSeats());
    }

    @Test
    void bookingBeyondAvailabilityIsRejected() {
        TicketCategory t = ticket(200, 2);

        assertThrows(ConflictException.class,
                () -> service.book(Map.of("ticketCategoryId", ticketId.toString(), "quantity", 3), attendee));
        assertEquals(2, t.getAvailableSeats(), "seats must be untouched on failure");
    }

    @Test
    void quantityAboveSpecMaximumIsRejected() {
        ticket(200, 200);

        assertThrows(IllegalArgumentException.class,
                () -> service.book(Map.of("ticketCategoryId", ticketId.toString(), "quantity", 6), attendee));
    }

    @Test
    void bookingUnknownTicketCategoryIsRejected() {
        Mockito.when(ticketCategoryRepository.findById(Mockito.any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.book(Map.of("ticketCategoryId", UUID.randomUUID().toString()), attendee));
    }

    @Test
    void cancelRestoresSeats() {
        TicketCategory t = ticket(200, 195);
        Booking b = booking(t, attendee, 5, BookingService.STATUS_CONFIRMED);

        service.cancel(bookingId, attendee);

        assertEquals(200, t.getAvailableSeats());
        assertEquals(BookingService.STATUS_CANCELLED, b.getStatus());
    }

    @Test
    void cancellingTwiceDoesNotRestoreSeatsTwice() {
        TicketCategory t = ticket(200, 200);
        booking(t, attendee, 5, BookingService.STATUS_CANCELLED);

        assertThrows(ConflictException.class, () -> service.cancel(bookingId, attendee));
        assertEquals(200, t.getAvailableSeats(), "availableSeats must not exceed totalSeats");
    }

    @Test
    void cancellingSomeoneElsesBookingIsRejected() {
        TicketCategory t = ticket(200, 195);
        booking(t, attendee, 5, BookingService.STATUS_CONFIRMED);

        assertThrows(ForbiddenException.class, () -> service.cancel(bookingId, otherAttendee));
        assertEquals(195, t.getAvailableSeats());
    }
}

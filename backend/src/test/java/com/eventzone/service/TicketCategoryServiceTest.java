package com.eventzone.service;

import com.eventzone.dto.event.TicketCategoryRequest;
import com.eventzone.dto.event.TicketCategoryResponse;
import com.eventzone.entity.User;
import com.eventzone.exception.ConflictException;
import com.eventzone.exception.ForbiddenException;
import com.eventzone.model.Event;
import com.eventzone.model.TicketCategory;
import com.eventzone.repository.BookingRepository;
import com.eventzone.repository.TicketCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TicketCategoryServiceTest {

    private TicketCategoryRepository ticketCategoryRepository;
    private BookingRepository bookingRepository;
    private com.eventzone.repository.EventRepository eventRepository;
    private EventService eventService;
    private TicketCategoryService service;

    private final UUID eventId = UUID.randomUUID();
    private final UUID ticketId = UUID.randomUUID();
    private User organiser;
    private User otherOrganiser;
    private Event event;

    @BeforeEach
    void setUp() {
        ticketCategoryRepository = Mockito.mock(TicketCategoryRepository.class);
        bookingRepository = Mockito.mock(BookingRepository.class);
        eventRepository = Mockito.mock(com.eventzone.repository.EventRepository.class);
        // A real EventService over mocked repositories, so the ownership rule
        // exercised here is the same code path the controllers use.
        eventService = new EventService(
                eventRepository,
                Mockito.mock(com.eventzone.repository.EventCategoryRepository.class),
                bookingRepository);
        service = new TicketCategoryService(ticketCategoryRepository, bookingRepository, eventService);

        organiser = User.builder().id(UUID.randomUUID()).email("org1@e.com")
                .name("Org One").role("ORGANISER").passwordHash("h").build();
        otherOrganiser = User.builder().id(UUID.randomUUID()).email("org2@e.com")
                .name("Org Two").role("ORGANISER").passwordHash("h").build();

        event = new Event();
        event.setOrganiser(organiser);

        Mockito.when(ticketCategoryRepository.save(Mockito.any(TicketCategory.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    private TicketCategory existingTicket(int totalSeats, int availableSeats) {
        TicketCategory ticket = new TicketCategory();
        ticket.setEvent(event);
        ticket.setName("General");
        ticket.setPrice(999.0);
        ticket.setTotalSeats(totalSeats);
        ticket.setAvailableSeats(availableSeats);
        Mockito.when(ticketCategoryRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        return ticket;
    }

    @Test
    void addStartsWithEverySeatAvailable() {
        Mockito.when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        TicketCategoryResponse response =
                service.add(eventId, new TicketCategoryRequest("VIP", 2499.0, 50), organiser);

        assertEquals("VIP", response.name());
        assertEquals(50, response.totalSeats());
        assertEquals(50, response.availableSeats());
    }

    @Test
    void addIsRefusedForAnotherOrganisersEvent() {
        Mockito.when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThrows(ForbiddenException.class,
                () -> service.add(eventId, new TicketCategoryRequest("VIP", 10.0, 5), otherOrganiser));
        Mockito.verify(ticketCategoryRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void raisingCapacityKeepsSoldSeatsSold() {
        existingTicket(200, 195); // 5 sold

        TicketCategoryResponse response =
                service.update(ticketId, new TicketCategoryRequest("General", 999.0, 300), organiser);

        assertEquals(300, response.totalSeats());
        assertEquals(295, response.availableSeats(), "5 sold seats must stay sold");
    }

    @Test
    void loweringCapacityKeepsSoldSeatsSold() {
        existingTicket(200, 195); // 5 sold

        TicketCategoryResponse response =
                service.update(ticketId, new TicketCategoryRequest("General", 100.0, 100), organiser);

        assertEquals(100, response.totalSeats());
        assertEquals(95, response.availableSeats());
    }

    @Test
    void capacityBelowSoldSeatsIsRejected() {
        existingTicket(200, 195); // 5 sold

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.update(ticketId, new TicketCategoryRequest("General", 999.0, 3), organiser));
        assertTrue(ex.getMessage().contains("5"));
        Mockito.verify(ticketCategoryRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void deleteIsRefusedWhenBookingsReferenceTheCategory() {
        existingTicket(200, 195);
        Mockito.when(bookingRepository.existsByTicketCategory_Id(ticketId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete(ticketId, organiser));
        Mockito.verify(ticketCategoryRepository, Mockito.never()).delete(Mockito.any());
    }

    @Test
    void deleteSucceedsWhenUnbooked() {
        TicketCategory ticket = existingTicket(50, 50);
        Mockito.when(bookingRepository.existsByTicketCategory_Id(ticketId)).thenReturn(false);

        service.delete(ticketId, organiser);

        Mockito.verify(ticketCategoryRepository).delete(ticket);
    }
}

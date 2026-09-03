package com.eventzone.service;

import com.eventzone.dto.event.EventRequest;
import com.eventzone.dto.event.EventResponse;
import com.eventzone.entity.User;
import com.eventzone.exception.ConflictException;
import com.eventzone.exception.ForbiddenException;
import com.eventzone.exception.NotFoundException;
import com.eventzone.model.Event;
import com.eventzone.model.EventCategory;
import com.eventzone.repository.BookingRepository;
import com.eventzone.repository.EventCategoryRepository;
import com.eventzone.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventServiceTest {

    private EventRepository eventRepository;
    private EventCategoryRepository eventCategoryRepository;
    private BookingRepository bookingRepository;
    private EventService eventService;

    private final UUID categoryId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private EventCategory category;
    private User organiser;
    private User otherOrganiser;
    private User admin;

    @BeforeEach
    void setUp() {
        eventRepository = Mockito.mock(EventRepository.class);
        eventCategoryRepository = Mockito.mock(EventCategoryRepository.class);
        bookingRepository = Mockito.mock(BookingRepository.class);
        eventService = new EventService(eventRepository, eventCategoryRepository, bookingRepository);

        category = new EventCategory();
        category.setName("Concert");

        organiser = User.builder().id(UUID.randomUUID()).email("org1@e.com")
                .name("Org One").role("ORGANISER").passwordHash("h").build();
        otherOrganiser = User.builder().id(UUID.randomUUID()).email("org2@e.com")
                .name("Org Two").role("ORGANISER").passwordHash("h").build();
        admin = User.builder().id(UUID.randomUUID()).email("admin@e.com")
                .name("Admin").role("ADMIN").passwordHash("h").build();

        Mockito.when(eventCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        Mockito.when(eventRepository.save(Mockito.any(Event.class))).thenAnswer(i -> i.getArgument(0));
    }

    private EventRequest request() {
        return new EventRequest("Rock Night", "Live rock", LocalDateTime.of(2025, 11, 15, 19, 0),
                "HICC", "https://img", categoryId);
    }

    private Event existingEventOwnedBy(User owner) {
        Event event = new Event();
        event.setTitle("Old title");
        event.setOrganiser(owner);
        event.setCategory(category);
        Mockito.when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        return event;
    }

    @Test
    void createAssignsCallerAsOrganiser() {
        EventResponse response = eventService.create(request(), organiser);

        assertEquals("Rock Night", response.title());
        assertEquals(organiser.getId(), response.organiserId());
        assertEquals("Concert", response.categoryName());
    }

    @Test
    void createRejectsUnknownCategory() {
        EventRequest bad = new EventRequest("T", "D", LocalDateTime.now(), "V", null, UUID.randomUUID());

        assertThrows(NotFoundException.class, () -> eventService.create(bad, organiser));
    }

    @Test
    void ownerCanUpdateOwnEvent() {
        existingEventOwnedBy(organiser);

        EventResponse response = eventService.update(eventId, request(), organiser);

        assertEquals("Rock Night", response.title());
    }

    @Test
    void organiserCannotUpdateSomeoneElsesEvent() {
        existingEventOwnedBy(organiser);

        assertThrows(ForbiddenException.class,
                () -> eventService.update(eventId, request(), otherOrganiser));
        Mockito.verify(eventRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void adminCanUpdateAnyEvent() {
        existingEventOwnedBy(organiser);

        assertEquals("Rock Night", eventService.update(eventId, request(), admin).title());
    }

    @Test
    void organiserCannotDeleteSomeoneElsesEvent() {
        existingEventOwnedBy(organiser);

        assertThrows(ForbiddenException.class, () -> eventService.delete(eventId, otherOrganiser));
        Mockito.verify(eventRepository, Mockito.never()).delete(Mockito.any());
    }

    @Test
    void deleteIsRefusedWhenBookingsExist() {
        existingEventOwnedBy(organiser);
        Mockito.when(bookingRepository.existsByTicketCategory_Event_Id(eventId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> eventService.delete(eventId, organiser));
        Mockito.verify(eventRepository, Mockito.never()).delete(Mockito.any());
    }

    @Test
    void deleteSucceedsWhenNoBookingsExist() {
        Event event = existingEventOwnedBy(organiser);
        Mockito.when(bookingRepository.existsByTicketCategory_Event_Id(eventId)).thenReturn(false);

        eventService.delete(eventId, organiser);

        Mockito.verify(eventRepository).delete(event);
    }

    @Test
    void listMineQueriesByOrganiserId() {
        Mockito.when(eventRepository.findByOrganiser_Id(organiser.getId())).thenReturn(java.util.List.of());

        assertTrue(eventService.listMine(organiser).isEmpty());
        Mockito.verify(eventRepository).findByOrganiser_Id(organiser.getId());
    }
}

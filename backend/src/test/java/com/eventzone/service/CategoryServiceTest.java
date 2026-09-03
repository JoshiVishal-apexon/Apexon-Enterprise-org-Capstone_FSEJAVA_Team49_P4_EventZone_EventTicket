package com.eventzone.service;

import com.eventzone.dto.event.EventCategoryRequest;
import com.eventzone.dto.event.EventCategoryResponse;
import com.eventzone.exception.ConflictException;
import com.eventzone.exception.NotFoundException;
import com.eventzone.model.EventCategory;
import com.eventzone.repository.EventCategoryRepository;
import com.eventzone.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoryServiceTest {

    private EventCategoryRepository eventCategoryRepository;
    private EventRepository eventRepository;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        eventCategoryRepository = Mockito.mock(EventCategoryRepository.class);
        eventRepository = Mockito.mock(EventRepository.class);
        service = new CategoryService(eventCategoryRepository, eventRepository);

        Mockito.when(eventCategoryRepository.save(Mockito.any(EventCategory.class)))
                .thenAnswer(i -> i.getArgument(0));
        Mockito.when(eventCategoryRepository.findByName(Mockito.anyString())).thenReturn(Optional.empty());
    }

    /**
     * EventCategory generates its own id on construction and has no setter, so
     * the repository stub is keyed off the entity's actual id -- mirroring what
     * findById would really return.
     */
    private EventCategory existing(String name) {
        EventCategory c = new EventCategory();
        c.setName(name);
        Mockito.when(eventCategoryRepository.findById(c.getId())).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void createTrimsName() {
        EventCategoryResponse response = service.create(new EventCategoryRequest("  Comedy  "));

        assertEquals("Comedy", response.name());
    }

    @Test
    void duplicateNameIsRejected() {
        EventCategory clash = new EventCategory();
        clash.setName("Concert");
        Mockito.when(eventCategoryRepository.findByName("Concert")).thenReturn(Optional.of(clash));

        assertThrows(ConflictException.class, () -> service.create(new EventCategoryRequest("Concert")));
        Mockito.verify(eventCategoryRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void renameAllowsKeepingItsOwnName() {
        EventCategory category = existing("Concert");
        // findByName returns this same row; renaming to its own name must not 409.
        Mockito.when(eventCategoryRepository.findByName("Concert")).thenReturn(Optional.of(category));

        assertEquals("Concert",
                service.rename(category.getId(), new EventCategoryRequest("Concert")).name());
    }

    @Test
    void renameToAnotherCategorysNameIsRejected() {
        EventCategory category = existing("Concert");
        EventCategory other = new EventCategory();
        other.setName("Sports");
        Mockito.when(eventCategoryRepository.findByName("Sports")).thenReturn(Optional.of(other));

        assertThrows(ConflictException.class,
                () -> service.rename(category.getId(), new EventCategoryRequest("Sports")));
    }

    @Test
    void renameUnknownCategoryIsRejected() {
        Mockito.when(eventCategoryRepository.findById(Mockito.any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.rename(UUID.randomUUID(), new EventCategoryRequest("X")));
    }

    @Test
    void deleteIsRefusedWhileEventsUseTheCategory() {
        EventCategory category = existing("Concert");
        Mockito.when(eventRepository.existsByCategory_Id(category.getId())).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete(category.getId()));
        Mockito.verify(eventCategoryRepository, Mockito.never()).delete(Mockito.any());
    }

    @Test
    void deleteSucceedsWhenUnused() {
        EventCategory category = existing("Comedy");
        Mockito.when(eventRepository.existsByCategory_Id(category.getId())).thenReturn(false);

        service.delete(category.getId());

        Mockito.verify(eventCategoryRepository).delete(category);
    }
}

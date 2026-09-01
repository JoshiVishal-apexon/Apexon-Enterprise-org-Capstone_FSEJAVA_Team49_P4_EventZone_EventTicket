package com.eventzone.controller;

import com.eventzone.model.Event;
import com.eventzone.repository.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventRepository eventRepository;

    public EventController(EventRepository eventRepository) { this.eventRepository = eventRepository; }

    @GetMapping
    public List<Event> list(@RequestParam(required = false) String category) {
        if (category == null) return eventRepository.findAll();
        return eventRepository.findByCategory_Name(category);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable UUID id) {
        Optional<Event> o = eventRepository.findById(id);
        return o.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}

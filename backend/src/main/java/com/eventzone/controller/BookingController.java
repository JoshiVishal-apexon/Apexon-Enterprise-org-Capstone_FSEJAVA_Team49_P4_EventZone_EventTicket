package com.eventzone.controller;

import com.eventzone.model.Booking;
import com.eventzone.model.TicketCategory;
import com.eventzone.model.User;
import com.eventzone.repository.BookingRepository;
import com.eventzone.repository.TicketCategoryRepository;
import com.eventzone.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingRepository bookingRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final AuthService authService;

    public BookingController(BookingRepository bookingRepository, TicketCategoryRepository ticketCategoryRepository, AuthService authService) {
        this.bookingRepository = bookingRepository;
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.authService = authService;
    }

    private User authFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        return authService.findByToken(token).orElse(null);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                    @RequestBody Map<String,Object> body) {
        User u = authFromHeader(authHeader);
        if (u == null) return ResponseEntity.status(401).body(Map.of("error","AUTH_REQUIRED","message","login required"));
        String ticketId = (String) body.get("ticketCategoryId");
        Integer qty = (Integer) body.getOrDefault("quantity", 1);
        TicketCategory tc = ticketCategoryRepository.findById(java.util.UUID.fromString(ticketId)).orElse(null);
        if (tc == null) return ResponseEntity.badRequest().body(Map.of("error","VALIDATION_ERROR","message","invalid ticketCategoryId"));
        if (tc.getAvailableSeats() < qty) return ResponseEntity.badRequest().body(Map.of("error","NO_SEATS","message","not enough seats"));
        tc.setAvailableSeats(tc.getAvailableSeats() - qty);
        ticketCategoryRepository.save(tc);
        Booking b = new Booking();
        b.setUser(u);
        b.setTicketCategory(tc);
        b.setQuantity(qty);
        bookingRepository.save(b);
        return ResponseEntity.status(201).body(Map.of("bookingRef", b.getBookingRef()));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> mine(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User u = authFromHeader(authHeader);
        if (u == null) return ResponseEntity.status(401).body(Map.of("error","AUTH_REQUIRED","message","login required"));
        List<Booking> list = bookingRepository.findByUser(u);
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                    @PathVariable java.util.UUID id) {
        User u = authFromHeader(authHeader);
        if (u == null) return ResponseEntity.status(401).body(Map.of("error","AUTH_REQUIRED","message","login required"));
        Booking b = bookingRepository.findById(id).orElse(null);
        if (b == null) return ResponseEntity.notFound().build();
        if (!b.getUser().getId().equals(u.getId())) return ResponseEntity.status(403).build();
        b.setStatus("CANCELLED");
        bookingRepository.save(b);
        TicketCategory tc = b.getTicketCategory();
        tc.setAvailableSeats(tc.getAvailableSeats() + b.getQuantity());
        ticketCategoryRepository.save(tc);
        return ResponseEntity.ok(Map.of("status","cancelled"));
    }
}

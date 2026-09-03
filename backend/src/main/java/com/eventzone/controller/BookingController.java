package com.eventzone.controller;

import com.eventzone.entity.User;
import com.eventzone.model.Booking;
import com.eventzone.repository.BookingRepository;
import com.eventzone.security.SecurityUtils;
import com.eventzone.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Map<String, String> create(@RequestBody Map<String, Object> body) {
        // The security filter chain already rejects unauthenticated calls to
        // /api/bookings, so currentUser() is guaranteed to resolve here.
        User user = SecurityUtils.currentUser();
        Booking booking = bookingService.book(body, user);
        return Map.of("bookingRef", booking.getBookingRef());
    }

    @GetMapping("/mine")
    public List<Booking> mine() {
        return bookingRepository.findByUser(SecurityUtils.currentUser());
    }

    @PutMapping("/{id}/cancel")
    public Map<String, String> cancel(@PathVariable UUID id) {
        bookingService.cancel(id, SecurityUtils.currentUser());
        return Map.of("status", "cancelled");
    }
}

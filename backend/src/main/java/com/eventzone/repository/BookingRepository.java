package com.eventzone.repository;

import com.eventzone.model.Booking;
import com.eventzone.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByUser(User user);
}

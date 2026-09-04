package com.eventzone.repository;

import com.eventzone.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    List<Booking> findByTicketCategory_IdAndStatusNot(UUID ticketCategoryId, String status);

    List<Booking> findByTicketCategory_Event_IdAndStatusNot(UUID eventId, String status);

    Booking findFirstByTicketCategory_Event_Id(UUID eventId);

    boolean existsByBookingRef(String bookingRef);
}

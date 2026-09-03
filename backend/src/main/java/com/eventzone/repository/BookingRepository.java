package com.eventzone.repository;

import com.eventzone.entity.User;
import com.eventzone.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByUser(User user);

    /** Seats actually sold for one ticket category; cancelled bookings do not count. */
    @Query("select coalesce(sum(b.quantity), 0) from Booking b "
            + "where b.ticketCategory.id = :ticketCategoryId and b.status = 'CONFIRMED'")
    int sumConfirmedQuantityByTicketCategoryId(@Param("ticketCategoryId") UUID ticketCategoryId);

    /** Guards event deletion: an event with bookings against it must not be removed. */
    boolean existsByTicketCategory_Event_Id(UUID eventId);

    boolean existsByTicketCategory_Id(UUID ticketCategoryId);
}

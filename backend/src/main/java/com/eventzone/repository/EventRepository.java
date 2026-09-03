package com.eventzone.repository;

import com.eventzone.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findByCategory_Name(String categoryName);

    List<Event> findByOrganiser_Id(UUID organiserId);

    /** Public browse list hides events an admin has deactivated. */
    List<Event> findByActiveTrue();

    List<Event> findByCategory_NameAndActiveTrue(String categoryName);

    /** Guards category deletion. */
    boolean existsByCategory_Id(UUID categoryId);
}

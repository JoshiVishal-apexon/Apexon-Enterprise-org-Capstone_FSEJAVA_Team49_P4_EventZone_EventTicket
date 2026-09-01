package com.eventzone.repository;

import com.eventzone.model.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventCategoryRepository extends JpaRepository<EventCategory, UUID> {
    Optional<EventCategory> findByName(String name);
}

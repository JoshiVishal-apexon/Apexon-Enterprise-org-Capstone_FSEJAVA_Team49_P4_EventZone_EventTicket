package com.eventzone.repository;

import com.eventzone.model.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, UUID> {
}

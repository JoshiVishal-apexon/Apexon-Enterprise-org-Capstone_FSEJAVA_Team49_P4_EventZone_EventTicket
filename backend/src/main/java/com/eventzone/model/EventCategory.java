package com.eventzone.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class EventCategory {
    @Id
    private UUID id = UUID.randomUUID();
    @Column(unique = true, nullable = false)
    private String name;

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

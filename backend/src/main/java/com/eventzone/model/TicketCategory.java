package com.eventzone.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

@Entity
public class TicketCategory {
    @Id
    private UUID id = UUID.randomUUID();
    @ManyToOne
    private Event event;
    private String name;
    private double price;
    private int totalSeats;
    private int availableSeats;

    public UUID getId() { return id; }
    // Serialising the whole Event would recurse (Event -> ticketCategories ->
    // event), so the association is hidden and only the title is exposed --
    // which is what the My Bookings table needs.
    @JsonIgnore
    public Event getEvent() { return event; }

    @JsonProperty("eventTitle")
    public String getEventTitle() { return event == null ? null : event.getTitle(); }
    public void setEvent(Event event) { this.event = event; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }
    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
}

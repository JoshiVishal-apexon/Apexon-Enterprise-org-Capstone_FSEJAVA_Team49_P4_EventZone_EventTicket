package com.eventzone.model;

import com.eventzone.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Booking {
    @Id
    private UUID id = UUID.randomUUID();
    @ManyToOne
    private User user;
    @ManyToOne
    private TicketCategory ticketCategory;
    private int quantity;
    private String status = "CONFIRMED";
    private String bookingRef = "BK-" + id.toString();
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public TicketCategory getTicketCategory() { return ticketCategory; }
    public void setTicketCategory(TicketCategory ticketCategory) { this.ticketCategory = ticketCategory; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBookingRef() { return bookingRef; }
    public Instant getCreatedAt() { return createdAt; }
}

package com.eventzone.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
public class Event {
    @Id
    private UUID id = UUID.randomUUID();
    private String title;
    @Column(length = 4000)
    private String description;
    private LocalDateTime eventDate;
    private String venue;
    private String coverImageUrl;
    @ManyToOne
    private User organiser;
    @ManyToOne
    private EventCategory category;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "event")
    private List<TicketCategory> ticketCategories;

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public User getOrganiser() { return organiser; }
    public void setOrganiser(User organiser) { this.organiser = organiser; }
    public EventCategory getCategory() { return category; }
    public void setCategory(EventCategory category) { this.category = category; }
    public List<TicketCategory> getTicketCategories() { return ticketCategories; }
    public void setTicketCategories(List<TicketCategory> ticketCategories) { this.ticketCategories = ticketCategories; }
}

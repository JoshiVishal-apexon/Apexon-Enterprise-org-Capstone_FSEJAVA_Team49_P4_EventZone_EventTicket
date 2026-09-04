package com.eventzone.config;

import com.eventzone.entity.Event;
import com.eventzone.entity.EventCategory;
import com.eventzone.entity.TicketCategory;
import com.eventzone.entity.User;
import com.eventzone.repository.EventCategoryRepository;
import com.eventzone.repository.EventRepository;
import com.eventzone.repository.TicketCategoryRepository;
import com.eventzone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Ensures the required seeded accounts exist with the expected role, name, and
 * password. Existing app data is preserved, but legacy or outdated seed users are
 * normalized so the database reflects the current required credentials.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final String SEED_PASSWORD = "Password@123";

    private final UserRepository userRepository;
    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean hasSeedUsers = userRepository.findByEmail("admin@eventzone.com").isPresent()
                || userRepository.findByEmail("organiser1@eventzone.com").isPresent()
                || userRepository.findByEmail("organiser2@eventzone.com").isPresent()
                || userRepository.findByEmail("attendee1@eventzone.com").isPresent();

        if (userRepository.count() > 0 && !hasSeedUsers) {
            log.info("EventZone: non-seed data already present, skipping seed.");
            return;
        }

        log.info("EventZone: seeding or normalizing initial data...");

        User admin = saveOrUpdateUser("admin@eventzone.com", "Admin User", "ADMIN");
        User organiser1 = saveOrUpdateUser("organiser1@eventzone.com", "Silverline Events", "ORGANISER");
        User organiser2 = saveOrUpdateUser("organiser2@eventzone.com", "BluePeak Productions", "ORGANISER");
        User attendee = saveOrUpdateUser("attendee1@eventzone.com", "Ava Carter", "ATTENDEE");

        Map<String, EventCategory> categories = new HashMap<>();
        for (String name : new String[]{"Concert", "Sports", "Workshop", "Conference"}) {
            EventCategory category = categoryRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> categoryRepository.save(EventCategory.builder().name(name).build()));
            categories.put(name, category);
        }

        // Dates are deliberately set after this seeder's "today" so they remain
        // genuinely upcoming (the EventCreateRequest DTO enforces @Future on
        // organiser-submitted dates via the API; seeded rows bypass that DTO
        // but should still look realistic in the UI).
        createEvent(organiser1, categories.get("Concert"), "Sunburn Arena ft. DJ Nova",
                "An electrifying night of EDM with DJ Nova and supporting acts.",
                LocalDateTime.of(2026, 9, 14, 19, 0), "Jawaharlal Nehru Stadium, Delhi",
                "https://picsum.photos/seed/sunburn/600/400",
                new TicketCategoryDef("General", new BigDecimal("999.00"), 500),
                new TicketCategoryDef("VIP", new BigDecimal("2999.00"), 100));

        createEvent(organiser1, categories.get("Concert"), "Unplugged: Acoustic Nights",
                "An intimate acoustic evening featuring indie artists from across the country.",
                LocalDateTime.of(2026, 10, 5, 18, 30), "Phoenix Marketcity Amphitheatre, Bengaluru",
                "https://picsum.photos/seed/unplugged/600/400",
                new TicketCategoryDef("General", new BigDecimal("499.00"), 300),
                new TicketCategoryDef("VIP", new BigDecimal("1499.00"), 60));

        createEvent(organiser2, categories.get("Sports"), "City Marathon 2027",
                "A 21K half marathon through the heart of the city, open to all skill levels.",
                LocalDateTime.of(2027, 1, 25, 6, 0), "Marine Drive, Mumbai",
                "https://picsum.photos/seed/marathon/600/400",
                new TicketCategoryDef("General", new BigDecimal("299.00"), 2000),
                new TicketCategoryDef("VIP", new BigDecimal("999.00"), 200));

        createEvent(organiser2, categories.get("Sports"), "Premier Cricket League Final",
                "The season finale of the Premier Cricket League. Witness the champions crowned.",
                LocalDateTime.of(2026, 11, 12, 15, 0), "Eden Gardens, Kolkata",
                "https://picsum.photos/seed/cricket/600/400",
                new TicketCategoryDef("General", new BigDecimal("799.00"), 5000),
                new TicketCategoryDef("VIP", new BigDecimal("4999.00"), 500));

        createEvent(organiser1, categories.get("Workshop"), "Full-Stack Development Bootcamp",
                "A hands-on weekend workshop covering Angular, Spring Boot, and REST API design.",
                LocalDateTime.of(2026, 9, 28, 9, 0), "WeWork Galaxy, Bengaluru",
                "https://picsum.photos/seed/bootcamp/600/400",
                new TicketCategoryDef("General", new BigDecimal("1499.00"), 80),
                new TicketCategoryDef("VIP", new BigDecimal("2999.00"), 20));

        createEvent(organiser2, categories.get("Conference"), "TechForward Summit 2027",
                "Industry leaders discuss the future of cloud, AI, and platform engineering.",
                LocalDateTime.of(2027, 2, 20, 9, 30), "HITEX Exhibition Centre, Hyderabad",
                "https://picsum.photos/seed/techforward/600/400",
                new TicketCategoryDef("General", new BigDecimal("1999.00"), 1000),
                new TicketCategoryDef("VIP", new BigDecimal("5999.00"), 150));

        log.info("=================================================================");
        log.info("EventZone seed data created. Login credentials (password for all: {}):", SEED_PASSWORD);
        log.info("  ADMIN      -> {}", admin.getEmail());
        log.info("  ORGANISER  -> {}", organiser1.getEmail());
        log.info("  ORGANISER  -> {}", organiser2.getEmail());
        log.info("  ATTENDEE   -> {}", attendee.getEmail());
        log.info("=================================================================");
    }

    private User saveOrUpdateUser(String email, String name, String role) {
        return userRepository.findByEmail(email)
                .map(existing -> {
                    existing.setName(name);
                    existing.setRole(role);
                    existing.setPasswordHash(passwordEncoder.encode(SEED_PASSWORD));
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode(SEED_PASSWORD))
                        .role(role)
                        .name(name)
                        .build()));
    }

    private void createEvent(User organiser, EventCategory category, String title, String description,
                              LocalDateTime eventDate, String venue, String coverImageUrl,
                              TicketCategoryDef... ticketCategoryDefs) {
        Event event = eventRepository.save(Event.builder()
                .title(title)
                .description(description)
                .eventDate(eventDate)
                .venue(venue)
                .coverImageUrl(coverImageUrl)
                .organiser(organiser)
                .category(category)
                .active(true)
                .build());

        for (TicketCategoryDef def : ticketCategoryDefs) {
            ticketCategoryRepository.save(TicketCategory.builder()
                    .event(event)
                    .name(def.name())
                    .price(def.price())
                    .totalSeats(def.totalSeats())
                    .availableSeats(def.totalSeats())
                    .build());
        }
    }

    private record TicketCategoryDef(String name, BigDecimal price, int totalSeats) {
    }
}

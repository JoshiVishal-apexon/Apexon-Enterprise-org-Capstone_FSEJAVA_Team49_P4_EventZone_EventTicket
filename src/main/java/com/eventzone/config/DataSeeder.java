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
        createEvent(organiser1, categories.get("Concert"), "Neon Nights Festival",
                "A high-energy live music festival featuring indie bands, pop artists, and electronic sets.",
                LocalDateTime.of(2026, 9, 18, 19, 30), "Qutub Shahi Tombs Lawn, Hyderabad",
                "https://res.cloudinary.com/tickeri/image/upload/v1784182165/yrosrhxihm6qwwkjg0hj.jpg",
                new TicketCategoryDef("General", new BigDecimal("1299.00"), 600),
                new TicketCategoryDef("VIP", new BigDecimal("3499.00"), 120));

        createEvent(organiser1, categories.get("Concert"), "Riverside Acoustic Evening",
                "An intimate evening of acoustic performances, folk classics, and café-style storytelling.",
                LocalDateTime.of(2026, 10, 14, 18, 0), "Sula Vineyard Stage, Nashik",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRcnPouP76SOeQ1Ks_TjKfi7W4k1n1Plijwh2Zb1euWj3ORNeV0o6tg2oIE&s=10",
                new TicketCategoryDef("General", new BigDecimal("699.00"), 350),
                new TicketCategoryDef("VIP", new BigDecimal("1799.00"), 70));

        createEvent(organiser2, categories.get("Sports"), "Metro City 10K Run",
                "A vibrant city run for beginners and seasoned athletes, followed by wellness booths and music.",
                LocalDateTime.of(2027, 1, 16, 6, 30), "Bandra Fort Loop, Mumbai",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRhMAU5G7e9nObkYdzyzgu8-goQH0Rb1yy_ibpo3dz23A&s=10",
                new TicketCategoryDef("General", new BigDecimal("499.00"), 2500),
                new TicketCategoryDef("VIP", new BigDecimal("1499.00"), 250));

        createEvent(organiser2, categories.get("Sports"), "Champions Cup Final",
                "Watch the biggest football showdown of the season with live commentary, fan zones, and merchandise stalls.",
                LocalDateTime.of(2026, 11, 28, 17, 30), "Yuva Bharati Krirangan, Kolkata",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR8EzZ-14reAIKdlKdCGUth0YuLr8R3yWzwK8ng0_ERqg&s=10",
                new TicketCategoryDef("General", new BigDecimal("999.00"), 6000),
                new TicketCategoryDef("VIP", new BigDecimal("5999.00"), 600));

        createEvent(organiser2, categories.get("Conference"), "Future of AI Summit",
                "Leadership talks, Demos, and networking sessions on generative AI, cloud automation, and digital trust.",
                LocalDateTime.of(2027, 2, 12, 9, 0), "HITEX Exhibition Centre, Hyderabad",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSBzb_QsR641e7W4-sbd6yOyaEt1_GvrSKJPqSAvWjomA&s=10",
                new TicketCategoryDef("General", new BigDecimal("2499.00"), 1200),
                new TicketCategoryDef("VIP", new BigDecimal("6999.00"), 180));

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

-- Seed data, matching section 16 of the capstone spec.
--
-- Logins (all three share the same password):
--   admin@eventzone.com  / Password123!   ADMIN
--   org1@eventzone.com   / Password123!   ORGANISER  (organises Rock Night 2025)
--   user1@eventzone.com  / Password123!   ATTENDEE
--
-- The spec writes password_hash as the placeholder '$2a$10$hash'; these are real
-- 60-char BCrypt hashes of Password123! instead, since a placeholder can never
-- satisfy passwordEncoder.matches() and makes every login fail with 401.
--
-- Fixed UUIDs are used rather than the spec's gen_random_uuid() so that each
-- statement can be guarded and this script stays safe to re-run: with a
-- file-backed H2, spring.sql.init.mode=always executes it on every startup.
-- Every insert is therefore conditional, and existing rows are never
-- overwritten -- that protects mutable columns such as
-- ticket_category.available_seats, which bookings decrement and which a plain
-- re-INSERT (or MERGE) would silently reset back to the seeded value.

-- Users ---------------------------------------------------------------------

INSERT INTO users(id, email, password_hash, role, name, created_at)
SELECT '11111111-1111-1111-1111-111111111111', 'admin@eventzone.com',
       '$2a$10$wSmFtD6QH2cF1aKkeZqj8.HD9u9WiGZYCsjdXMoVC/NhDmS8QQpFW',
       'ADMIN', 'Admin', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = '11111111-1111-1111-1111-111111111111');

INSERT INTO users(id, email, password_hash, role, name, created_at)
SELECT '55555555-5555-5555-5555-555555555555', 'org1@eventzone.com',
       '$2a$10$/6pmZILyNujfImJHJZenE..Ov53.j7YX.t8eCrRVyRFXsFYVZS3ne',
       'ORGANISER', 'Abhishek Events', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = '55555555-5555-5555-5555-555555555555');

INSERT INTO users(id, email, password_hash, role, name, created_at)
SELECT '66666666-6666-6666-6666-666666666666', 'user1@eventzone.com',
       '$2a$10$LgFcIXHFHGQA9atRr5sNpOb3yoHeJmQP.0BT3xXwTqcekJfInYESW',
       'ATTENDEE', 'Divya', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = '66666666-6666-6666-6666-666666666666');

-- Event categories ----------------------------------------------------------

INSERT INTO event_category(id, name)
SELECT 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Concert'
WHERE NOT EXISTS (SELECT 1 FROM event_category WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');

INSERT INTO event_category(id, name)
SELECT 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Sports'
WHERE NOT EXISTS (SELECT 1 FROM event_category WHERE id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');

INSERT INTO event_category(id, name)
SELECT 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'Workshop'
WHERE NOT EXISTS (SELECT 1 FROM event_category WHERE id = 'cccccccc-cccc-cccc-cccc-cccccccccccc');

-- Events --------------------------------------------------------------------
-- Organised by org1 (Abhishek Events), per the spec, not by the admin account.

INSERT INTO event(id, title, description, event_date, venue, cover_image_url, organiser_id, category_id, active)
SELECT '22222222-2222-2222-2222-222222222222', 'Rock Night 2025',
       'Live rock concert by top bands', '2025-11-15T19:00:00',
       'HICC Hyderabad', 'https://picsum.photos/seed/rock/400',
       '55555555-5555-5555-5555-555555555555', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', TRUE
WHERE NOT EXISTS (SELECT 1 FROM event WHERE id = '22222222-2222-2222-2222-222222222222');

-- Ticket categories ---------------------------------------------------------

INSERT INTO ticket_category(id, event_id, name, price, total_seats, available_seats)
SELECT '33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222',
       'General', 999.00, 200, 200
WHERE NOT EXISTS (SELECT 1 FROM ticket_category WHERE id = '33333333-3333-3333-3333-333333333333');

INSERT INTO ticket_category(id, event_id, name, price, total_seats, available_seats)
SELECT '44444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222',
       'VIP', 2499.00, 50, 50
WHERE NOT EXISTS (SELECT 1 FROM ticket_category WHERE id = '44444444-4444-4444-4444-444444444444');

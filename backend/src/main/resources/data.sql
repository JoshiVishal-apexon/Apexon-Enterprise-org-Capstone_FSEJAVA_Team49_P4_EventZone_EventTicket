INSERT INTO users(id,email,password_hash,role,name,created_at) VALUES
('11111111-1111-1111-1111-111111111111','admin@eventzone.com','$2a$10$7Qz3B2yQmQY2qF5uQpT2uOXGZfZ5Gh3o1q9aWZ9y3Y2p3Q2','ADMIN','Admin',CURRENT_TIMESTAMP);
INSERT INTO event_category(id,name) VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','Concert');
INSERT INTO event(id,title,description,event_date,venue,cover_image_url,organiser_id,category_id) VALUES
('22222222-2222-2222-2222-222222222222','Rock Night 2025','Live rock concert','2025-11-15T19:00:00','HICC Hyderabad','https://picsum.photos/seed/rock/400','11111111-1111-1111-1111-111111111111','aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');
INSERT INTO ticket_category(id,event_id,name,price,total_seats,available_seats) VALUES
('33333333-3333-3333-3333-333333333333','22222222-2222-2222-2222-222222222222','General',999.00,200,200),
('44444444-4444-4444-4444-444444444444','22222222-2222-2222-2222-222222222222','VIP',2499.00,50,50);

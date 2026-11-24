-- =============================================================
--  Sample data for demos. Run ONCE after schema.sql:
--  mysql -u root -p < database/seed.sql
--  Booking dates are relative to today so the demo always looks current.
-- =============================================================

USE hotel_reservation;

INSERT INTO rooms (room_number, room_type, floor_no, capacity, price_per_night, status, description) VALUES
    ('101', 'STANDARD', 1, 2,  750.00, 'AVAILABLE',   'Garden side, queen bed'),
    ('102', 'STANDARD', 1, 2,  750.00, 'OCCUPIED',    'Garden side, twin beds'),
    ('103', 'STANDARD', 1, 1,  600.00, 'AVAILABLE',   'Single room near reception'),
    ('104', 'STANDARD', 1, 2,  750.00, 'MAINTENANCE', 'Air conditioner being serviced'),
    ('201', 'DELUXE',   2, 3, 1200.00, 'AVAILABLE',   'Balcony, king bed + sofa bed'),
    ('202', 'DELUXE',   2, 2, 1200.00, 'AVAILABLE',   'Balcony, king bed'),
    ('203', 'DELUXE',   2, 4, 1450.00, 'AVAILABLE',   'Family room, two double beds'),
    ('204', 'DELUXE',   2, 2, 1200.00, 'AVAILABLE',   'Corner room, city view'),
    ('301', 'SUITE',    3, 4, 2500.00, 'AVAILABLE',   'Executive suite with lounge'),
    ('302', 'SUITE',    3, 2, 3000.00, 'AVAILABLE',   'Honeymoon suite, sea view');

INSERT INTO customers (full_name, phone, email, id_number, nationality) VALUES
    ('Aminata Kamara',  '+232 76 123456', 'aminata.kamara@example.com', 'SL-NIN-004512', 'Sierra Leonean'),
    ('Mohamed Bangura', '+232 77 654321', 'mbangura@example.com',       'SL-NIN-118230', 'Sierra Leonean'),
    ('Fatmata Sesay',   '+232 88 112233', NULL,                         'SL-NIN-220981', 'Sierra Leonean'),
    ('Kwame Mensah',    '+233 24 555 0198', 'kwame.mensah@example.com', 'G0451123',      'Ghanaian');

-- Room 102 is occupied by a guest who checked in yesterday.
INSERT INTO bookings (customer_id, room_id, check_in, check_out, guests, breakfast, airport_pickup,
                      payment_method, total_amount, status, notes) VALUES
    (1, 2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 2 DAY), 2, FALSE, FALSE,
        'ORANGE_MONEY', 2250.00, 'CHECKED_IN', ''),
    (2, 5, DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 2, TRUE, FALSE,
        'CARD', 3000.00, 'CONFIRMED', 'Late arrival, around 22:00'),
    (4, 9, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 DAY), 2, FALSE, TRUE,
        'CASH', 5950.00, 'CONFIRMED', 'Pickup from Lungi airport'),
    (3, 6, DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 9 DAY), 1, FALSE, FALSE,
        'AFRIMONEY', 2400.00, 'CANCELLED', 'Guest changed travel plans');

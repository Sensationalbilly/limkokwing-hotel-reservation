-- =============================================================
--  Hotel Reservation System - database schema (MySQL 8+)
--  Run once:  mysql -u root -p < database/schema.sql
--  Then (optional sample data):  mysql -u root -p < database/seed.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS hotel_reservation
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hotel_reservation;

-- Staff accounts. Passwords are stored as PBKDF2 hashes, never plain text.
-- The app creates default accounts automatically on first start if this table is empty.
CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          ENUM('ADMIN', 'RECEPTIONIST') NOT NULL DEFAULT 'RECEPTIONIST',
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,  -- set after an admin reset
    rating        TINYINT NULL,                            -- admin's 1-5 star rating
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Daily attendance. clock_in is recorded by the system (Freetown time) at the first sign-in
-- of the day; reported_time is what the staff member typed, kept for comparison.
CREATE TABLE IF NOT EXISTS attendance (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT      NOT NULL,
    work_date     DATE     NOT NULL,
    clock_in      DATETIME NOT NULL,
    reported_time TIME     NULL,
    late_minutes  INT      NOT NULL DEFAULT 0,
    note          VARCHAR(255) DEFAULT '',
    clock_out     DATETIME NULL,
    UNIQUE KEY uq_attendance_day (user_id, work_date),
    CONSTRAINT fk_attendance_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Who did what, used for the staff performance report.
CREATE TABLE IF NOT EXISTS activity_log (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NULL,
    action     VARCHAR(30) NOT NULL,
    booking_id INT NULL,
    amount     DECIMAL(10,2) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activity_user (user_id, created_at),
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

-- "Forgot password" requests sent from the sign-in screen to administrators.
CREATE TABLE IF NOT EXISTS password_requests (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    note       VARCHAR(255) DEFAULT '',
    status     ENUM('PENDING', 'RESOLVED', 'DISMISSED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    handled_at TIMESTAMP NULL DEFAULT NULL,
    handled_by INT NULL,
    CONSTRAINT fk_requests_user  FOREIGN KEY (user_id)    REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_requests_admin FOREIGN KEY (handled_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS rooms (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    room_number     VARCHAR(10)   NOT NULL UNIQUE,
    room_type       ENUM('STANDARD', 'DELUXE', 'SUITE') NOT NULL,
    floor_no        INT           NOT NULL DEFAULT 1,
    capacity        INT           NOT NULL DEFAULT 2,
    price_per_night DECIMAL(10,2) NOT NULL,
    status          ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    description     VARCHAR(255)  DEFAULT '',
    amenities       VARCHAR(500)  NULL,  -- features ticked by the admin, ";"-separated; NULL = type defaults
    CHECK (capacity > 0),
    CHECK (price_per_night > 0)
);

CREATE TABLE IF NOT EXISTS customers (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    phone       VARCHAR(25)  NOT NULL,
    email       VARCHAR(100) DEFAULT NULL,
    id_number   VARCHAR(50)  DEFAULT NULL,
    nationality VARCHAR(50)  DEFAULT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_customers_phone (phone)
);

CREATE TABLE IF NOT EXISTS bookings (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    customer_id    INT           NOT NULL,
    room_id        INT           NOT NULL,
    check_in       DATE          NOT NULL,
    check_out      DATE          NOT NULL,
    guests         INT           NOT NULL DEFAULT 1,
    breakfast      BOOLEAN       NOT NULL DEFAULT FALSE,
    airport_pickup BOOLEAN       NOT NULL DEFAULT FALSE,
    payment_method VARCHAR(20)   NOT NULL DEFAULT 'CASH',
    total_amount   DECIMAL(10,2) NOT NULL,
    status         ENUM('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    notes          VARCHAR(255)  DEFAULT '',
    created_by     INT           DEFAULT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_room     FOREIGN KEY (room_id)     REFERENCES rooms (id)     ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_user     FOREIGN KEY (created_by)  REFERENCES users (id)     ON DELETE SET NULL,
    CHECK (check_out > check_in),
    CHECK (guests > 0),
    INDEX idx_bookings_room_dates (room_id, check_in, check_out)
);

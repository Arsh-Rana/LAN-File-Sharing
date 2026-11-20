-- ============================================================================
-- LAN File Sharing Application Database Schema
-- Database: fileshare_db
-- ============================================================================

CREATE DATABASE IF NOT EXISTS fileshare_db;
USE fileshare_db;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. File Transfers Table
CREATE TABLE IF NOT EXISTS file_transfers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    transfer_uuid VARCHAR(64) NOT NULL UNIQUE,
    sender_username VARCHAR(50) NOT NULL,
    receiver_username VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Pending',
    transferred_bytes BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_sender_user FOREIGN KEY (sender_username) REFERENCES users(username) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_receiver_user FOREIGN KEY (receiver_username) REFERENCES users(username) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Useful indexes for querying transfer history and user status
CREATE INDEX IF NOT EXISTS idx_transfers_sender ON file_transfers(sender_username);
CREATE INDEX IF NOT EXISTS idx_transfers_receiver ON file_transfers(receiver_username);
CREATE INDEX IF NOT EXISTS idx_transfers_status ON file_transfers(status);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- Seed Initial Test Users (Password for all seed users is 'password123')
-- SHA-256 for 'password123': ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f
INSERT INTO users (username, password_hash, full_name, email, status)
VALUES 
('alice', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'Alice Johnson', 'alice@lanfileshare.com', 'OFFLINE'),
('bob', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'Bob Smith', 'bob@lanfileshare.com', 'OFFLINE')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

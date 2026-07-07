CREATE DATABASE IF NOT EXISTS treehole DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE treehole;

CREATE TABLE IF NOT EXISTS user (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    role INT DEFAULT 1 COMMENT '1: user, 2: admin',
    avatar_url LONGTEXT,
    status INT DEFAULT 1,
    mute_status INT DEFAULT 0 COMMENT '0: normal, 1: muted',
    mute_until DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- Insert default admin
INSERT IGNORE INTO user (username, password, nickname, role) VALUES ('admin', '123456', '管理员', 2);
-- Insert default user
INSERT IGNORE INTO user (username, password, nickname, role) VALUES ('user', '123456', '匿名同学', 1);

CREATE TABLE IF NOT EXISTS post (
    id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(100),
    body TEXT,
    category VARCHAR(50),
    mood VARCHAR(50),
    alias VARCHAR(50),
    author_username VARCHAR(50),
    tags JSON,
    media LONGTEXT,
    likes INT DEFAULT 0,
    liked_by JSON,
    hugs INT DEFAULT 0,
    hugged_by JSON,
    reports INT DEFAULT 0,
    reported_by JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    audit_status INT DEFAULT 0 COMMENT '0: pending, 1: approved, 2: rejected',
    audit_reason VARCHAR(255),
    audited_by VARCHAR(50),
    audited_at DATETIME,
    is_deleted INT DEFAULT 0,
    is_pinned INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS reply (
    id VARCHAR(50) PRIMARY KEY,
    post_id VARCHAR(50),
    parent_reply_id VARCHAR(50),
    body TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    author_username VARCHAR(50),
    alias VARCHAR(50),
    likes INT DEFAULT 0,
    liked_by JSON,
    hugs INT DEFAULT 0,
    hugged_by JSON,
    reports INT DEFAULT 0,
    reported_by JSON,
    audit_status INT DEFAULT 0 COMMENT '0: pending, 1: approved, 2: rejected',
    audit_reason VARCHAR(255),
    audited_by VARCHAR(50),
    audited_at DATETIME,
    is_deleted INT DEFAULT 0,
    FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS listener_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    reason VARCHAR(500),
    bio VARCHAR(500),
    topics VARCHAR(255),
    available_time VARCHAR(255),
    status INT DEFAULT 0 COMMENT '0: pending, 1: approved, 2: rejected',
    audit_reason VARCHAR(255),
    audited_by VARCHAR(50),
    audited_at DATETIME,
    listen_count INT DEFAULT 0,
    thanks_count INT DEFAULT 0,
    warmth INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS listener_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listener_username VARCHAR(50) NOT NULL,
    requester_username VARCHAR(50) NOT NULL,
    topic VARCHAR(120),
    message TEXT,
    response_mode VARCHAR(50),
    status INT DEFAULT 0 COMMENT '0: pending, 1: accepted, 2: rejected, 3: replied, 4: completed',
    reply_text TEXT,
    thanks_sent INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    accepted_at DATETIME,
    replied_at DATETIME,
    completed_at DATETIME
);

CREATE TABLE IF NOT EXISTS listener_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    sender_username VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_listener_message_request (request_id),
    INDEX idx_listener_message_sender (sender_username)
);

-- If you already created the database before media upload was added, run:
-- ALTER TABLE post ADD COLUMN media LONGTEXT;
-- ALTER TABLE post ADD COLUMN is_pinned INT DEFAULT 0;
-- ALTER TABLE user ADD COLUMN mute_status INT DEFAULT 0;
-- ALTER TABLE user ADD COLUMN mute_until DATETIME;
-- ALTER TABLE user MODIFY COLUMN avatar_url LONGTEXT;
-- ALTER TABLE reply ADD COLUMN parent_reply_id VARCHAR(50);
-- ALTER TABLE reply ADD COLUMN likes INT DEFAULT 0;
-- ALTER TABLE reply ADD COLUMN liked_by JSON;
-- ALTER TABLE reply ADD COLUMN hugs INT DEFAULT 0;
-- ALTER TABLE reply ADD COLUMN hugged_by JSON;
-- ALTER TABLE reply ADD COLUMN reports INT DEFAULT 0;
-- ALTER TABLE reply ADD COLUMN reported_by JSON;
-- CREATE TABLE IF NOT EXISTS listener_message (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     request_id BIGINT NOT NULL,
--     sender_username VARCHAR(50) NOT NULL,
--     message TEXT NOT NULL,
--     created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
--     INDEX idx_listener_message_request (request_id),
--     INDEX idx_listener_message_sender (sender_username)
-- );

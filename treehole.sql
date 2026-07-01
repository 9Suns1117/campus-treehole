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
    body TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    author_username VARCHAR(50),
    alias VARCHAR(50),
    audit_status INT DEFAULT 0 COMMENT '0: pending, 1: approved, 2: rejected',
    audit_reason VARCHAR(255),
    audited_by VARCHAR(50),
    audited_at DATETIME,
    is_deleted INT DEFAULT 0,
    FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE
);

-- If you already created the database before media upload was added, run:
-- ALTER TABLE post ADD COLUMN media LONGTEXT;
-- ALTER TABLE post ADD COLUMN is_pinned INT DEFAULT 0;
-- ALTER TABLE user ADD COLUMN mute_status INT DEFAULT 0;
-- ALTER TABLE user ADD COLUMN mute_until DATETIME;
-- ALTER TABLE user MODIFY COLUMN avatar_url LONGTEXT;

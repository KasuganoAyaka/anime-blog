-- Anime Blog database schema
CREATE DATABASE IF NOT EXISTS `anime_blog`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `anime_blog`;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `email_verified` TINYINT NOT NULL DEFAULT 0,
    `avatar` TEXT DEFAULT NULL,
    `nickname` VARCHAR(100) DEFAULT NULL,
    `bio` TEXT DEFAULT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'user',
    `status` TINYINT NOT NULL DEFAULT 1,
    `last_login_time` DATETIME DEFAULT NULL,
    `last_login_ip` VARCHAR(50) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_email` (`email`),
    KEY `idx_user_role_status` (`role`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `email_code` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(100) NOT NULL,
    `code` VARCHAR(10) NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `expire_time` DATETIME NOT NULL,
    `used` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_email_code_lookup` (`email`, `type`, `used`, `create_time`),
    KEY `idx_email_code_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `music` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL,
    `artist` VARCHAR(100) DEFAULT NULL,
    `album` VARCHAR(200) DEFAULT NULL,
    `url` VARCHAR(500) NOT NULL,
    `cover_url` VARCHAR(500) DEFAULT NULL,
    `duration` INT NOT NULL DEFAULT 0,
    `lyrics` TEXT DEFAULT NULL,
    `sort` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    `play_count` BIGINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_music_status_sort` (`status`, `sort`),
    KEY `idx_music_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `site_stats` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `stat_key` VARCHAR(50) NOT NULL,
    `stat_value` BIGINT NOT NULL DEFAULT 0,
    `description` VARCHAR(255) DEFAULT NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_site_stats_stat_key` (`stat_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `visitor_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `ip` VARCHAR(50) DEFAULT NULL,
    `ua` VARCHAR(500) DEFAULT NULL,
    `page` VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_visitor_log_create_time` (`create_time`),
    KEY `idx_visitor_log_ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `friend_link` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `url` VARCHAR(500) NOT NULL,
    `logo` VARCHAR(500) DEFAULT NULL,
    `description` TEXT DEFAULT NULL,
    `email` VARCHAR(100) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 0,
    `sort` INT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_friend_link_url` (`url`),
    KEY `idx_friend_link_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL,
    `slug` VARCHAR(200) DEFAULT NULL,
    `content` LONGTEXT DEFAULT NULL,
    `summary` TEXT DEFAULT NULL,
    `excerpt` TEXT DEFAULT NULL,
    `category` VARCHAR(100) DEFAULT NULL,
    `tags` VARCHAR(500) DEFAULT NULL,
    `cover_image` VARCHAR(500) DEFAULT NULL,
    `user_id` BIGINT DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'published',
    `view_count` BIGINT NOT NULL DEFAULT 0,
    `like_count` BIGINT NOT NULL DEFAULT 0,
    `comment_count` BIGINT NOT NULL DEFAULT 0,
    `is_featured` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_slug` (`slug`),
    KEY `idx_post_status_create_time` (`status`, `create_time`),
    KEY `idx_post_category_status` (`category`, `status`),
    KEY `idx_post_user_id` (`user_id`),
    KEY `idx_post_featured_status` (`is_featured`, `status`),
    CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `post_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT DEFAULT NULL,
    `user_id` BIGINT NOT NULL,
    `action` VARCHAR(20) NOT NULL DEFAULT 'create',
    `review_status` VARCHAR(20) NOT NULL DEFAULT 'pending',
    `title` VARCHAR(200) NOT NULL,
    `slug` VARCHAR(200) DEFAULT NULL,
    `content` LONGTEXT DEFAULT NULL,
    `summary` TEXT DEFAULT NULL,
    `excerpt` TEXT DEFAULT NULL,
    `category` VARCHAR(100) DEFAULT NULL,
    `tags` VARCHAR(500) DEFAULT NULL,
    `cover_image` VARCHAR(500) DEFAULT NULL,
    `post_status` VARCHAR(20) NOT NULL DEFAULT 'published',
    `review_note` TEXT DEFAULT NULL,
    `reviewer_id` BIGINT DEFAULT NULL,
    `reviewed_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_post_review_user_time` (`user_id`, `update_time`, `create_time`),
    KEY `idx_post_review_post_id` (`post_id`),
    KEY `idx_post_review_status_time` (`review_status`, `update_time`, `create_time`),
    KEY `idx_post_review_reviewer_id` (`reviewer_id`),
    CONSTRAINT `fk_post_review_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_post_review_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_post_review_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `contact_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `subject` VARCHAR(200) NOT NULL,
    `message` TEXT NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 0,
    `reply_content` TEXT DEFAULT NULL,
    `replied_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_contact_message_email` (`email`),
    KEY `idx_contact_message_status` (`status`),
    KEY `idx_contact_message_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_friend` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `friend_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chat_friend_pair` (`user_id`, `friend_id`),
    KEY `idx_chat_friend_friend` (`friend_id`),
    CONSTRAINT `fk_chat_friend_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_friend_target` FOREIGN KEY (`friend_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `room_type` VARCHAR(20) NOT NULL,
    `room_key` VARCHAR(100) NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `receiver_id` BIGINT DEFAULT NULL,
    `content` TEXT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_chat_message_room` (`room_type`, `room_key`, `create_time`),
    KEY `idx_chat_message_sender` (`sender_id`, `create_time`),
    KEY `idx_chat_message_receiver` (`receiver_id`, `create_time`),
    CONSTRAINT `fk_chat_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_message_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_friend_request` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `requester_id` BIGINT NOT NULL,
    `target_user_id` BIGINT NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
    `handled_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_chat_friend_request_target_status` (`target_user_id`, `status`, `update_time`),
    KEY `idx_chat_friend_request_requester_status` (`requester_id`, `status`, `update_time`),
    CONSTRAINT `fk_chat_friend_request_requester` FOREIGN KEY (`requester_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_friend_request_target` FOREIGN KEY (`target_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_block` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `blocked_user_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chat_block_pair` (`user_id`, `blocked_user_id`),
    KEY `idx_chat_block_target` (`blocked_user_id`),
    CONSTRAINT `fk_chat_block_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_block_target` FOREIGN KEY (`blocked_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_room_state` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `room_type` VARCHAR(20) NOT NULL,
    `room_key` VARCHAR(100) NOT NULL,
    `peer_user_id` BIGINT DEFAULT NULL,
    `unread_count` INT NOT NULL DEFAULT 0,
    `last_message_preview` VARCHAR(300) DEFAULT NULL,
    `last_read_time` DATETIME DEFAULT NULL,
    `last_message_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chat_room_state_user_room` (`user_id`, `room_type`, `room_key`),
    KEY `idx_chat_room_state_user_unread` (`user_id`, `unread_count`, `last_message_time`),
    KEY `idx_chat_room_state_peer` (`peer_user_id`),
    CONSTRAINT `fk_chat_room_state_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_room_state_peer` FOREIGN KEY (`peer_user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `post_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT DEFAULT NULL,
    `parent_id` BIGINT DEFAULT NULL,
    `author_name` VARCHAR(100) NOT NULL,
    `author_email` VARCHAR(100) DEFAULT NULL,
    `author_website` VARCHAR(255) DEFAULT NULL,
    `author_avatar` TEXT DEFAULT NULL,
    `content` TEXT DEFAULT NULL,
    `images` TEXT DEFAULT NULL,
    `like_count` BIGINT NOT NULL DEFAULT 0,
    `anonymous` TINYINT NOT NULL DEFAULT 0,
    `status` VARCHAR(20) NOT NULL DEFAULT 'visible',
    `reviewed_by` BIGINT DEFAULT NULL,
    `review_note` VARCHAR(500) DEFAULT NULL,
    `reviewed_time` DATETIME DEFAULT NULL,
    `client_ip` VARCHAR(50) DEFAULT NULL,
    `user_agent` VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_post_comment_post_status_time` (`post_id`, `status`, `create_time`),
    KEY `idx_post_comment_user_time` (`user_id`, `create_time`),
    KEY `idx_post_comment_parent` (`parent_id`),
    KEY `idx_post_comment_review_status` (`status`, `reviewed_time`),
    CONSTRAINT `fk_post_comment_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_post_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `comment_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `comment_id` BIGINT NOT NULL,
    `post_id` BIGINT NOT NULL,
    `reporter_id` BIGINT DEFAULT NULL,
    `reason_code` VARCHAR(32) NOT NULL,
    `reason_label` VARCHAR(100) NOT NULL,
    `other_reason` VARCHAR(100) DEFAULT NULL,
    `description` VARCHAR(1000) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
    `resolution_action` VARCHAR(20) DEFAULT NULL,
    `resolution_note` VARCHAR(500) DEFAULT NULL,
    `resolved_by` BIGINT DEFAULT NULL,
    `resolved_time` DATETIME DEFAULT NULL,
    `comment_author_name` VARCHAR(100) NOT NULL,
    `comment_content` TEXT DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_comment_report_status_time` (`status`, `create_time`),
    KEY `idx_comment_report_comment_status` (`comment_id`, `status`),
    KEY `idx_comment_report_post_time` (`post_id`, `create_time`),
    KEY `idx_comment_report_reporter` (`reporter_id`),
    KEY `idx_comment_report_resolver` (`resolved_by`),
    CONSTRAINT `fk_comment_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_comment_report_resolver` FOREIGN KEY (`resolved_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ip_region_cache` (
    `ip` VARCHAR(64) NOT NULL,
    `region` VARCHAR(100) NOT NULL,
    `expires_at` DATETIME NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`ip`),
    KEY `idx_ip_region_cache_expires_at` (`expires_at`),
    KEY `idx_ip_region_cache_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- QN Contest 数据库初始化脚本
-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `qn` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `qn`;

-- 创建用户表
CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('USER','ADMIN','MODERATOR') NOT NULL DEFAULT 'USER',
  `is_enabled` bit(1) NOT NULL DEFAULT b'1',
  `is_account_non_expired` bit(1) NOT NULL DEFAULT b'1',
  `is_account_non_locked` bit(1) NOT NULL DEFAULT b'1',
  `is_credentials_non_expired` bit(1) NOT NULL DEFAULT b'1',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_username` (`username`),
  UNIQUE KEY `UK_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建刷新令牌表
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `token` varchar(255) NOT NULL,
  `user_id` bigint NOT NULL,
  `expiry_date` datetime(6) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_token` (`token`),
  KEY `FK_refresh_tokens_user_id` (`user_id`),
  CONSTRAINT `FK_refresh_tokens_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入默认管理员用户（密码已加密：admin123）
INSERT IGNORE INTO `users` (`username`, `email`, `password`, `role`, `created_at`, `updated_at`) VALUES
('admin', 'admin@qncontest.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'ADMIN', NOW(), NOW());

-- 插入默认测试用户（密码已加密：test123）
INSERT IGNORE INTO `users` (`username`, `email`, `password`, `role`, `created_at`, `updated_at`) VALUES
('testuser', 'test@qncontest.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'USER', NOW(), NOW());

-- 创建聊天会话表
CREATE TABLE IF NOT EXISTS `chat_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  `title` varchar(500),
  `created_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `is_active` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_session_user` (`session_id`, `user_id`),
  INDEX `IDX_user_id_updated` (`user_id`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建聊天消息表
CREATE TABLE IF NOT EXISTS `chat_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `role` enum('user','assistant','system') NOT NULL,
  `content` longtext NOT NULL,
  `tokens` int DEFAULT NULL,
  `sequence_number` int NOT NULL,
  `created_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `FK_chat_messages_session` (`session_id`),
  KEY `IDX_session_sequence` (`session_id`, `sequence_number`),
  CONSTRAINT `FK_chat_messages_session` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 显示创建的表
SHOW TABLES;

-- 显示用户数据
SELECT id, username, email, role, created_at FROM users;

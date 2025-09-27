-- ===================================================================
-- QN Contest 角色扮演AI系统 - 完整数据库初始化脚本
-- 版本: 2.0
-- 创建时间: 2025-09-23
-- 说明: 包含基础用户系统、聊天系统和角色扮演扩展功能
-- ===================================================================

-- 设置字符集和排序规则
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET CHARACTER_SET_CLIENT = utf8mb4;
SET CHARACTER_SET_CONNECTION = utf8mb4;
SET CHARACTER_SET_RESULTS = utf8mb4;
SET collation_connection = utf8mb4_unicode_ci;

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `qn` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `qn`;

-- ===================================================================
-- 清除现有数据（重置数据库）
-- ===================================================================
-- 注意：这将删除所有现有数据，请谨慎使用
SET FOREIGN_KEY_CHECKS = 0;

-- 删除所有数据（按依赖关系的逆序）
DROP TABLE IF EXISTS `convergence_status`;
DROP TABLE IF EXISTS `world_events`;
DROP TABLE IF EXISTS `dice_rolls`;
DROP TABLE IF EXISTS `stability_anchors`;
DROP TABLE IF EXISTS `world_states`;
DROP TABLE IF EXISTS `chat_messages`;
DROP TABLE IF EXISTS `chat_sessions`;
DROP TABLE IF EXISTS `refresh_tokens`;
DROP TABLE IF EXISTS `world_templates`;
DROP TABLE IF EXISTS `users`;

SET FOREIGN_KEY_CHECKS = 1;

-- ===================================================================
-- 1. 基础用户系统表
-- ===================================================================

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
  `created_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
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
  `created_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_token` (`token`),
  KEY `FK_refresh_tokens_user_id` (`user_id`),
  CONSTRAINT `FK_refresh_tokens_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- 2. 聊天系统表（包含角色扮演扩展）
-- ===================================================================

-- 创建聊天会话表（包含角色扮演字段和评估系统）
CREATE TABLE IF NOT EXISTS `chat_sessions` (
  `session_id` varchar(255) NOT NULL,
  `title` varchar(500) NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `total_rounds` int DEFAULT 0 COMMENT '累计总轮数',
  `current_arc_start_round` int DEFAULT NULL COMMENT '当前情节起始轮数',
  `current_arc_name` varchar(255) DEFAULT NULL COMMENT '当前情节名称',
  -- 角色扮演相关字段
  `world_type` varchar(50) DEFAULT 'general' COMMENT '世界类型',
  `world_rules` JSON COMMENT '世界规则配置',
  `god_mode_rules` JSON COMMENT '上帝模式自定义规则',
  `world_state` JSON COMMENT '当前世界状态（重构后的结构化格式）',
  `skills_state` JSON COMMENT '技能状态（重构后的结构化格式）',
  `story_checkpoints` JSON COMMENT '故事检查点',
  `stability_anchor` JSON COMMENT '稳定性锚点',
  `version` int DEFAULT 1 COMMENT '状态版本号，用于乐观锁',
  `checksum` varchar(32) COMMENT '状态校验和',
  -- 评估系统相关字段
  `assessment_history` JSON COMMENT '评估历史记录',
  `last_assessment_id` BIGINT COMMENT '最后一次评估ID',
  `convergence_progress` DECIMAL(3,2) DEFAULT 0.0 COMMENT '当前收敛进度',
  `active_quests` JSON COMMENT '活跃任务列表',
  `completed_quests` JSON COMMENT '已完成任务列表',
  PRIMARY KEY (`session_id`),
  KEY `FK_chat_sessions_user_id` (`user_id`),
  KEY `IDX_user_updated` (`user_id`, `updated_at`),
  KEY `IDX_world_type` (`world_type`),
  KEY `IDX_convergence_progress` (`convergence_progress`),
  KEY `IDX_last_assessment` (`last_assessment_id`),
  CONSTRAINT `FK_chat_sessions_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建聊天消息表
CREATE TABLE IF NOT EXISTS `chat_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(255) NOT NULL,
  `role` enum('USER','ASSISTANT') NOT NULL,
  `content` longtext NOT NULL,
  `sequence_number` int NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `FK_chat_messages_session` (`session_id`),
  KEY `IDX_session_sequence` (`session_id`, `sequence_number`),
  CONSTRAINT `FK_chat_messages_session` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`session_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- 3. 角色扮演系统表
-- ===================================================================

-- 世界模板表
CREATE TABLE IF NOT EXISTS `world_templates` (
  `world_id` varchar(255) NOT NULL,
  `world_name` varchar(255) NOT NULL,
  `description` text,
  `system_prompt_template` text NOT NULL,
  `default_rules` JSON NOT NULL,
  `character_templates` JSON COMMENT '角色模板',
  `location_templates` JSON COMMENT '地点模板',
  `quest_templates` JSON COMMENT '任务模板',
  `stability_anchors` JSON COMMENT '稳定性锚点模板',
  `convergence_scenarios` JSON COMMENT '收敛场景集合',
  `dm_instructions` TEXT COMMENT 'DM行为指令',
  `convergence_rules` JSON COMMENT '收敛规则',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`world_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 世界状态表（用于状态历史和备份）
CREATE TABLE IF NOT EXISTS `world_states` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(255) NOT NULL,
  `current_location` JSON COMMENT '当前位置',
  `characters` JSON COMMENT '角色状态',
  `inventory` JSON COMMENT '物品清单',
  `active_quests` JSON COMMENT '活跃任务',
  `completed_quests` JSON COMMENT '已完成任务',
  `factions` JSON COMMENT '势力关系',
  `event_history` JSON COMMENT '事件历史',
  `version` int NOT NULL DEFAULT 1,
  `checksum` varchar(32) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `FK_world_states_session` (`session_id`),
  KEY `IDX_session_version` (`session_id`, `version`),
  CONSTRAINT `FK_world_states_session` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`session_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 稳定性锚点表
CREATE TABLE IF NOT EXISTS `stability_anchors` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(255) NOT NULL,
  `anchor_key` varchar(255) NOT NULL,
  `anchor_type` enum('WORLD_RULE','CHARACTER','LOCATION','QUEST','FACTION','ITEM','EVENT') NOT NULL,
  `anchor_value` text NOT NULL,
  `priority` int NOT NULL DEFAULT 1,
  `is_immutable` boolean NOT NULL DEFAULT FALSE,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `FK_stability_anchors_session` (`session_id`),
  KEY `IDX_session_type` (`session_id`, `anchor_type`),
  CONSTRAINT `FK_stability_anchors_session` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`session_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 世界事件表（事件溯源）
CREATE TABLE IF NOT EXISTS `world_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(255) NOT NULL,
  `event_type` enum('USER_ACTION','AI_RESPONSE','DICE_ROLL','QUEST_UPDATE','STATE_CHANGE','SKILL_USE','LOCATION_CHANGE','CHARACTER_UPDATE','MEMORY_UPDATE','SYSTEM_EVENT') NOT NULL,
  `event_data` JSON NOT NULL,
  `sequence` int NOT NULL,
  `total_rounds` int DEFAULT NULL COMMENT '事件时会话总轮数',
  `current_arc_start_round` int DEFAULT NULL COMMENT '事件时情节起始轮数',
  `current_arc_name` varchar(255) DEFAULT NULL COMMENT '事件时情节名称',
  `timestamp` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `checksum` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_world_events_session` (`session_id`),
  KEY `IDX_session_sequence` (`session_id`, `sequence`),
  KEY `IDX_event_type` (`event_type`),
  UNIQUE KEY `UK_session_sequence` (`session_id`, `sequence`),
  CONSTRAINT `FK_world_events_session` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`session_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 骰子记录表
CREATE TABLE IF NOT EXISTS `dice_rolls` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(255) NOT NULL,
  `roll_id` varchar(255) NOT NULL,
  `dice_type` int NOT NULL COMMENT '骰子类型（如20表示d20）',
  `num_dice` int NOT NULL DEFAULT 1 COMMENT '骰子数量',
  `modifier` int NOT NULL DEFAULT 0 COMMENT '修正值',
  `result` int NOT NULL COMMENT '骰子结果',
  `final_result` int NOT NULL COMMENT '最终结果（骰子+修正）',
  `reason` varchar(500) COMMENT '检定原因',
  `difficulty_class` int COMMENT '难度等级',
  `is_successful` boolean COMMENT '是否成功',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `FK_dice_rolls_session` (`session_id`),
  KEY `IDX_roll_id` (`roll_id`),
  KEY `IDX_created_at` (`created_at`),
  CONSTRAINT `FK_dice_rolls_session` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`session_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 收敛状态表（存储故事收敛进度和状态）
CREATE TABLE IF NOT EXISTS `convergence_status` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(255) NOT NULL,
  `progress` double NOT NULL DEFAULT 0.0 COMMENT '整体收敛进度 (0-1)',
  `nearest_scenario_id` varchar(255) COMMENT '最近的收敛场景ID',
  `nearest_scenario_title` varchar(255) COMMENT '最近场景标题',
  `distance_to_nearest` double COMMENT '到最近场景的距离',
  `scenario_progress` JSON COMMENT '所有场景进度JSON',
  `active_hints` JSON COMMENT '当前活跃的引导提示',
  `last_updated` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `FK_convergence_status_session` (`session_id`),
  KEY `IDX_progress` (`progress`),
  CONSTRAINT `FK_convergence_status_session` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`session_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ===================================================================
-- 4. 初始化数据
-- ===================================================================

-- 插入默认管理员用户（密码: admin123）
INSERT IGNORE INTO `users` (`username`, `email`, `password`, `role`, `created_at`, `updated_at`) VALUES
('admin', 'admin@qncontest.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'ADMIN', NOW(), NOW());

-- 插入默认测试用户（密码: 123456）
INSERT IGNORE INTO `users` (`username`, `email`, `password`, `role`, `created_at`, `updated_at`) VALUES
('panzijian', 'panzijian@example.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'USER', NOW(), NOW());

-- ===================================================================
-- 5. 创建索引和优化
-- ===================================================================

-- 为频繁查询的字段创建复合索引
CREATE INDEX `IDX_chat_sessions_user_world` ON `chat_sessions` (`user_id`, `world_type`, `updated_at`);
CREATE INDEX `IDX_world_events_session_type` ON `world_events` (`session_id`, `event_type`, `timestamp`);
CREATE INDEX `IDX_dice_rolls_session_time` ON `dice_rolls` (`session_id`, `created_at`);

-- ===================================================================
-- 6. 显示创建结果
-- ===================================================================

-- 显示所有表
SHOW TABLES;

-- 显示用户数据
SELECT id, username, email, role, created_at FROM users;

-- 显示世界模板（包含新字段）
SELECT
    world_id,
    world_name,
    description,
    JSON_EXTRACT(convergence_scenarios, '$.story_convergence_1.title') as story_start,
    JSON_EXTRACT(convergence_scenarios, '$.main_convergence.title') as main_scenario,
    JSON_EXTRACT(convergence_scenarios, '$.alternative_convergence.title') as alt_scenario,
    JSON_EXTRACT(convergence_rules, '$.convergence_threshold') as threshold
FROM world_templates;

-- 显示数据库初始化完成信息
SELECT
    'Database initialization completed successfully!' as status,
    NOW() as completed_at,
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'qn') as total_tables,
    (SELECT COUNT(*) FROM users) as total_users,
    (SELECT COUNT(*) FROM world_templates) as total_world_templates,
    (SELECT COUNT(*) FROM chat_sessions) as total_sessions,
    (SELECT COUNT(*) FROM chat_messages) as total_messages;

-- 重置自增序列（所有表创建完毕后执行）
ALTER TABLE `users` AUTO_INCREMENT = 1;
ALTER TABLE `chat_messages` AUTO_INCREMENT = 1;
ALTER TABLE `world_states` AUTO_INCREMENT = 1;
ALTER TABLE `stability_anchors` AUTO_INCREMENT = 1;
ALTER TABLE `world_events` AUTO_INCREMENT = 1;
ALTER TABLE `dice_rolls` AUTO_INCREMENT = 1;
ALTER TABLE `refresh_tokens` AUTO_INCREMENT = 1;



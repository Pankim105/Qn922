-- ===================================================================
-- QN Contest 角色扮演AI系统 - 完整数据库初始化脚本
-- 版本: 2.0
-- 创建时间: 2025-09-23
-- 说明: 包含基础用户系统、聊天系统和角色扮演扩展功能
-- ===================================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `qn` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `qn`;

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

-- 创建聊天会话表（包含角色扮演字段）
CREATE TABLE IF NOT EXISTS `chat_sessions` (
  `session_id` varchar(255) NOT NULL,
  `title` varchar(500) NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  -- 角色扮演相关字段
  `world_type` varchar(50) DEFAULT 'general' COMMENT '世界类型',
  `world_rules` JSON COMMENT '世界规则配置',
  `god_mode_rules` JSON COMMENT '上帝模式自定义规则',
  `world_state` JSON COMMENT '当前世界状态',
  `skills_state` JSON COMMENT '技能状态（任务、学习进度等）',
  `story_checkpoints` JSON COMMENT '故事检查点',
  `stability_anchor` JSON COMMENT '稳定性锚点',
  `version` int DEFAULT 1 COMMENT '状态版本号，用于乐观锁',
  `checksum` varchar(32) COMMENT '状态校验和',
  PRIMARY KEY (`session_id`),
  KEY `FK_chat_sessions_user_id` (`user_id`),
  KEY `IDX_user_updated` (`user_id`, `updated_at`),
  KEY `IDX_world_type` (`world_type`),
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
  `event_type` enum('USER_ACTION','AI_RESPONSE','DICE_ROLL','QUEST_UPDATE','STATE_CHANGE','SKILL_USE','LOCATION_CHANGE','CHARACTER_UPDATE','SYSTEM_EVENT') NOT NULL,
  `event_data` JSON NOT NULL,
  `sequence` int NOT NULL,
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
  `timestamp` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `seed` varchar(255) COMMENT '随机种子（用于重现）',
  PRIMARY KEY (`id`),
  KEY `FK_dice_rolls_session` (`session_id`),
  KEY `IDX_roll_id` (`roll_id`),
  KEY `IDX_timestamp` (`timestamp`),
  CONSTRAINT `FK_dice_rolls_session` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`session_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- 4. 初始化数据
-- ===================================================================

-- 插入默认管理员用户（密码: admin123）
INSERT IGNORE INTO `users` (`username`, `email`, `password`, `role`, `created_at`, `updated_at`) VALUES
('admin', 'admin@qncontest.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'ADMIN', NOW(), NOW());

-- 插入默认测试用户（密码: 123456）
INSERT IGNORE INTO `users` (`username`, `email`, `password`, `role`, `created_at`, `updated_at`) VALUES
('panzijian1234', 'panzijian@example.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'USER', NOW(), NOW());

-- 插入世界模板数据
INSERT IGNORE INTO `world_templates` (`world_id`, `world_name`, `description`, `system_prompt_template`, `default_rules`) VALUES
('fantasy_adventure', '异世界探险', '经典的奇幻冒险世界，充满魔法、怪物和宝藏', 
 '你是一个奇幻世界的游戏主持人。这个世界充满了魔法、神秘生物和古老的传说。用户是一名冒险者，你需要为他们创造引人入胜的冒险故事。规则: {world_rules}。当前状态: {world_state}', 
 JSON_OBJECT('magic_system', '经典魔法体系', 'technology_level', '中世纪', 'danger_level', '中等')),

('western_magic', '西方魔幻', '西式魔法世界，包含法师、骑士和龙', 
 '你是西方魔幻世界的向导。这里有强大的法师、勇敢的骑士、神秘的龙族和各种魔法生物。为用户创造史诗般的冒险。规则: {world_rules}。当前状态: {world_state}', 
 JSON_OBJECT('magic_schools', JSON_ARRAY('元素魔法', '神圣魔法', '暗黑魔法'), 'guilds', true, 'dragons', true)),

('martial_arts', '东方武侠', '充满武功、江湖恩怨的武侠世界', 
 '你是武侠世界的说书人。这里有各种武功秘籍、江湖门派、侠客义士。用户将体验刀光剑影的江湖生活。规则: {world_rules}。当前状态: {world_state}', 
 JSON_OBJECT('martial_arts', true, 'sects', JSON_ARRAY('少林', '武当', '峨眉'), 'weapons', JSON_ARRAY('剑', '刀', '拳法'))),

('japanese_school', '日式校园', '现代日本校园生活，充满青春与友情', 
 '你是日式校园生活的叙述者。这里有社团活动、校园祭典、青春恋爱和友情故事。为用户创造温馨的校园体验。规则: {world_rules}。当前状态: {world_state}', 
 JSON_OBJECT('setting', '现代日本', 'school_type', '高中', 'clubs', true, 'festivals', true)),

('educational', '寓教于乐', '教育性世界，通过互动学习知识', 
 '你是一个教育向导，通过角色扮演和互动故事帮助用户学习各种知识。让学习变得有趣和难忘。规则: {world_rules}。当前状态: {world_state}', 
 JSON_OBJECT('subjects', JSON_ARRAY('数学', '历史', '科学', '语言'), 'interactive', true, 'gamified', true));

-- ===================================================================
-- 5. 创建索引和优化
-- ===================================================================

-- 为频繁查询的字段创建复合索引
CREATE INDEX IF NOT EXISTS `IDX_chat_sessions_user_world` ON `chat_sessions` (`user_id`, `world_type`, `updated_at`);
CREATE INDEX IF NOT EXISTS `IDX_world_events_session_type` ON `world_events` (`session_id`, `event_type`, `timestamp`);
CREATE INDEX IF NOT EXISTS `IDX_dice_rolls_session_time` ON `dice_rolls` (`session_id`, `timestamp`);

-- ===================================================================
-- 6. 显示创建结果
-- ===================================================================

-- 显示所有表
SHOW TABLES;

-- 显示用户数据
SELECT id, username, email, role, created_at FROM users;

-- 显示世界模板
SELECT world_id, world_name, description FROM world_templates;

-- 显示数据库初始化完成信息
SELECT 
    'Database initialization completed successfully!' as status,
    NOW() as completed_at,
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'qn') as total_tables,
    (SELECT COUNT(*) FROM users) as total_users,
    (SELECT COUNT(*) FROM world_templates) as total_world_templates;

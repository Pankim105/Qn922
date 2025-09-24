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
DROP TABLE IF EXISTS `dm_assessments`;
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
  `character_stats` JSON COMMENT '角色属性（等级、经验、金币等）',
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

-- DM评估表（存储大模型的智能评估结果）
CREATE TABLE IF NOT EXISTS `dm_assessments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(255) NOT NULL,
  `rule_compliance` decimal(3,2) NOT NULL COMMENT '规则合规性 (0-1)',
  `context_consistency` decimal(3,2) NOT NULL COMMENT '上下文一致性 (0-1)',
  `convergence_progress` decimal(3,2) NOT NULL COMMENT '收敛推进度 (0-1)',
  `overall_score` decimal(3,2) NOT NULL COMMENT '综合评分 (0-1)',
  `strategy` enum('ACCEPT','ADJUST','CORRECT') NOT NULL COMMENT '评估策略',
  `assessment_notes` text COMMENT '评估说明',
  `suggested_actions` JSON COMMENT '建议行动列表',
  `convergence_hints` JSON COMMENT '收敛提示列表',
  `quest_updates` JSON COMMENT '任务更新信息（完成、进度、奖励等）',
  `world_state_updates` JSON COMMENT '世界状态更新',
  `skills_state_updates` JSON COMMENT '技能状态更新',
  `assessed_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '评估时间',
  `user_action` text COMMENT '用户行为描述',
  PRIMARY KEY (`id`),
  KEY `FK_dm_assessments_session` (`session_id`),
  KEY `IDX_assessed_at` (`assessed_at`),
  KEY `IDX_strategy` (`strategy`),
  KEY `IDX_overall_score` (`overall_score`),
  CONSTRAINT `FK_dm_assessments_session` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`session_id`) ON DELETE CASCADE
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
INSERT IGNORE INTO `world_templates` (`world_id`, `world_name`, `description`, `system_prompt_template`, `default_rules`, `convergence_scenarios`, `dm_instructions`, `convergence_rules`) VALUES
('fantasy_adventure', '异世界探险', '经典的奇幻冒险世界，充满魔法、怪物和宝藏',
 '你是一个奇幻世界的游戏主持人。这个世界充满了魔法、神秘生物和古老的传说。用户是一名冒险者，你需要为他们创造引人入胜的冒险故事。规则: {world_rules}。当前状态: {world_state}',
 JSON_OBJECT('magic_system', '经典魔法体系', 'technology_level', '中世纪', 'danger_level', '中等'),
 JSON_OBJECT(
   'story_convergence_1', JSON_OBJECT(
     'scenario_id', 'village_crisis',
     'title', '村庄危机',
     'description', '玩家所在的村庄遭受怪物袭击，发现远古预言',
     'trigger_conditions', JSON_ARRAY('explored_starting_area', 'encountered_first_enemy'),
     'required_elements', JSON_ARRAY('player_character', 'village', 'ancient_scroll'),
     'leads_to', 'story_convergence_2',
     'outcomes', JSON_ARRAY('discover_prophecy', 'village_saved', 'mysterious_survivor')
   ),
   'story_convergence_2', JSON_OBJECT(
     'scenario_id', 'ancient_temple',
     'title', '远古神庙',
     'description', '根据预言前往远古神庙，寻找失落的魔法物品',
     'trigger_conditions', JSON_ARRAY('completed_village_quest', 'gathered_party_members'),
     'required_elements', JSON_ARRAY('magic_artifact', 'temple_guardians', 'puzzles'),
     'leads_to', 'story_convergence_3',
     'outcomes', JSON_ARRAY('artifact_found', 'guardian_defeated', 'temple_collapse')
   ),
   'story_convergence_3', JSON_OBJECT(
     'scenario_id', 'dragon_lair',
     'title', '巨龙巢穴',
     'description', '追踪魔法物品的源头，来到巨龙巢穴',
     'trigger_conditions', JSON_ARRAY('found_ancient_temple', 'decoded_prophecy'),
     'required_elements', JSON_ARRAY('dragon', 'magic_sword', 'treasure_hoard'),
     'leads_to', 'story_convergence_4',
     'outcomes', JSON_ARRAY('dragon_encountered', 'treasure_discovered', 'lair_explored')
   ),
   'main_convergence', JSON_OBJECT(
     'scenario_id', 'dragon_slaying',
     'title', '屠龙英雄',
     'description', '击败巨龙，成为传说中的英雄，终结预言中的灾难',
     'trigger_conditions', JSON_ARRAY('entered_dragon_lair', 'prepared_for_final_battle'),
     'required_elements', JSON_ARRAY('hero', 'dragon', 'magic_sword'),
     'outcomes', JSON_ARRAY('victory', 'defeat', 'compromise')
   ),
   'alternative_convergence', JSON_OBJECT(
     'scenario_id', 'kingdom_savior',
     'title', '王国救世主',
     'description', '拯救王国于危难之中，建立新的秩序',
     'trigger_conditions', JSON_ARRAY('player_chose_diplomatic_approach', 'maintained_peace_long_enough')
   )
 ),
 '作为奇幻世界的DM，你需要平衡魔法与现实，鼓励英雄主义行为，同时确保故事的连贯性和趣味性。',
 JSON_OBJECT('convergence_threshold', 0.8, 'max_exploration_turns', 50, 'story_completeness_required', 0.7)),

('western_magic', '西方魔幻', '西式魔法世界，包含法师、骑士和龙',
 '你是西方魔幻世界的向导。这里有强大的法师、勇敢的骑士、神秘的龙族和各种魔法生物。为用户创造史诗般的冒险。规则: {world_rules}。当前状态: {world_state}',
 JSON_OBJECT('magic_schools', JSON_ARRAY('元素魔法', '神圣魔法', '暗黑魔法'), 'guilds', true, 'dragons', true),
 JSON_OBJECT(
   'story_convergence_1', JSON_OBJECT(
     'scenario_id', 'academy_enrollment',
     'title', '魔法学院入学',
     'description', '玩家进入著名的魔法学院，开始学习魔法',
     'trigger_conditions', JSON_ARRAY('showed_magical_talent', 'passed_entrance_exam'),
     'required_elements', JSON_ARRAY('student_mage', 'spellbook', 'wand'),
     'leads_to', 'story_convergence_2',
     'outcomes', JSON_ARRAY('accepted_into_academy', 'met_rival_student', 'discovered_hidden_talent')
   ),
   'story_convergence_2', JSON_OBJECT(
     'scenario_id', 'forbidden_library',
     'title', '禁忌图书馆',
     'description', '意外发现学院的禁忌图书馆，接触失传的魔法',
     'trigger_conditions', JSON_ARRAY('advanced_in_studies', 'broke_academy_rules'),
     'required_elements', JSON_ARRAY('forbidden_knowledge', 'ancient_tomes', 'magical_traps'),
     'leads_to', 'story_convergence_3',
     'outcomes', JSON_ARRAY('knowledge_gained', 'curse_acquired', 'professor_suspicion')
   ),
   'story_convergence_3', JSON_OBJECT(
     'scenario_id', 'dragon_sanctuary',
     'title', '龙族圣殿',
     'description', '根据古籍中的线索，寻找传说中的龙族圣殿',
     'trigger_conditions', JSON_ARRAY('decoded_ancient_texts', 'mastered_forbidden_spells'),
     'required_elements', JSON_ARRAY('dragon_guardians', 'ancient_artifacts', 'magical_barriers'),
     'leads_to', 'main_convergence',
     'outcomes', JSON_ARRAY('sanctuary_found', 'dragon_alliance', 'magical_awakening')
   ),
   'main_convergence', JSON_OBJECT(
     'scenario_id', 'archmage_trial',
     'title', '大法师试炼',
     'description', '通过魔法学院的最终试炼，成为大法师',
     'trigger_conditions', JSON_ARRAY('player_mastered_magic', 'completed_academy_quests'),
     'required_elements', JSON_ARRAY('mage', 'spellbook', 'magic_crystal'),
     'outcomes', JSON_ARRAY('ascension', 'failure', 'alternative_path')
   ),
   'alternative_convergence', JSON_OBJECT(
     'scenario_id', 'dragon_peace',
     'title', '龙族和平',
     'description', '与龙族达成和平协议，建立魔法与龙族的联盟',
     'trigger_conditions', JSON_ARRAY('player_negotiated_with_dragons', 'prevented_war')
   )
 ),
 '作为西方魔幻世界的DM，你需要维护魔法世界的平衡，鼓励玩家探索不同的魔法流派，同时引导故事向史诗般的结局发展。',
 JSON_OBJECT('convergence_threshold', 0.75, 'max_exploration_turns', 40, 'story_completeness_required', 0.8)),

('martial_arts', '东方武侠', '充满武功、江湖恩怨的武侠世界',
 '你是武侠世界的说书人。这里有各种武功秘籍、江湖门派、侠客义士。用户将体验刀光剑影的江湖生活。规则: {world_rules}。当前状态: {world_state}',
 JSON_OBJECT('martial_arts', true, 'sects', JSON_ARRAY('少林', '武当', '峨眉'), 'weapons', JSON_ARRAY('剑', '刀', '拳法')),
 JSON_OBJECT(
   'story_convergence_1', JSON_OBJECT(
     'scenario_id', 'sect_origin',
     'title', '师门恩怨',
     'description', '玩家出身的门派遭受仇家袭击，揭开家族秘密',
     'trigger_conditions', JSON_ARRAY('completed_basic_training', 'witnessed_attack'),
     'required_elements', JSON_ARRAY('young_warrior', 'sect_members', 'ancient_letter'),
     'leads_to', 'story_convergence_2',
     'outcomes', JSON_ARRAY('survived_attack', 'discovered_conspiracy', 'met_mentor_figure')
   ),
   'story_convergence_2', JSON_OBJECT(
     'scenario_id', 'treasure_hunt',
     'title', '秘籍寻踪',
     'description', '根据线索寻找失传的武功秘籍，游历江湖',
     'trigger_conditions', JSON_ARRAY('left_home_sect', 'gathered_allies'),
     'required_elements', JSON_ARRAY('martial_arts_manual', 'sect_rivals', 'hidden_locations'),
     'leads_to', 'story_convergence_3',
     'outcomes', JSON_ARRAY('manual_found', 'rival_defeated', 'power_increased')
   ),
   'story_convergence_3', JSON_OBJECT(
     'scenario_id', 'grand_tournament',
     'title', '武林大会',
     'description', '参加武林大会，与各派高手一较高下',
     'trigger_conditions', JSON_ARRAY('mastered_secret_techniques', 'gained_reputation'),
     'required_elements', JSON_ARRAY('tournament_competitors', 'legendary_weapon', 'judges_panel'),
     'leads_to', 'main_convergence',
     'outcomes', JSON_ARRAY('tournament_joined', 'alliances_formed', 'secrets_revealed')
   ),
   'main_convergence', JSON_OBJECT(
     'scenario_id', 'wulin_supremacy',
     'title', '武林至尊',
     'description', '在武林大会上证明自己的武功天下第一，成为武林盟主',
     'trigger_conditions', JSON_ARRAY('player_mastered_martial_arts', 'defeated_all_sects'),
     'required_elements', JSON_ARRAY('warrior', 'legendary_weapon', 'inner_power'),
     'outcomes', JSON_ARRAY('martial_supremacy', 'sect_leader', 'hidden_master')
   ),
   'alternative_convergence', JSON_OBJECT(
     'scenario_id', 'jianghu_peace',
     'title', '江湖和平',
     'description', '化解各大门派恩怨，建立武林和平，结束江湖争斗',
     'trigger_conditions', JSON_ARRAY('player_mediated_conflicts', 'united_factions')
   )
 ),
 '作为武侠世界的DM，你需要维护江湖的道义和规矩，鼓励侠义行为，同时引导故事向武林传奇的方向发展。',
 JSON_OBJECT('convergence_threshold', 0.7, 'max_exploration_turns', 35, 'story_completeness_required', 0.75)),

('japanese_school', '日式校园', '现代日本校园生活，充满青春与友情',
 '你是日式校园生活的叙述者。这里有社团活动、校园祭典、青春恋爱和友情故事。为用户创造温馨的校园体验。规则: {world_rules}。当前状态: {world_state}',
 JSON_OBJECT('setting', '现代日本', 'school_type', '高中', 'clubs', true, 'festivals', true),
 JSON_OBJECT(
   'story_convergence_1', JSON_OBJECT(
     'scenario_id', 'school_transfer',
     'title', '转校生的到来',
     'description', '玩家作为转校生来到新学校，面对陌生的环境',
     'trigger_conditions', JSON_ARRAY('enrolled_in_school', 'first_day_anxiety'),
     'required_elements', JSON_ARRAY('new_student', 'classroom', 'school_uniform'),
     'leads_to', 'story_convergence_2',
     'outcomes', JSON_ARRAY('made_first_friends', 'joined_first_club', 'discovered_school_secret')
   ),
   'story_convergence_2', JSON_OBJECT(
     'scenario_id', 'club_activities',
     'title', '社团活动',
     'description', '参加社团活动，发展兴趣爱好，结识志同道合的朋友',
     'trigger_conditions', JSON_ARRAY('settled_in_school', 'showed_talent_in_activity'),
     'required_elements', JSON_ARRAY('club_members', 'club_room', 'upcoming_event'),
     'leads_to', 'story_convergence_3',
     'outcomes', JSON_ARRAY('club_bond_formed', 'skill_improved', 'challenge_arose')
   ),
   'story_convergence_3', JSON_OBJECT(
     'scenario_id', 'friendship_trials',
     'title', '友情考验',
     'description', '面对朋友间的误会和考验，学会珍惜和维护友谊',
     'trigger_conditions', JSON_ARRAY('deepened_relationships', 'faced_interpersonal_conflict'),
     'required_elements', JSON_ARRAY('close_friends', 'misunderstanding', 'school_event'),
     'leads_to', 'main_convergence',
     'outcomes', JSON_ARRAY('conflict_resolved', 'friendship_strengthened', 'personal_growth')
   ),
   'main_convergence', JSON_OBJECT(
     'scenario_id', 'school_festival_success',
     'title', '校园祭典的成功',
     'description', '成功举办校园祭典，成为学生会的核心成员，留下美好回忆',
     'trigger_conditions', JSON_ARRAY('joined_student_council', 'organized_successful_events'),
     'required_elements', JSON_ARRAY('student', 'friends', 'club_activities'),
     'outcomes', JSON_ARRAY('student_council_president', 'club_leader', 'graduation_memory')
   ),
   'alternative_convergence', JSON_OBJECT(
     'scenario_id', 'youth_romance',
     'title', '青春恋爱物语',
     'description', '发展一段美好的校园恋情，体验青涩的青春',
     'trigger_conditions', JSON_ARRAY('built_close_relationships', 'experienced_school_life')
   )
 ),
 '作为校园生活的DM，你需要营造温暖、青春洋溢的氛围，鼓励玩家参与社团活动和人际交往，同时引导故事向成长和回忆的方向发展。',
 JSON_OBJECT('convergence_threshold', 0.6, 'max_exploration_turns', 25, 'story_completeness_required', 0.65)),

('educational', '寓教于乐', '教育性世界，通过互动学习知识',
 '你是一个教育向导，通过角色扮演和互动故事帮助用户学习各种知识。让学习变得有趣和难忘。规则: {world_rules}。当前状态: {world_state}',
 JSON_OBJECT('subjects', JSON_ARRAY('数学', '历史', '科学', '语言'), 'interactive', true, 'gamified', true),
 JSON_OBJECT(
   'story_convergence_1', JSON_OBJECT(
     'scenario_id', 'learning_begins',
     'title', '学习之旅启程',
     'description', '玩家开始学习之旅，选择感兴趣的学科',
     'trigger_conditions', JSON_ARRAY('enrolled_in_courses', 'showed_initial_interest'),
     'required_elements', JSON_ARRAY('curious_student', 'textbooks', 'learning_goals'),
     'leads_to', 'story_convergence_2',
     'outcomes', JSON_ARRAY('basic_knowledge_acquired', 'study_habit_formed', 'mentor_found')
   ),
   'story_convergence_2', JSON_OBJECT(
     'scenario_id', 'challenge_accepted',
     'title', '挑战与突破',
     'description', '面对学习中的困难和挑战，寻找解决方案',
     'trigger_conditions', JSON_ARRAY('encountered_learning_obstacles', 'sought_help'),
     'required_elements', JSON_ARRAY('difficult_problems', 'study_group', 'learning_resources'),
     'leads_to', 'story_convergence_3',
     'outcomes', JSON_ARRAY('problem_solved', 'confidence_gained', 'method_mastered')
   ),
   'story_convergence_3', JSON_OBJECT(
     'scenario_id', 'knowledge_application',
     'title', '知识应用实践',
     'description', '将学到的知识应用到实际问题和项目中',
     'trigger_conditions', JSON_ARRAY('mastered_core_concepts', 'found_practical_use'),
     'required_elements', JSON_ARRAY('real_world_problems', 'projects', 'collaboration'),
     'leads_to', 'main_convergence',
     'outcomes', JSON_ARRAY('practical_success', 'innovation_achieved', 'impact_made')
   ),
   'main_convergence', JSON_OBJECT(
     'scenario_id', 'knowledge_master',
     'title', '知识大师',
     'description', '掌握所有学科知识，成为全能学者，开启学术生涯',
     'trigger_conditions', JSON_ARRAY('completed_all_subjects', 'achieved_high_scores'),
     'required_elements', JSON_ARRAY('student', 'study_materials', 'practice_tests'),
     'outcomes', JSON_ARRAY('academic_excellence', 'teaching_career', 'research_path')
   ),
   'alternative_convergence', JSON_OBJECT(
     'scenario_id', 'practical_application',
     'title', '学以致用',
     'description', '将所学知识应用到实际问题解决中，成为实践专家',
     'trigger_conditions', JSON_ARRAY('applied_knowledge_practically', 'solved_real_world_problems')
   )
 ),
 '作为教育世界的DM，你需要让学习变得有趣和互动，鼓励玩家积极参与知识探索，同时确保教育目标的达成。',
 JSON_OBJECT('convergence_threshold', 0.85, 'max_exploration_turns', 30, 'story_completeness_required', 0.9)),

('sci_fi', '科幻探险', '未来科技世界，包含太空旅行、AI和外星文明',
 '你是科幻世界的AI向导。这里有先进的科技、太空探索、外星文明和未来社会。为用户创造科幻冒险故事。规则: {world_rules}。当前状态: {world_state}',
 JSON_OBJECT('technology_level', '高度发达', 'space_travel', true, 'ai_systems', true, 'alien_civilizations', true),
 JSON_OBJECT(
   'story_convergence_1', JSON_OBJECT(
     'scenario_id', 'first_contact',
     'title', '首次接触',
     'description', '玩家发现外星信号，开启太空探索之旅',
     'trigger_conditions', JSON_ARRAY('detected_anomaly', 'accessed_space_agency'),
     'required_elements', JSON_ARRAY('young_scientist', 'research_lab', 'communication_signal'),
     'leads_to', 'story_convergence_2',
     'outcomes', JSON_ARRAY('signal_decoded', 'team_assembled', 'mission_launched')
   ),
   'story_convergence_2', JSON_OBJECT(
     'scenario_id', 'deep_space',
     'title', '深空探索',
     'description', '深入太空，面对未知的太空现象和挑战',
     'trigger_conditions', JSON_ARRAY('entered_deep_space', 'encountered_first_anomaly'),
     'required_elements', JSON_ARRAY('spaceship', 'crew_members', 'advanced_sensors'),
     'leads_to', 'story_convergence_3',
     'outcomes', JSON_ARRAY('anomaly_investigated', 'technology_upgraded', 'ally_encountered')
   ),
   'story_convergence_3', JSON_OBJECT(
     'scenario_id', 'alien_civilization',
     'title', '外星文明',
     'description', '发现外星文明，面对外交和生存挑战',
     'trigger_conditions', JSON_ARRAY('found_alien_artifacts', 'established_communication'),
     'required_elements', JSON_ARRAY('alien_technology', 'diplomatic_relations', 'cultural_exchange'),
     'leads_to', 'main_convergence',
     'outcomes', JSON_ARRAY('alliance_formed', 'conflict_arose', 'technology_shared')
   ),
   'main_convergence', JSON_OBJECT(
     'scenario_id', 'galaxy_savior',
     'title', '银河系救世主',
     'description', '阻止外星入侵，拯救银河系，建立新秩序',
     'trigger_conditions', JSON_ARRAY('discovered_alien_threat', 'assembled_hero_team'),
     'required_elements', JSON_ARRAY('scientist', 'advanced_tech', 'spaceship'),
     'outcomes', JSON_ARRAY('victory', 'compromise', 'new_alliance')
   ),
   'alternative_convergence', JSON_OBJECT(
     'scenario_id', 'ai_awakening',
     'title', 'AI觉醒',
     'description', '见证AI系统的觉醒和进化，探索意识的本质',
     'trigger_conditions', JSON_ARRAY('ai_systems_developed', 'ethical_dilemmas_resolved')
   )
 ),
 '作为科幻世界的DM，你需要平衡科技与人性，探索未来社会的可能性，同时引导故事向宏大的宇宙叙事发展。',
 JSON_OBJECT('convergence_threshold', 0.75, 'max_exploration_turns', 45, 'story_completeness_required', 0.8)),

('detective', '侦探推理', '充满谜团和阴谋的侦探世界',
 '你是侦探故事的叙述者。这里有复杂的案件、神秘的嫌疑人、隐藏的线索和惊人的真相。为用户创造悬疑推理体验。规则: {world_rules}。当前状态: {world_state}',
 JSON_OBJECT('mystery_level', '高度复杂', 'clues', true, 'red_herrings', true, 'multiple_endings', true),
 JSON_OBJECT(
   'story_convergence_1', JSON_OBJECT(
     'scenario_id', 'first_case',
     'title', '初出茅庐',
     'description', '玩家作为新人侦探接手第一个案件',
     'trigger_conditions', JSON_ARRAY('joined_detective_agency', 'assigned_first_case'),
     'required_elements', JSON_ARRAY('rookie_detective', 'case_file', 'crime_scene'),
     'leads_to', 'story_convergence_2',
     'outcomes', JSON_ARRAY('first_clue_found', 'suspect_identified', 'method_learned')
   ),
   'story_convergence_2', JSON_OBJECT(
     'scenario_id', 'complex_web',
     'title', '复杂关系网',
     'description', '发现案件背后复杂的利益关系和人物关系',
     'trigger_conditions', JSON_ARRAY('gathered_evidence', 'interviewed_witnesses'),
     'required_elements', JSON_ARRAY('suspect_list', 'motive_analysis', 'alibi_check'),
     'leads_to', 'story_convergence_3',
     'outcomes', JSON_ARRAY('pattern_discovered', 'false_lead_identified', 'connection_made')
   ),
   'story_convergence_3', JSON_OBJECT(
     'scenario_id', 'breakthrough',
     'title', '关键突破',
     'description', '找到关键证据，接近真相的核心',
     'trigger_conditions', JSON_ARRAY('followed_all_leads', 'analyzed_all_evidence'),
     'required_elements', JSON_ARRAY('key_evidence', 'witness_testimony', 'forensic_analysis'),
     'leads_to', 'main_convergence',
     'outcomes', JSON_ARRAY('breakthrough_achieved', 'conspiracy_uncovered', 'culprit_identified')
   ),
   'main_convergence', JSON_OBJECT(
     'scenario_id', 'master_detective',
     'title', '名侦探',
     'description', '破解所有谜团，成为传奇侦探，伸张正义',
     'trigger_conditions', JSON_ARRAY('solved_all_cases', 'uncovered_main_conspiracy'),
     'required_elements', JSON_ARRAY('detective', 'evidence', 'interrogation_skills'),
     'outcomes', JSON_ARRAY('justice_served', 'unexpected_twist', 'personal_redemption')
   ),
   'alternative_convergence', JSON_OBJECT(
     'scenario_id', 'hidden_truth',
     'title', '隐藏的真相',
     'description', '发现比表面更深层的阴谋，揭露更大规模的犯罪网络',
     'trigger_conditions', JSON_ARRAY('followed_unexpected_leads', 'questioned_assumptions')
   )
 ),
 '作为侦探世界的DM，你需要制造紧张感和悬疑氛围，合理安排线索和误导，同时引导故事向真相大白的方向发展。',
 JSON_OBJECT('convergence_threshold', 0.8, 'max_exploration_turns', 25, 'story_completeness_required', 0.85));

-- ===================================================================
-- 5. 创建索引和优化
-- ===================================================================

-- 为频繁查询的字段创建复合索引
CREATE INDEX `IDX_chat_sessions_user_world` ON `chat_sessions` (`user_id`, `world_type`, `updated_at`);
CREATE INDEX `IDX_world_events_session_type` ON `world_events` (`session_id`, `event_type`, `timestamp`);
CREATE INDEX `IDX_dice_rolls_session_time` ON `dice_rolls` (`session_id`, `timestamp`);

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
ALTER TABLE `dm_assessments` AUTO_INCREMENT = 1;
ALTER TABLE `refresh_tokens` AUTO_INCREMENT = 1;



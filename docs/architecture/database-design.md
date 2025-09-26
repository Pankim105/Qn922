# 数据库设计文档

QN Contest采用MySQL数据库，设计遵循第三范式，支持高并发访问和数据一致性。

## 数据库概览

### 基本信息
- **数据库类型**: MySQL 8.0+
- **字符集**: utf8mb4
- **排序规则**: utf8mb4_unicode_ci
- **存储引擎**: InnoDB
- **连接池**: HikariCP

### 设计原则
- **规范化**: 遵循第三范式，减少数据冗余
- **性能优化**: 合理设计索引，优化查询性能
- **数据完整性**: 使用外键约束和检查约束
- **扩展性**: 支持水平扩展和功能扩展

## 表结构设计

### 1. 用户管理表

#### users 表
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_created_at (created_at)
);
```

**字段说明**:
- `id`: 用户唯一标识
- `username`: 用户名，唯一约束
- `password`: 加密后的密码
- `email`: 邮箱地址，唯一约束
- `created_at`: 创建时间
- `updated_at`: 更新时间
- `is_active`: 账户状态

### 2. 会话管理表

#### chat_sessions 表
```sql
CREATE TABLE chat_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(100) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    world_type VARCHAR(50),
    world_state TEXT,
    skills_state TEXT,
    world_rules TEXT,
    god_mode_rules TEXT,
    character_stats TEXT,
    active_quests TEXT,
    completed_quests TEXT,
    assessment_history TEXT,
    story_checkpoints TEXT,
    stability_anchor TEXT,
    convergence_progress DOUBLE DEFAULT 0.0,
    current_arc_name VARCHAR(200),
    current_arc_start_round INT DEFAULT 1,
    total_rounds INT DEFAULT 1,
    last_assessment_id BIGINT,
    version INT DEFAULT 1,
    checksum VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_world_type (world_type),
    INDEX idx_created_at (created_at)
);
```

**字段说明**:
- `session_id`: 会话唯一标识
- `user_id`: 用户ID，外键关联users表
- `title`: 会话标题
- `world_type`: 世界类型
- `world_state`: 世界状态JSON
- `skills_state`: 技能状态JSON
- `world_rules`: 世界规则JSON
- `god_mode_rules`: 上帝模式规则JSON
- `character_stats`: 角色属性JSON
- `active_quests`: 活跃任务JSON
- `completed_quests`: 已完成任务JSON
- `assessment_history`: 评估历史JSON
- `story_checkpoints`: 故事检查点JSON
- `stability_anchor`: 稳定性锚点JSON
- `convergence_progress`: 收敛进度
- `current_arc_name`: 当前情节名称
- `current_arc_start_round`: 当前情节起始轮数
- `total_rounds`: 总轮数
- `last_assessment_id`: 最后评估ID
- `version`: 版本号，用于乐观锁
- `checksum`: 数据校验和

#### chat_messages 表
```sql
CREATE TABLE chat_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(100) NOT NULL,
    role ENUM('USER', 'ASSISTANT') NOT NULL,
    content TEXT NOT NULL,
    sequence_number INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
    INDEX idx_session_id (session_id),
    INDEX idx_sequence_number (session_id, sequence_number),
    INDEX idx_created_at (created_at)
);
```

**字段说明**:
- `id`: 消息唯一标识
- `session_id`: 会话ID，外键关联chat_sessions表
- `role`: 消息角色（用户/助手）
- `content`: 消息内容
- `sequence_number`: 消息序号
- `created_at`: 创建时间

### 3. 评估系统表

#### dm_assessments 表
```sql
CREATE TABLE dm_assessments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(100) NOT NULL,
    user_action TEXT NOT NULL,
    ai_response TEXT NOT NULL,
    rule_compliance DOUBLE NOT NULL,
    context_consistency DOUBLE NOT NULL,
    convergence_progress DOUBLE NOT NULL,
    overall_score DOUBLE NOT NULL,
    strategy VARCHAR(20) NOT NULL,
    assessment_notes TEXT,
    suggested_actions TEXT,
    convergence_hints TEXT,
    dice_rolls JSON,
    learning_challenges JSON,
    state_updates JSON,
    quest_updates JSON,
    world_state_updates JSON,
    skills_state_updates JSON,
    arc_updates JSON,
    convergence_status_updates JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at),
    INDEX idx_overall_score (overall_score)
);
```

**字段说明**:
- `id`: 评估唯一标识
- `session_id`: 会话ID，外键关联chat_sessions表
- `user_action`: 用户行为
- `ai_response`: AI回复
- `rule_compliance`: 规则合规性评分
- `context_consistency`: 上下文一致性评分
- `convergence_progress`: 收敛推进度评分
- `overall_score`: 综合评分
- `strategy`: 处理策略
- `assessment_notes`: 评估说明
- `suggested_actions`: 建议行动JSON
- `convergence_hints`: 收敛提示JSON
- `dice_rolls`: 骰子检定JSON
- `learning_challenges`: 学习挑战JSON
- `state_updates`: 状态更新JSON
- `quest_updates`: 任务更新JSON
- `world_state_updates`: 世界状态更新JSON
- `skills_state_updates`: 技能状态更新JSON
- `arc_updates`: 情节更新JSON
- `convergence_status_updates`: 收敛状态更新JSON

### 4. 游戏机制表

#### dice_rolls 表
```sql
CREATE TABLE dice_rolls (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(100) NOT NULL,
    dice_type INT NOT NULL,
    modifier INT DEFAULT 0,
    result INT NOT NULL,
    final_result INT NOT NULL,
    context VARCHAR(200),
    difficulty_class INT,
    is_successful BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at),
    INDEX idx_dice_type (dice_type)
);
```

**字段说明**:
- `id`: 骰子记录唯一标识
- `session_id`: 会话ID，外键关联chat_sessions表
- `dice_type`: 骰子类型（如d20、d6等）
- `modifier`: 修正值
- `result`: 原始结果
- `final_result`: 最终结果（原始结果+修正值）
- `context`: 检定上下文
- `difficulty_class`: 难度等级
- `is_successful`: 是否成功

#### learning_challenges 表
```sql
CREATE TABLE learning_challenges (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(100) NOT NULL,
    challenge_type VARCHAR(50) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    user_answer TEXT,
    is_correct BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
    INDEX idx_session_id (session_id),
    INDEX idx_challenge_type (challenge_type),
    INDEX idx_created_at (created_at)
);
```

**字段说明**:
- `id`: 挑战唯一标识
- `session_id`: 会话ID，外键关联chat_sessions表
- `challenge_type`: 挑战类型（MATH、HISTORY、LANGUAGE等）
- `difficulty`: 难度等级
- `question`: 问题内容
- `answer`: 正确答案
- `user_answer`: 用户答案
- `is_correct`: 是否正确

### 5. 世界事件表

#### world_events 表
```sql
CREATE TABLE world_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_description TEXT NOT NULL,
    event_data JSON,
    current_arc_name VARCHAR(200),
    current_arc_start_round INT,
    total_rounds INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
    INDEX idx_session_id (session_id),
    INDEX idx_event_type (event_type),
    INDEX idx_created_at (created_at)
);
```

**字段说明**:
- `id`: 事件唯一标识
- `session_id`: 会话ID，外键关联chat_sessions表
- `event_type`: 事件类型
- `event_description`: 事件描述
- `event_data`: 事件数据JSON
- `current_arc_name`: 当前情节名称
- `current_arc_start_round`: 当前情节起始轮数
- `total_rounds`: 总轮数

### 6. 收敛状态表

#### convergence_status 表
```sql
CREATE TABLE convergence_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(100) UNIQUE NOT NULL,
    progress DOUBLE DEFAULT 0.0,
    nearest_scenario_id VARCHAR(100),
    nearest_scenario_title VARCHAR(200),
    distance_to_nearest DOUBLE,
    scenario_progress TEXT,
    active_hints TEXT,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
    INDEX idx_session_id (session_id),
    INDEX idx_progress (progress),
    INDEX idx_last_updated (last_updated)
);
```

**字段说明**:
- `id`: 收敛状态唯一标识
- `session_id`: 会话ID，外键关联chat_sessions表
- `progress`: 整体收敛进度 (0-1)
- `nearest_scenario_id`: 最近收敛场景ID
- `nearest_scenario_title`: 最近收敛场景标题
- `distance_to_nearest`: 到最近场景的距离
- `scenario_progress`: 各场景进度JSON
- `active_hints`: 活跃引导提示JSON
- `last_updated`: 最后更新时间
- `created_at`: 创建时间

### 7. 记忆系统表

#### memories 表
```sql
CREATE TABLE memories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    memory_type VARCHAR(50) NOT NULL,
    importance DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
    INDEX idx_session_id (session_id),
    INDEX idx_memory_type (memory_type),
    INDEX idx_importance (importance),
    INDEX idx_created_at (created_at)
);
```

**字段说明**:
- `id`: 记忆唯一标识
- `session_id`: 会话ID，外键关联chat_sessions表
- `content`: 记忆内容
- `memory_type`: 记忆类型（CHARACTER、EVENT、RELATIONSHIP等）
- `importance`: 重要性评分
- `created_at`: 创建时间

## 索引设计

### 主键索引
所有表都使用自增主键，提供唯一标识和快速查找。

### 外键索引
为所有外键字段创建索引，优化关联查询性能。

### 业务索引
根据常用查询模式创建复合索引：

```sql
-- 会话查询优化
CREATE INDEX idx_session_user_created ON chat_sessions(user_id, created_at);

-- 消息查询优化
CREATE INDEX idx_message_session_sequence ON chat_messages(session_id, sequence_number);

-- 评估查询优化
CREATE INDEX idx_assessment_session_score ON dm_assessments(session_id, overall_score);

-- 骰子查询优化
CREATE INDEX idx_dice_session_type ON dice_rolls(session_id, dice_type);

-- 事件查询优化
CREATE INDEX idx_event_session_type ON world_events(session_id, event_type);
```

## 数据完整性

### 外键约束
所有外键都设置了适当的约束：
- `ON DELETE CASCADE`: 删除父记录时自动删除子记录
- `ON UPDATE CASCADE`: 更新父记录时自动更新子记录

### 检查约束
为关键字段设置检查约束：

```sql
-- 评分范围检查
ALTER TABLE dm_assessments ADD CONSTRAINT chk_rule_compliance 
CHECK (rule_compliance >= 0 AND rule_compliance <= 1);

ALTER TABLE dm_assessments ADD CONSTRAINT chk_context_consistency 
CHECK (context_consistency >= 0 AND context_consistency <= 1);

-- 进度范围检查
ALTER TABLE convergence_status ADD CONSTRAINT chk_progress 
CHECK (progress >= 0 AND progress <= 1);

-- 重要性范围检查
ALTER TABLE memories ADD CONSTRAINT chk_importance 
CHECK (importance >= 0 AND importance <= 1);
```

### 唯一约束
为关键字段设置唯一约束：
- `users.username`: 用户名唯一
- `users.email`: 邮箱唯一
- `chat_sessions.session_id`: 会话ID唯一
- `convergence_status.session_id`: 收敛状态唯一

## 性能优化

### 分区策略
对于大数据量表，考虑按时间分区：

```sql
-- 按月份分区消息表
ALTER TABLE chat_messages PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
    PARTITION p202501 VALUES LESS THAN (202502),
    PARTITION p202502 VALUES LESS THAN (202503),
    PARTITION p202503 VALUES LESS THAN (202504),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### 查询优化
- 使用适当的索引
- 避免全表扫描
- 优化JOIN查询
- 使用EXPLAIN分析查询计划

### 连接池配置
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

## 数据备份

### 备份策略
- **全量备份**: 每日凌晨进行全量备份
- **增量备份**: 每小时进行增量备份
- **日志备份**: 实时备份二进制日志

### 备份命令
```bash
# 全量备份
mysqldump -u root -p --single-transaction --routines --triggers qncontest > backup_$(date +%Y%m%d).sql

# 增量备份
mysqlbinlog --start-datetime="2025-01-27 00:00:00" mysql-bin.000001 > incremental_backup.sql
```

## 监控和维护

### 性能监控
- 监控慢查询日志
- 监控连接数使用情况
- 监控磁盘空间使用
- 监控索引使用效率

### 维护任务
- 定期分析表统计信息
- 定期优化表结构
- 定期清理过期数据
- 定期检查数据完整性

### 监控查询
```sql
-- 查看慢查询
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';

-- 查看连接数
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Max_used_connections';

-- 查看表大小
SELECT 
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.tables
WHERE table_schema = 'qncontest'
ORDER BY (data_length + index_length) DESC;
```

## 扩展性考虑

### 水平扩展
- 支持读写分离
- 支持分库分表
- 支持分布式事务

### 垂直扩展
- 支持字段扩展
- 支持表结构变更
- 支持索引优化

### 数据迁移
- 提供数据迁移脚本
- 支持版本升级
- 支持数据回滚

## 安全考虑

### 数据加密
- 敏感数据加密存储
- 传输数据SSL加密
- 密码哈希存储

### 访问控制
- 数据库用户权限管理
- 应用程序权限控制
- 审计日志记录

### 数据脱敏
- 开发环境数据脱敏
- 测试数据匿名化
- 日志数据脱敏

---

**数据库设计遵循最佳实践，确保高性能、高可用性和数据安全性。**



# 数据库初始化指南

## 概述

本项目使用MySQL数据库，包含完整的角色扮演AI系统数据结构。数据库初始化脚本会自动创建所有必要的表和初始数据。

## 数据库结构

### 主要表结构

1. **用户系统表**
   - `users` - 用户信息
   - `refresh_tokens` - 刷新令牌

2. **聊天系统表**
   - `chat_sessions` - 聊天会话（包含角色扮演字段）
   - `chat_messages` - 聊天消息

3. **角色扮演系统表**
   - `world_templates` - 世界模板（包含故事收敛数据）
   - `world_states` - 世界状态历史
   - `stability_anchors` - 稳定性锚点
   - `world_events` - 世界事件记录（事件溯源）
   - `dice_rolls` - 骰子记录

## 世界模板和故事收敛系统

每个世界模板都包含：

### 收敛场景数据结构
```json
{
  "main_convergence": {
    "scenario_id": "unique_scenario_id",
    "title": "场景标题",
    "description": "场景描述",
    "trigger_conditions": ["条件1", "条件2"],
    "required_elements": ["元素1", "元素2"],
    "outcomes": ["结局1", "结局2"]
  },
  "alternative_convergence": {
    // 备选收敛场景
  }
}
```

### DM指令
- 每个世界都有专门的DM（Dungeon Master）行为指令
- 指导AI如何平衡故事发展和用户自由度

### 收敛规则
- `convergence_threshold`: 收敛阈值 (0-1)
- `max_exploration_turns`: 最大探索轮数
- `story_completeness_required`: 故事完整度要求

## 初始化步骤

### 方法1: 使用批处理脚本（推荐）

1. 确保MySQL服务正在运行
2. 双击运行 `init-db.bat`
3. 输入MySQL root密码
4. 确认初始化操作

### 方法2: 手动执行

1. 启动MySQL客户端
2. 执行以下命令：
```sql
source database-init.sql
```

## 默认数据

### 用户账户
- **管理员**: admin / admin123
- **测试用户**: panzijian1234 / 123456

### 可用世界模板
1. **fantasy_adventure** - 异世界探险
2. **western_magic** - 西方魔幻
3. **martial_arts** - 东方武侠
4. **japanese_school** - 日式校园
5. **educational** - 寓教于乐
6. **sci_fi** - 科幻探险
7. **detective** - 侦探推理

## 故事收敛机制

### 工作原理
1. 用户在世界中自由探索和互动
2. AI作为DM评估用户行为
3. 系统计算故事收敛进度
4. 当达到收敛条件时，引导故事向预设结局发展
5. 用户仍保留决策自由，但故事会自然收敛

### 评估标记
系统使用特殊标记 `^&*` 在AI响应流中分离：
- 用户可见内容
- 后端评估数据（JSON格式）

## 故障排除

### 常见问题

1. **MySQL连接失败**
   - 确保MySQL服务正在运行
   - 检查用户名和密码
   - 确认MySQL添加到系统PATH

2. **权限不足**
   - 使用root用户或有足够权限的用户
   - 检查数据库用户权限

3. **编码问题**
   - 数据库使用utf8mb4字符集
   - 确保客户端支持UTF-8

### 日志查看
- 应用启动时会显示数据库初始化状态
- 查看 `application.log` 获取详细错误信息

## 数据备份

在运行初始化脚本前，建议备份重要数据：

```sql
mysqldump -u root -p qn > backup_$(date +%Y%m%d_%H%M%S).sql
```

## 技术支持

如遇到问题，请查看：
1. 应用启动日志
2. MySQL错误日志
3. 数据库连接配置

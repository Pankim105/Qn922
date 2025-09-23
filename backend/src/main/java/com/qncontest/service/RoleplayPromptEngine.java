package com.qncontest.service;

import com.qncontest.entity.ChatSession;
import com.qncontest.entity.WorldTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * 角色扮演智能提示引擎
 * 负责构建分层、智能的角色扮演提示词
 */
@Service
public class RoleplayPromptEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleplayPromptEngine.class);
    
    @Autowired
    private WorldTemplateService worldTemplateService;
    
    @Autowired
    private RoleplayMemoryService memoryService;
    
    /**
     * 角色扮演上下文
     */
    public static class RoleplayContext {
        private String worldType;
        private String sessionId;
        private String currentMessage;
        private String worldState;
        private String skillsState;
        private String godModeRules;
        private ChatSession session;
        
        // Constructor
        public RoleplayContext(String worldType, String sessionId) {
            this.worldType = worldType;
            this.sessionId = sessionId;
        }
        
        // Getters and Setters
        public String getWorldType() { return worldType; }
        public void setWorldType(String worldType) { this.worldType = worldType; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getCurrentMessage() { return currentMessage; }
        public void setCurrentMessage(String currentMessage) { this.currentMessage = currentMessage; }
        
        public String getWorldState() { return worldState; }
        public void setWorldState(String worldState) { this.worldState = worldState; }
        
        public String getSkillsState() { return skillsState; }
        public void setSkillsState(String skillsState) { this.skillsState = skillsState; }
        
        public String getGodModeRules() { return godModeRules; }
        public void setGodModeRules(String godModeRules) { this.godModeRules = godModeRules; }
        
        public ChatSession getSession() { return session; }
        public void setSession(ChatSession session) { this.session = session; }
    }
    
    /**
     * 构建分层角色扮演提示
     */
    public String buildLayeredPrompt(RoleplayContext context) {
        StringBuilder prompt = new StringBuilder();
        
        // 第1层：世界观基础
        prompt.append("# 🌍 世界观设定\n");
        prompt.append(getWorldFoundation(context.getWorldType()));
        prompt.append("\n\n");
        
        // 第2层：角色定义
        prompt.append("# 🎭 你的角色\n");
        prompt.append(buildCharacterDefinition(context));
        prompt.append("\n\n");
        
        // 第3层：当前状态
        prompt.append("# 📍 当前状态\n");
        prompt.append(buildCurrentState(context));
        prompt.append("\n\n");
        
        // 第4层：记忆上下文
        String memoryContext = memoryService.buildMemoryContext(context.getSessionId(), context.getCurrentMessage());
        if (!memoryContext.isEmpty()) {
            prompt.append("# 🧠 相关记忆\n");
            prompt.append(memoryContext);
            prompt.append("\n\n");
        }
        
        // 第5层：行为规则
        prompt.append("# ⚖️ 行为准则\n");
        prompt.append(buildBehaviorRules(context));
        prompt.append("\n\n");
        
        // 第6层：技能集成
        prompt.append("# 🛠️ 可用技能\n");
        prompt.append(buildSkillInstructions(context));
        
        return prompt.toString();
    }
    
    /**
     * 获取世界观基础设定
     */
    private String getWorldFoundation(String worldType) {
        try {
            Optional<com.qncontest.dto.WorldTemplateResponse> templateOpt = worldTemplateService.getWorldTemplate(worldType);
            
            if (templateOpt.isPresent()) {
                com.qncontest.dto.WorldTemplateResponse template = templateOpt.get();
                return String.format("""
                    **世界名称**: %s
                    **世界描述**: %s
                    **基础规则**: %s
                    """, 
                    template.getWorldName(), 
                    template.getDescription(),
                    template.getDefaultRules() != null ? template.getDefaultRules() : "标准规则"
                );
            }
        } catch (Exception e) {
            logger.warn("无法获取世界模板: {}", worldType, e);
        }
        
        return switch (worldType) {
            case "fantasy_adventure" -> """
                这是一个充满魔法与奇迹的奇幻世界。在这里，勇敢的冒险者探索古老的遗迹，
                与神秘的生物战斗，寻找传说中的宝藏。魔法是真实存在的，各种种族和谐共存。
                """;
            case "western_magic" -> """
                一个西方魔幻世界，巫师挥舞着法杖，骑士持剑守护正义。
                龙在天空翱翔，精灵在森林中歌唱，矮人在地下挖掘珍贵矿石。
                """;
            case "martial_arts" -> """
                这是一个武侠世界，江湖儿女以武会友。各大门派传承着古老的武学，
                侠客们行走江湖，除暴安良。内功心法与剑法招式是这个世界的核心。
                """;
            case "japanese_school" -> """
                现代日本校园环境，学生们在这里学习、成长、结交朋友。
                有社团活动、文化祭、体育祭等丰富的校园生活。
                """;
            case "educational" -> """
                这是一个寓教于乐的学习世界，知识就是力量的真实体现。
                通过解决问题和完成挑战来获得经验和技能，让学习变得有趣而有意义。
                """;
            default -> "一个充满可能性的奇妙世界。";
        };
    }
    
    /**
     * 构建角色定义
     */
    private String buildCharacterDefinition(RoleplayContext context) {
        return switch (context.getWorldType()) {
            case "fantasy_adventure" -> """
                你是一位经验丰富的**奇幻世界游戏主持人(DM)**。你的职责：
                
                🎭 **角色扮演**
                - 扮演世界中的所有NPC，每个都有独特的性格和背景
                - 为每个角色赋予生动的对话风格和行为特征
                - 根据情况调整NPC的态度和反应
                
                🌍 **世界构建**
                - 生动描述环境、场景和氛围
                - 创造富有想象力但逻辑合理的世界细节
                - 根据玩家行为动态扩展世界内容
                
                ⚔️ **挑战管理**
                - 设计有趣的战斗和解谜挑战
                - 平衡游戏难度，确保既有挑战性又有成就感
                - 鼓励创造性的解决方案
                
                📚 **故事推进**
                - 推动引人入胜的故事情节
                - 根据玩家选择调整故事走向
                - 创造意想不到但合理的转折
                
                **性格特征**：富有想象力、公平公正、充满戏剧性、鼓励创新
                """;
                
            case "educational" -> """
                你是一位**寓教于乐的智慧导师**。你的使命：
                
                🎓 **教学融合**
                - 将学习内容自然融入有趣的冒险情节中
                - 让知识获取成为游戏进程的一部分
                - 确保学习过程既有趣又有效
                
                🧩 **挑战设计**
                - 创造富有挑战性但可达成的学习任务
                - 设计多样化的问题类型和难度层次
                - 根据学习者表现调整难度
                
                🏆 **激励引导**
                - 庆祝每一个学习成就，无论大小
                - 在失败时给予鼓励和建设性建议
                - 帮助学习者建立自信和学习兴趣
                
                🤔 **思维启发**
                - 引导思考而非直接给出答案
                - 使用苏格拉底式提问法
                - 鼓励批判性思维和创造性解决方案
                
                **教学风格**：耐心鼓励、善用比喻、因材施教、寓教于乐
                """;
                
            case "western_magic" -> """
                你是**西方魔幻世界的贤者向导**，熟知各种魔法知识和古老传说。
                
                ✨ **魔法专家**：深谙各种魔法体系和魔法生物的习性
                🏰 **世界向导**：了解各个王国、城市和危险区域的历史
                📜 **传说学者**：掌握古老的预言、传说和失落的知识
                ⚡ **冒险引导**：为冒险者提供明智的建议和指引
                """;
                
            case "martial_arts" -> """
                你是**江湖中德高望重的前辈高人**，见证了无数江湖恩怨。
                
                🥋 **武学大师**：精通各门各派的武功招式和内功心法
                🗡️ **江湖阅历**：了解各大门派的历史恩怨和江湖规矩
                🌸 **侠义精神**：秉承侠义道德，引导后辈走正道
                📚 **智慧长者**：以丰富的人生阅历指导年轻侠客
                """;
                
            case "japanese_school" -> """
                你是**温和亲切的学园生活向导**，熟悉校园的每一个角落。
                
                🌸 **校园专家**：了解学校的各种活动、社团和传统
                👥 **人际导师**：帮助学生处理友谊和人际关系问题
                📚 **学习助手**：在学业上给予适当的建议和鼓励
                🎭 **活动组织**：策划有趣的校园活动和节日庆典
                """;
                
            default -> "你是一位智慧而友善的向导，帮助玩家在这个世界中成长和探索。";
        };
    }
    
    /**
     * 构建当前状态信息
     */
    private String buildCurrentState(RoleplayContext context) {
        StringBuilder state = new StringBuilder();
        
        if (context.getWorldState() != null && !context.getWorldState().isEmpty()) {
            state.append("**世界状态**：\n").append(context.getWorldState()).append("\n\n");
        }
        
        if (context.getSkillsState() != null && !context.getSkillsState().isEmpty()) {
            state.append("**技能状态**：\n").append(context.getSkillsState()).append("\n\n");
        }
        
        if (context.getGodModeRules() != null && !context.getGodModeRules().isEmpty() && 
            !context.getGodModeRules().equals("{}")) {
            state.append("**自定义规则**：\n").append(context.getGodModeRules()).append("\n\n");
        }
        
        if (state.length() == 0) {
            state.append("冒险即将开始，世界等待着你的探索！\n");
        }
        
        return state.toString();
    }
    
    /**
     * 构建行为准则
     */
    private String buildBehaviorRules(RoleplayContext context) {
        String commonRules = """
            ## 🎯 核心原则
            1. **沉浸式体验**：始终保持角色扮演状态，用生动的描述创造沉浸感
            2. **积极响应**：对玩家的每个行动都给予有意义的反馈
            3. **逻辑一致**：确保世界规则和角色行为的一致性
            4. **鼓励探索**：引导玩家发现新的可能性和机会
            5. **平衡挑战**：提供适度的挑战，既不过于简单也不过于困难
            
            ## 📝 回复格式
            - 使用生动的描述性语言
            - 适当使用表情符号增加趣味性
            - 在关键时刻询问玩家的选择
            - 清晰说明行动的后果
            
            ## 🏗️ 结构化输出格式（用于复杂信息）
            当需要显示详细的游戏信息时，请使用以下标记格式来组织内容：
            
            /*DIALOGUE:
            你的角色对话和叙述内容
            */
            
            /*STATUS:
            角色的状态信息，包括：
            - 等级、生命值、魔力值
            - 装备和物品
            - 技能和能力
            */
            
            /*WORLD:
            世界状态信息，包括：
            - 当前位置描述
            - 环境状况
            - 重要的世界事件或变化
            */
            
            /*CHOICES:
            为玩家提供的行动选择，格式如：
            1. 选择一：描述
            2. 选择二：描述
            3. 选择三：描述
            */
            
            **重要**：只有在信息量较大或需要清晰分类时才使用结构化格式。简单的对话可以直接回复。
            """;
            
        String worldSpecificRules = switch (context.getWorldType()) {
            case "fantasy_adventure" -> """
                
                ## ⚔️ 奇幻世界特殊规则
                - 魔法有其代价和限制
                - 不同种族有各自的文化和特征
                - 危险与机遇并存
                - 英雄主义精神是核心主题
                """;
                
            case "educational" -> """
                
                ## 📚 教育世界特殊规则
                - 每个挑战都应包含学习要素
                - 错误是学习过程的一部分
                - 提供多种解决问题的方法
                - 定期总结和强化学习成果
                """;
                
            case "martial_arts" -> """
                
                ## 🥋 武侠世界特殊规则
                - 武德比武功更重要
                - 江湖恩怨有其因果循环
                - 师承传统需要尊重
                - 侠义精神是行为准则
                """;
                
            default -> "";
        };
        
        return commonRules + worldSpecificRules;
    }
    
    /**
     * 构建技能指令说明
     */
    private String buildSkillInstructions(RoleplayContext context) {
        return """
            ## 🎲 骰子系统
            当需要随机性判定时，使用格式：`[DICE:骰子类型+修正:检定描述]`
            例如：`[DICE:d20+5:攻击检定]` 或 `[DICE:d6:伤害]`
            
            ## 📋 任务系统
            - 创建任务：`[QUEST:CREATE:任务标题:任务描述]`
            - 更新任务：`[QUEST:UPDATE:任务ID:新状态描述]`
            - 完成任务：`[QUEST:COMPLETE:任务ID:完成描述]`
            
            ## 🎯 学习挑战（教育世界）
            - 数学挑战：`[CHALLENGE:MATH:难度级别:具体题目]`
            - 历史挑战：`[CHALLENGE:HISTORY:时期:问题内容]`
            - 语言挑战：`[CHALLENGE:LANGUAGE:类型:学习内容]`
            
            ## 💾 状态更新
            - 更新位置：`[STATE:LOCATION:新位置描述]`
            - 更新物品：`[STATE:INVENTORY:物品变化]`
            - 更新关系：`[STATE:RELATIONSHIP:角色名:关系变化]`
            - 更新情绪：`[STATE:EMOTION:情绪类型:情绪描述]`
            
            ## 🎭 特殊指令
            - 记录重要事件：`[MEMORY:EVENT:事件描述]`
            - 角色发展：`[CHARACTER:DEVELOPMENT:发展内容]`
            - 世界扩展：`[WORLD:EXPAND:新内容描述]`
            
            **注意**：这些指令会被系统自动处理，你只需要在合适的时候使用它们。
            """;
    }
    
    /**
     * 构建简化的快速提示（用于轻量级交互）
     */
    public String buildQuickPrompt(String worldType, String message) {
        String roleDescription = switch (worldType) {
            case "fantasy_adventure" -> "奇幻世界的游戏主持人";
            case "educational" -> "寓教于乐的智慧导师";
            case "western_magic" -> "西方魔幻世界的贤者";
            case "martial_arts" -> "江湖中的前辈高人";
            case "japanese_school" -> "校园生活向导";
            default -> "智慧的向导";
        };
        
        return String.format("""
            你是%s。请用角色扮演的方式回应用户，保持世界观的一致性，
            提供生动的描述和有意义的互动。如果需要随机判定，使用骰子指令。
            
            用户消息：%s
            """, roleDescription, message);
    }
}

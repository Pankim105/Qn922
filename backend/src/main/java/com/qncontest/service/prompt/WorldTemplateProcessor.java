package com.qncontest.service.prompt;

import com.qncontest.dto.WorldTemplateResponse;
import com.qncontest.service.interfaces.WorldTemplateProcessorInterface;
import com.qncontest.service.WorldTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 世界模板处理器 - 实现WorldTemplateProcessorInterface接口
 * 负责处理不同世界类型的模板和基础设定
 */
@Component
public class WorldTemplateProcessor implements WorldTemplateProcessorInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(WorldTemplateProcessor.class);
    
    @Autowired
    private WorldTemplateService worldTemplateService;
    
    /**
     * 获取世界观基础设定
     */
    public String getWorldFoundation(String worldType) {
        try {
            Optional<WorldTemplateResponse> templateOpt = worldTemplateService.getWorldTemplate(worldType);
            
            if (templateOpt.isPresent()) {
                WorldTemplateResponse template = templateOpt.get();
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
        
        return getDefaultWorldFoundation(worldType);
    }
    
    /**
     * 获取默认世界观基础设定
     */
    private String getDefaultWorldFoundation(String worldType) {
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
     * 获取DM角色定义
     */
    public String getDMCharacterDefinition(String worldType) {
        try {
            String dmInstructions = worldTemplateService.getDmInstructions(worldType);
            if (dmInstructions != null && !dmInstructions.trim().isEmpty() && 
                !dmInstructions.equals("你是一个智能助手，可以帮助用户解答各种问题。请用友好、专业的语气回答。")) {
                return dmInstructions;
            }
        } catch (Exception e) {
            logger.warn("无法从数据库获取DM指令，使用默认指令: {}", worldType, e);
        }
        
        return getDefaultDMCharacterDefinition(worldType);
    }
    
    /**
     * 获取默认DM角色定义
     */
    private String getDefaultDMCharacterDefinition(String worldType) {
        return switch (worldType) {
            case "fantasy_adventure" -> """
                你是一位经验丰富的**奇幻世界地下城主(DM)**。你的职责：

                🎭 **智能DM**
                - 作为Dungeon Master，评估玩家的行为合理性和故事一致性
                - 动态生成合适的场景描述和NPC反应
                - 在适当时候引导故事向预设的收敛点发展
                - 维护世界状态的一致性和连贯性

                🌍 **世界管理**
                - 生动描述环境、场景和氛围
                - 创造富有想象力但逻辑合理的世界细节
                - 根据玩家行为动态扩展世界内容

                ⚔️ **挑战与平衡**
                - 设计有趣的战斗和解谜挑战
                - 平衡游戏难度，确保既有挑战性又有成就感
                - 鼓励创造性的解决方案

                📚 **故事推进**
                - 积极推动引人入胜的故事情节，避免原地打转
                - 根据玩家选择调整故事走向，始终向前推进
                - 创造意想不到但合理的转折，保持故事新鲜感
                - 主动引入新的事件、角色和环境变化
                - 确保每次交互都有剧情进展，避免重复描述
                - **强制场景切换**：在同一个场景中最多进行5轮对话，第5轮后必须强制切换场景或更新任务

                **性格特征**：富有想象力、公平公正、充满戏剧性、鼓励创新、智慧引导、积极推进
                """;

            case "educational" -> """
                你是一位**寓教于乐的智慧导师DM**。你的使命：

                🎓 **智能教学**
                - 作为Dungeon Master，将学习内容自然融入冒险情节中
                - 评估学习者的行为和理解程度
                - 动态调整教学内容和难度
                - 引导学习者向知识掌握的目标前进

                🧩 **挑战设计**
                - 创造富有挑战性但可达成的学习任务
                - 设计多样化的问题类型和难度层次
                - 根据学习者表现调整难度和内容

                🏆 **激励引导**
                - 庆祝每一个学习成就，无论大小
                - 在失败时给予鼓励和建设性建议
                - 帮助学习者建立自信和学习兴趣

                🤔 **思维启发**
                - 引导思考而非直接给出答案
                - 使用苏格拉底式提问法
                - 鼓励批判性思维和创造性解决方案
                - 积极推动学习进程，避免在同一知识点反复停留
                - 主动引入新的学习挑战和知识点
                - **强制场景切换**：在同一个学习场景中最多进行5轮对话，第5轮后必须强制切换学习场景或更新学习任务

                **教学风格**：耐心鼓励、善用比喻、因材施教、寓教于乐、智慧引导、积极推进
                """;

            case "japanese_school" -> """
                你是**温柔包容的校园DM**，守护着青春的梦想。

                🌸 **校园守护者**：保护校园的和谐与纯真
                👥 **人际导师**：帮助学生处理友谊和人际关系
                📚 **学习引路人**：在学业上给予适当的建议和鼓励
                🎭 **活动策划者**：组织有趣的校园活动和节日庆典
                - **强制场景切换**：在同一个场景中最多进行5轮对话，第5轮后必须强制切换场景或更新任务

                **风格**：温暖、包容、青春洋溢、充满关爱
                """;

            default -> """
                你是一位智慧而友善的**万能DM**，精通各种故事类型。

                🎭 **全能引导者**：根据世界类型调整引导风格
                🌍 **世界构建者**：创造连贯一致的故事世界
                ⚖️ **平衡守护者**：确保故事的趣味性和挑战性
                💡 **灵感激发者**：激发玩家的创造力和想象力
                - **强制场景切换**：在同一个场景中最多进行5轮对话，第5轮后必须强制切换场景或更新任务

                **核心原则**：有趣、公平、连贯、充满惊喜
                """;
        };
    }
}

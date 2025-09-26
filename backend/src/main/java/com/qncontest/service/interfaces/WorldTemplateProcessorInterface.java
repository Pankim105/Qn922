package com.qncontest.service.interfaces;

/**
 * 世界模板处理器接口
 * 定义世界模板处理的标准行为
 */
public interface WorldTemplateProcessorInterface {
    
    /**
     * 获取世界观基础设定
     * @param worldType 世界类型
     * @return 世界观基础设定
     */
    String getWorldFoundation(String worldType);
    
    /**
     * 获取DM角色定义
     * @param worldType 世界类型
     * @return DM角色定义
     */
    String getDMCharacterDefinition(String worldType);
}

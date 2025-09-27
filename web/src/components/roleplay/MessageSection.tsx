import React from 'react';
import { Button } from 'modern-ui-components';
import type { MessageSectionProps, MessageSection as MessageSectionType } from './types';
import { sectionIcons, getCardConfig } from './constants';
import { isContentEmpty } from './messageParser';
import {
  formatQuestTextContent,
  formatQuestContent,
  formatStatusContent,
  formatChoicesContent,
  formatAssessmentTextContent,
  formatAssessmentContent,
  formatDialogueContent
} from './messageFormatter';

const MessageSection: React.FC<MessageSectionProps> = ({ 
  section, 
  index, 
  isDark, 
  isLoading, 
  onChoiceSelect, 
  onFreeActionModeChange 
}) => {
  // 检查内容是否为空，如果为空则不渲染
  if (isContentEmpty(section.content)) {
    return null;
  }

  const getSectionIcon = (type: MessageSectionType['type']) => {
    return sectionIcons[type] || sectionIcons.plain;
  };

  const getSectionStyle = (type: MessageSectionType['type']) => {
    const baseStyle = "rounded-lg p-4 border-2 shadow-lg hover:shadow-xl transition-all duration-300 backdrop-blur-sm";

    if (isDark) {
      switch (type) {
        case 'status':
          return `${baseStyle} bg-gradient-to-br from-blue-900/60 to-blue-800/40 border-blue-400/60 text-blue-100 shadow-blue-500/20`;
        case 'world':
          return `${baseStyle} bg-gradient-to-br from-green-900/60 to-green-800/40 border-green-400/60 text-green-100 shadow-green-500/20`;
        case 'choices':
          return `${baseStyle} bg-gradient-to-br from-purple-900/60 to-purple-800/40 border-purple-400/60 text-purple-100 shadow-purple-500/20`;
        case 'dialogue':
          return `${baseStyle} bg-transparent border-slate-400/70 text-slate-100 shadow-slate-500/30 backdrop-blur-md`;
        case 'quests':
          return `${baseStyle} bg-gradient-to-br from-indigo-900/60 to-indigo-800/40 border-indigo-400/60 text-indigo-100 shadow-indigo-500/20`;
        case 'assessment':
          return `${baseStyle} bg-gradient-to-br from-gray-800/70 to-gray-700/50 border-gray-400/60 text-gray-100 shadow-gray-500/20`;
        default:
          return `${baseStyle} bg-gradient-to-br from-gray-800/70 to-gray-700/50 border-gray-400/60 text-gray-100 shadow-gray-500/20`;
      }
    } else {
      switch (type) {
        case 'status':
          return `${baseStyle} bg-gradient-to-br from-blue-100/90 to-blue-50/70 border-blue-500/70 text-blue-900 shadow-blue-500/30`;
        case 'world':
          return `${baseStyle} bg-gradient-to-br from-green-100/90 to-green-50/70 border-green-500/70 text-green-900 shadow-green-500/30`;
        case 'choices':
          return `${baseStyle} bg-gradient-to-br from-purple-100/90 to-purple-50/70 border-purple-500/70 text-purple-900 shadow-purple-500/30`;
        case 'dialogue':
          return `${baseStyle} bg-transparent border-slate-500/80 text-slate-900 shadow-slate-500/40 backdrop-blur-sm`;
        case 'quests':
          return `${baseStyle} bg-gradient-to-br from-indigo-100/90 to-indigo-50/70 border-indigo-500/70 text-indigo-900 shadow-indigo-500/30`;
        case 'assessment':
          return `${baseStyle} bg-gradient-to-br from-gray-200/90 to-gray-100/70 border-gray-500/70 text-gray-900 shadow-gray-500/30`;
        default:
          return `${baseStyle} bg-gradient-to-br from-gray-200/90 to-gray-100/70 border-gray-500/70 text-gray-900 shadow-gray-500/30`;
      }
    }
  };

  // 缓存解析结果，避免重复计算
  const parseSectionContent = (section: MessageSectionType) => {
    let result: any = null;

    // 对话信息解析
    if (section.type === 'dialogue') {
      result = {
        type: 'markdown',
        formatted: formatDialogueContent(section.content)
      };
    }

    // 任务信息解析
    else if (section.type === 'quests') {
      const isJsonFormat = section.content.trim().startsWith('{') && section.content.trim().endsWith('}');

      if (isJsonFormat) {
        try {
          const questData = JSON.parse(section.content);
          result = {
            type: 'json',
            data: questData,
            formatted: formatQuestContent(questData)
          };
        } catch (e) {
          result = {
            type: 'text',
            formatted: formatQuestTextContent(section.content)
          };
        }
      } else {
        result = {
          type: 'text',
          formatted: formatQuestTextContent(section.content)
        };
      }
    }

    // 评估信息解析
    else if (section.type === 'assessment') {
      const isJsonFormat = section.content.trim().startsWith('{') && section.content.trim().endsWith('}');

      if (isJsonFormat) {
        try {
          const assessmentData = JSON.parse(section.content);
          result = {
            type: 'json',
            data: assessmentData,
            formatted: formatAssessmentContent(assessmentData)
          };
        } catch (e) {
          console.error('DEBUG: 评估信息JSON解析失败', e, section.content);
          result = {
            type: 'text',
            formatted: formatAssessmentTextContent(section.content)
          };
        }
      } else {
        result = {
          type: 'text',
          formatted: formatAssessmentTextContent(section.content)
        };
      }
    }

    // 状态和世界信息解析
    else if (section.type === 'status' || section.type === 'world') {
      result = {
        type: 'markdown',
        formatted: formatStatusContent(section.content)
      };
    }

    // 选择信息解析
    else if (section.type === 'choices') {
      result = {
        type: 'markdown',
        formatted: formatChoicesContent(section.content)
      };
    }

    return result;
  };

  if (section.type === 'plain') {
    return (
      <div key={index} className="col-span-full whitespace-pre-wrap text-sm leading-relaxed p-3 bg-gradient-to-r from-gray-100/80 to-gray-50/60 dark:from-gray-800/60 dark:to-gray-700/40 rounded-lg border border-gray-300/50 dark:border-gray-600/50 shadow-md">
        {section.content}
      </div>
    );
  }

  // 特殊处理任务信息 - 游戏UI风格
  if (section.type === 'quests') {
    const parsedContent = parseSectionContent(section);
    
    // 检查任务内容是否为空
    let hasValidContent = false;
    if (parsedContent?.formatted && 
        !parsedContent.formatted.includes('暂无活跃任务') && 
        !parsedContent.formatted.includes('任务信息正在加载中')) {
      hasValidContent = true;
    } else if (section.content && !isContentEmpty(section.content)) {
      // 检查原始内容是否包含有效的任务信息
      const trimmedContent = section.content.trim();
      if (trimmedContent && 
          !trimmedContent.includes('暂无活跃任务') && 
          !trimmedContent.includes('任务信息正在加载中') &&
          !trimmedContent.includes('[无活跃任务]') &&
          trimmedContent.length > 5) { // 至少要有一定长度
        hasValidContent = true;
      }
    }
    
    // 如果没有有效内容，不渲染任务卡片
    if (!hasValidContent) {
      return null;
    }
    
    return (
      <div key={index} className={`${getCardConfig(section.type).cols} ${getSectionStyle(section.type)}`}>
        <div className="flex items-center gap-2 font-bold text-sm mb-3 opacity-95">
          {section.icon || getSectionIcon(section.type)}
          <span className="text-shadow-sm">{section.title}</span>
        </div>
        <div
          className="text-sm leading-relaxed"
          dangerouslySetInnerHTML={{ __html: parsedContent?.formatted || section.content }}
        />
      </div>
    );
  }

  // 特殊处理评估信息 - 游戏UI风格
  if (section.type === 'assessment') {
    const parsedContent = parseSectionContent(section);
    
    // 检查评估内容是否为空
    let hasValidContent = false;
    if (parsedContent?.formatted && parsedContent.formatted !== '暂无评估信息') {
      hasValidContent = true;
    } else if (section.content && !isContentEmpty(section.content)) {
      // 检查原始内容是否包含有效的评估信息
      const trimmedContent = section.content.trim();
      if (trimmedContent && 
          !trimmedContent.includes('暂无评估信息') && 
          !trimmedContent.includes('评估信息正在加载中') &&
          trimmedContent.length > 10) { // 至少要有一定长度
        hasValidContent = true;
      }
    }
    
    // 如果没有有效内容，不渲染评估卡片
    if (!hasValidContent) {
      return null;
    }
    
    return (
      <div key={index} className={`${getCardConfig(section.type).cols} ${getSectionStyle(section.type)}`}>
        <div className="flex items-center gap-2 font-bold text-sm mb-3 opacity-95">
          {section.icon || getSectionIcon(section.type)}
          <span className="text-shadow-sm">{section.title}</span>
        </div>
        <div
          className="text-sm leading-relaxed"
          dangerouslySetInnerHTML={{ __html: parsedContent?.formatted || section.content }}
        />
      </div>
    );
  }

  // 特殊处理行动选择 - 游戏UI风格
  if (section.type === 'choices') {
    // 使用新的格式化函数处理选择内容
    const parsedContent = parseSectionContent(section);
    
    // 检查选择内容是否为空
    let hasValidContent = false;
    if (parsedContent?.formatted && 
        !parsedContent.formatted.includes('暂无可选行动') && 
        !parsedContent.formatted.includes('选择信息正在加载中')) {
      hasValidContent = true;
    } else if (section.content && !isContentEmpty(section.content)) {
      // 检查原始内容是否包含有效的选择信息
      const trimmedContent = section.content.trim();
      if (trimmedContent && 
          !trimmedContent.includes('暂无可选行动') && 
          !trimmedContent.includes('选择信息正在加载中') &&
          trimmedContent.length > 5) { // 至少要有一定长度
        hasValidContent = true;
      }
    }
    
    // 如果没有有效内容，不渲染选择卡片
    if (!hasValidContent) {
      return null;
    }

    // 尝试解析选择项 - 支持分号分隔格式
    let choices = [];
    
    // 首先尝试按分号分割
    if (section.content.includes(';')) {
      const choiceItems = section.content.split(';').map(item => item.trim()).filter(item => item);
      choices = choiceItems.map(item => {
        // 匹配 数字. **标题** - 描述 格式
        const match = item.match(/^(\d+)\.\s*\*\*(.*?)\*\*\s*-\s*(.+)$/);
        if (match) {
          return `**${match[2]}** - ${match[3]}`;
        }
        // 匹配 **标题** - 描述 格式（没有序号）
        const noNumberMatch = item.match(/^\*\*(.*?)\*\*\s*-\s*(.+)$/);
        if (noNumberMatch) {
          return `**${noNumberMatch[1]}** - ${noNumberMatch[2]}`;
        }
        return item;
      });
    } else {
      // 如果没有分号，尝试按行分割
      const choiceLines = section.content.split('\n').filter(line => line.trim());
      choices = choiceLines.map(line => {
        const match = line.match(/^\d+\.\s*(.+)/);
        return match ? match[1] : line;
      }).filter(choice => choice.trim());
    }
    
    // 如果没有有效的选择项，尝试使用格式化内容
    if (choices.length === 0) {
      return (
        <div key={index} className={`${getCardConfig(section.type).cols} ${getSectionStyle(section.type)}`}>
          <div className="flex items-center gap-2 font-bold text-sm mb-3 opacity-95">
            {section.icon || getSectionIcon(section.type)}
            <span className="text-shadow-sm">{section.title}</span>
          </div>
          <div
            className="text-sm leading-relaxed"
            dangerouslySetInnerHTML={{ __html: parsedContent?.formatted || section.content }}
          />
        </div>
      );
    }

    return (
      <div key={index} className={`${getCardConfig(section.type).cols} ${getSectionStyle(section.type)}`}>
        <div className="flex items-center gap-2 font-bold text-sm mb-3 opacity-95">
          {section.icon || getSectionIcon(section.type)}
          <span className="text-shadow-sm">{section.title}</span>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {choices.map((choice, choiceIndex) => {
            // 解析选择项格式：**标题** - 描述
            const choiceMatch = choice.match(/^\*\*(.*?)\*\*\s*-\s*(.*)$/);
            const choiceTitle = choiceMatch ? choiceMatch[1] : choice;
            const choiceDesc = choiceMatch ? choiceMatch[2] : '';
            
            // 检查是否为自由行动选项
            const isFreeAction = choiceTitle.includes('自由行动') || choiceTitle.toLowerCase().includes('free action') || choiceTitle.includes('其他行动');
            
            return (
              <Button
                key={choiceIndex}
                variant="outline"
                size="sm"
                onClick={() => {
                  if (!isLoading) {
                    if (isFreeAction) {
                      // 自由行动：清空输入框并聚焦，提示用户输入
                      onFreeActionModeChange(true);
                    } else {
                      // 普通选择：直接发送
                      onChoiceSelect(choiceTitle);
                    }
                  }
                }}
                disabled={isLoading}
                className={`justify-start text-left h-auto p-3 whitespace-normal text-sm font-medium border-2 shadow-md hover:shadow-lg transition-all duration-200 ${
                  isFreeAction
                    ? isDark
                      ? 'hover:bg-orange-800/60 border-orange-400/70 text-orange-100 bg-orange-900/30'
                      : 'hover:bg-orange-100/80 border-orange-500/70 text-orange-900 bg-orange-50/50'
                    : isDark
                      ? 'hover:bg-purple-800/60 border-purple-400/70 text-purple-100 bg-purple-900/30'
                      : 'hover:bg-purple-100/80 border-purple-500/70 text-purple-900 bg-purple-50/50'
                }`}
              >
                <div className="flex flex-col gap-1">
                  <span className="font-bold text-sm">
                    {isFreeAction && <span className="mr-1">✏️</span>}
                    {choiceTitle}
                  </span>
                  {choiceDesc && (
                    <span className={`text-xs opacity-80 ${
                      isFreeAction
                        ? isDark ? 'text-orange-200' : 'text-orange-700'
                        : isDark ? 'text-purple-200' : 'text-purple-700'
                    }`}>
                      {choiceDesc}
                    </span>
                  )}
                </div>
              </Button>
            );
          })}
        </div>
      </div>
    );
  }

  // 特殊处理对话内容 - 使用格式化函数渲染不同的对话元素
  if (section.type === 'dialogue') {
    const parsedContent = parseSectionContent(section);

    // 检查对话内容是否为空
    let hasValidContent = false;
    if (parsedContent?.formatted && parsedContent.formatted.trim()) {
      hasValidContent = true;
    } else if (section.content && !isContentEmpty(section.content)) {
      const trimmedContent = section.content.trim();
      if (trimmedContent && trimmedContent.length > 5) {
        hasValidContent = true;
      }
    }

    // 如果没有有效内容，不渲染对话卡片
    if (!hasValidContent) {
      return null;
    }

    return (
      <div key={index} className={`${getCardConfig(section.type).cols} ${getSectionStyle(section.type)}`}>
        <div className="flex items-center gap-2 font-bold text-sm mb-3 opacity-95">
          {section.icon || getSectionIcon(section.type)}
          <span className="text-shadow-sm">{section.title}</span>
        </div>
        <div
          className="bg-transparent"
          dangerouslySetInnerHTML={{ __html: parsedContent?.formatted || formatDialogueContent(section.content) }}
        />
      </div>
    );
  }

  // 特殊处理markdown格式的状态和世界信息
  if (section.type === 'status' || section.type === 'world') {
    const parsedContent = parseSectionContent(section);

    // 检查状态和世界信息是否为空
    let hasValidContent = false;
    if (parsedContent?.formatted && parsedContent.formatted.trim()) {
      hasValidContent = true;
    } else if (section.content && !isContentEmpty(section.content)) {
      const trimmedContent = section.content.trim();
      if (trimmedContent && trimmedContent.length > 5) {
        hasValidContent = true;
      }
    }

    // 如果没有有效内容，不渲染状态/世界卡片
    if (!hasValidContent) {
      return null;
    }

    return (
      <div key={index} className={`${getCardConfig(section.type).cols} ${getSectionStyle(section.type)}`}>
        <div className="flex items-center gap-2 font-bold text-sm mb-3 opacity-95">
          {section.icon || getSectionIcon(section.type)}
          <span className="text-shadow-sm">{section.title}</span>
        </div>
        <div
          className="text-sm leading-relaxed"
          dangerouslySetInnerHTML={{ __html: parsedContent?.formatted || section.content }}
        />
      </div>
    );
  }

  // 默认处理其他类型的内容
  // 检查内容是否为空
  if (isContentEmpty(section.content)) {
    return null;
  }
  
  return (
    <div key={index} className={`${getCardConfig(section.type).cols} ${getSectionStyle(section.type)}`}>
      <div className="flex items-center gap-2 font-bold text-sm mb-3 opacity-95">
        {section.icon || getSectionIcon(section.type)}
        <span className="text-shadow-sm">{section.title}</span>
      </div>
      <div className="whitespace-pre-wrap text-sm leading-relaxed">
        {section.content}
      </div>
    </div>
  );
};

export default MessageSection;

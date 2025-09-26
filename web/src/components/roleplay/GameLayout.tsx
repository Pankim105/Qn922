import React, { memo } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from 'modern-ui-components';
import { 
  ScrollText, 
  MessageCircle, 
  BookOpen, 
  ArrowRight, 
  Globe, 
  User
} from 'lucide-react';
import type { GameLayoutProps } from './types';
import { formatQuestContent, formatQuestTextContent, formatAssessmentContent } from './messageFormatter';
import { ASSESSMENT_PATTERNS } from './constants';


const GameLayout: React.FC<GameLayoutProps> = memo(({
  isDark,
  isLoading,
  isAssessing = false,
  onChoiceSelect,
  onFreeActionModeChange,
  skillsState,
  structuredContent
}) => {
  // 解析消息内容
  const parseMessageContent = () => {
    // 优先使用传入的结构化内容依赖项
    if (structuredContent) {
      return {
        dialogue: structuredContent.dialogue || '',
        status: structuredContent.status || '',
        world: structuredContent.world || '',
        quests: structuredContent.quests || '',
        choices: structuredContent.choices || '',
        assessment: structuredContent.assessment || ''
      };
    }

    // 如果没有结构化内容，返回空内容
    return {
      dialogue: '',
      status: '',
      world: '',
      quests: '',
      choices: '',
      assessment: ''
    };
  };

  const content = parseMessageContent();
  

  // 解析选择项
  const parseChoices = () => {
    if (!content.choices) return [];
    
    const choices = [];
    if (content.choices.includes(';')) {
      const choiceItems = content.choices.split(';').map(item => item.trim()).filter(item => item);
      choices.push(...choiceItems.map(item => {
        const match = item.match(/^(\d+)\.\s*\*\*(.*?)\*\*\s*-\s*(.+)$/);
        if (match) {
          return { title: match[2], description: match[3] };
        }
        const noNumberMatch = item.match(/^\*\*(.*?)\*\*\s*-\s*(.+)$/);
        if (noNumberMatch) {
          return { title: noNumberMatch[1], description: noNumberMatch[2] };
        }
        return { title: item, description: '' };
      }));
    } else {
      const choiceLines = content.choices.split('\n').filter(line => line.trim());
      choices.push(...choiceLines.map(line => {
        const match = line.match(/^\d+\.\s*(.+)/);
        return { title: match ? match[1] : line, description: '' };
      }));
    }
    
    return choices;
  };

  const choices = parseChoices();

  // 格式化角色状态
  const formatCharacterStatus = () => {
    // 优先使用解析出的status内容
    if (content.status) {
      return content.status;
    }
    
    if (!skillsState) return '暂无角色状态';
    
    const { level, experience, gold, inventory, stats, abilities } = skillsState;
    
    return (
      <div className="h-full flex flex-col space-y-3">
        {/* 基础信息 */}
        <div className="bg-gradient-to-r from-blue-50 to-blue-100 dark:from-blue-900/30 dark:to-blue-800/30 rounded-lg p-3 border border-blue-200 dark:border-blue-700">
          <div className="flex justify-between items-center mb-2">
            <span className="font-semibold text-blue-600 dark:text-blue-400">等级:</span>
            <span className="font-bold text-xl bg-blue-500 text-white px-2 py-1 rounded-full">{level || 1}</span>
          </div>
          
          <div className="flex justify-between items-center mb-2">
            <span className="font-semibold text-green-600 dark:text-green-400">经验值:</span>
            <span className="font-mono bg-green-100 dark:bg-green-900/50 px-2 py-1 rounded">{experience || 0}</span>
          </div>
          
          <div className="flex justify-between items-center">
            <span className="font-semibold text-yellow-600 dark:text-yellow-400">金币:</span>
            <span className="font-mono bg-yellow-100 dark:bg-yellow-900/50 px-2 py-1 rounded">💰 {gold || 0}</span>
          </div>
        </div>
        
        {/* 物品 */}
        <div className="bg-gradient-to-r from-purple-50 to-purple-100 dark:from-purple-900/30 dark:to-purple-800/30 rounded-lg p-3 border border-purple-200 dark:border-purple-700 flex-1">
          <span className="font-semibold text-purple-600 dark:text-purple-400 block mb-2">物品:</span>
          <div className="text-sm">
            {inventory && inventory.length > 0 ? (
              <div className="space-y-1">
                {inventory.map((item: any, index: number) => (
                  <div key={index} className="bg-white dark:bg-gray-800 px-2 py-1 rounded border border-purple-200 dark:border-purple-600 text-gray-700 dark:text-gray-300">
                    📦 {item}
                  </div>
                ))}
              </div>
            ) : (
              <span className="text-gray-500 dark:text-gray-400 italic">暂无物品</span>
            )}
          </div>
        </div>
        
        {/* 技能 */}
        <div className="bg-gradient-to-r from-orange-50 to-orange-100 dark:from-orange-900/30 dark:to-orange-800/30 rounded-lg p-3 border border-orange-200 dark:border-orange-700 flex-1">
          <span className="font-semibold text-orange-600 dark:text-orange-400 block mb-2">技能:</span>
          <div className="text-sm">
            {abilities && abilities.length > 0 ? (
              <div className="space-y-1">
                {abilities.map((ability: any, index: number) => (
                  <div key={index} className="bg-white dark:bg-gray-800 px-2 py-1 rounded border border-orange-200 dark:border-orange-600 text-gray-700 dark:text-gray-300">
                    ⚡ {ability}
                  </div>
                ))}
              </div>
            ) : (
              <span className="text-gray-500 dark:text-gray-400 italic">暂无技能</span>
            )}
          </div>
        </div>
        
        {/* 属性 */}
        <div className="bg-gradient-to-r from-red-50 to-red-100 dark:from-red-900/30 dark:to-red-800/30 rounded-lg p-3 border border-red-200 dark:border-red-700 flex-1">
          <span className="font-semibold text-red-600 dark:text-red-400 block mb-2">属性:</span>
          <div className="text-sm">
            {stats ? (
              <div className="grid grid-cols-2 gap-2">
                {Object.entries(stats).map(([key, value]) => (
                  <div key={key} className="bg-white dark:bg-gray-800 px-2 py-1 rounded border border-red-200 dark:border-red-600 flex justify-between items-center">
                    <span className="text-gray-600 dark:text-gray-400">{key}:</span>
                    <span className="font-mono font-semibold text-red-600 dark:text-red-400">{String(value)}</span>
                  </div>
                ))}
              </div>
            ) : (
              <span className="text-gray-500 dark:text-gray-400 italic">暂无属性</span>
            )}
          </div>
        </div>
      </div>
    );
  };

  // 格式化世界状态
  const formatWorldStatus = () => {
    return '暂无世界状态';
  };

  // 获取卡片样式
  const getCardStyle = (type: string) => {
    const baseStyle = "rounded-lg border-2 shadow-lg transition-all duration-300";
    
    if (isDark) {
      switch (type) {
        case 'quests':
          return `${baseStyle} bg-gradient-to-br from-indigo-900/60 to-indigo-800/40 border-indigo-400/60 text-indigo-100`;
        case 'dialogue':
          return `${baseStyle} bg-gradient-to-br from-amber-900/60 to-amber-800/40 border-amber-400/60 text-amber-100`;
        case 'assessment':
          return `${baseStyle} bg-gradient-to-br from-gray-800/70 to-gray-700/50 border-gray-400/60 text-gray-100`;
        case 'choices':
          return `${baseStyle} bg-gradient-to-br from-purple-900/60 to-purple-800/40 border-purple-400/60 text-purple-100`;
        case 'world':
          return `${baseStyle} bg-gradient-to-br from-green-900/60 to-green-800/40 border-green-400/60 text-green-100`;
        case 'character':
          return `${baseStyle} bg-gradient-to-br from-blue-900/60 to-blue-800/40 border-blue-400/60 text-blue-100`;
        default:
          return `${baseStyle} bg-gradient-to-br from-gray-800/70 to-gray-700/50 border-gray-400/60 text-gray-100`;
      }
    } else {
      switch (type) {
        case 'quests':
          return `${baseStyle} bg-gradient-to-br from-indigo-100/90 to-indigo-50/70 border-indigo-500/70 text-indigo-900`;
        case 'dialogue':
          return `${baseStyle} bg-gradient-to-br from-amber-100/90 to-amber-50/70 border-amber-500/70 text-amber-900`;
        case 'assessment':
          return `${baseStyle} bg-gradient-to-br from-gray-200/90 to-gray-100/70 border-gray-500/70 text-gray-900`;
        case 'choices':
          return `${baseStyle} bg-gradient-to-br from-purple-100/90 to-purple-50/70 border-purple-500/70 text-purple-900`;
        case 'world':
          return `${baseStyle} bg-gradient-to-br from-green-100/90 to-green-50/70 border-green-500/70 text-green-900`;
        case 'character':
          return `${baseStyle} bg-gradient-to-br from-blue-100/90 to-blue-50/70 border-blue-500/70 text-blue-900`;
        default:
          return `${baseStyle} bg-gradient-to-br from-gray-200/90 to-gray-100/70 border-gray-500/70 text-gray-900`;
      }
    }
  };


  return (
    <div className="w-full">
      
      <div className="flex gap-4 min-h-[400px]">
        {/* 左列：任务列表和系统评估 */}
        <div className="flex-1 min-w-0 flex flex-col space-y-4">
          {/* 任务列表 */}
          <Card className={`${getCardStyle('quests')}`}>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-sm font-bold">
                <ScrollText className="w-4 h-4" />
                任务列表
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0">
              <div className="text-sm leading-relaxed max-h-[200px] overflow-y-auto">
                {content.quests ? (
                  (() => {
                    try {
                      // 首先尝试解析为JSON（新格式）
                      const questData = JSON.parse(content.quests);
                      return <div dangerouslySetInnerHTML={{ 
                        __html: formatQuestContent(questData) 
                      }} />;
                    } catch (e) {
                      // 如果JSON解析失败，使用纯文本格式化（旧格式）
                      return <div dangerouslySetInnerHTML={{ 
                        __html: formatQuestTextContent(content.quests) 
                      }} />;
                    }
                  })()
                ) : (
                  // 如果没有quests内容，尝试从assessment数据中获取任务信息
                  (() => {
                    if (content.assessment) {
                      try {
                        // 提取评估数据中的任务信息
                        let assessmentJson = content.assessment;
                        if (assessmentJson && assessmentJson.includes('§')) {
                          const match = assessmentJson.match(ASSESSMENT_PATTERNS.full);
                          if (match) {
                            assessmentJson = match[1];
                          } else {
                            // 如果没有匹配到完整模式，手动移除§符号
                            assessmentJson = assessmentJson.replace(/^§/, '').replace(/§$/, '');
                          }
                        }
                        
                        if (assessmentJson) {
                          const assessmentData = JSON.parse(assessmentJson);
                          if (assessmentData.questUpdates) {
                            const questUpdates = assessmentData.questUpdates;
                            let questContent = '';
                            
                            // 处理活跃任务
                            if (questUpdates.progress && questUpdates.progress.length > 0) {
                              questContent += '<div class="mb-4"><h5 class="font-bold text-sm mb-3 flex items-center gap-2 text-green-600 dark:text-green-400"><div class="w-3 h-3 rounded-full bg-green-500 shadow-sm"></div><span class="text-shadow-sm">活跃任务</span></h5><div class="space-y-3">';
                              questUpdates.progress.forEach((quest: any) => {
                                questContent += `<div class="border-l-4 border-green-400 bg-green-50 dark:bg-green-900/20 p-3 rounded-r-lg shadow-md"><div class="font-bold text-sm mb-2">任务 ${quest.questId}</div><div class="text-gray-600 dark:text-gray-400 text-sm">进度: ${quest.progress}</div></div>`;
                              });
                              questContent += '</div></div>';
                            }
                            
                            // 处理新任务
                            if (questUpdates.created && questUpdates.created.length > 0) {
                              questContent += '<div class="mb-4"><h5 class="font-bold text-sm mb-3 flex items-center gap-2 text-purple-600 dark:text-purple-400"><div class="w-3 h-3 rounded-full bg-purple-500 shadow-sm"></div><span class="text-shadow-sm">新任务</span></h5><div class="space-y-3">';
                              questUpdates.created.forEach((quest: any) => {
                                questContent += `<div class="border-l-4 border-purple-400 bg-purple-50 dark:bg-purple-900/20 p-3 rounded-r-lg shadow-md"><div class="font-bold text-sm mb-2">${quest.title || '新任务'}</div><div class="text-gray-600 dark:text-gray-400 text-sm">${quest.description || ''}</div></div>`;
                              });
                              questContent += '</div></div>';
                            }
                            
                            // 处理已完成任务
                            if (questUpdates.completed && questUpdates.completed.length > 0) {
                              questContent += '<div class="mb-4"><h5 class="font-bold text-sm mb-3 flex items-center gap-2 text-blue-600 dark:text-blue-400"><div class="w-3 h-3 rounded-full bg-blue-500 shadow-sm"></div><span class="text-shadow-sm">已完成任务</span></h5><div class="space-y-3">';
                              questUpdates.completed.forEach((quest: any) => {
                                questContent += `<div class="border-l-4 border-blue-400 bg-blue-50 dark:bg-blue-900/20 p-3 rounded-r-lg shadow-md"><div class="font-bold text-sm mb-2">${quest.title || '已完成任务'}</div><div class="text-gray-600 dark:text-gray-400 text-sm">${quest.description || ''}</div></div>`;
                              });
                              questContent += '</div></div>';
                            }
                            
                            if (questContent) {
                              return <div dangerouslySetInnerHTML={{ __html: questContent }} />;
                            }
                          }
                        }
                      } catch (e) {
                        console.warn('解析评估数据中的任务信息失败:', e);
                      }
                    }
                    
                    return '暂无活跃任务';
                  })()
                )}
              </div>
            </CardContent>
          </Card>

          {/* 系统评估 */}
          {(content.assessment || isAssessing) && (
            <Card className={`flex-1 ${getCardStyle('assessment')}`}>
              <CardHeader className="pb-3">
                <CardTitle className="flex items-center gap-2 text-sm font-bold">
                  <BookOpen className="w-4 h-4" />
                  系统评估
                </CardTitle>
              </CardHeader>
              <CardContent className="pt-0 flex-1 overflow-hidden">
                <div className="text-sm leading-relaxed h-full overflow-y-auto">
                  {isAssessing ? (
                    <div className="flex items-center justify-center py-8">
                      <div className="flex items-center gap-2 text-gray-500 dark:text-gray-400">
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-current"></div>
                        系统评估中...
                      </div>
                    </div>
                  ) : content.assessment ? (
                    (() => {
                      try {
                        // 如果评估内容包含§符号，先提取JSON部分
                        let assessmentJson = content.assessment;
                        if (assessmentJson && assessmentJson.includes('§')) {
                          const match = assessmentJson.match(ASSESSMENT_PATTERNS.full);
                          if (match) {
                            assessmentJson = match[1];
                          } else {
                            // 如果没有匹配到完整模式，手动移除§符号
                            assessmentJson = assessmentJson.replace(/^§/, '').replace(/§$/, '');
                          }
                        }
                        
                        if (assessmentJson) {
                          // 清理JSON格式：移除多余的逗号和空白字符
                          assessmentJson = assessmentJson
                            .replace(/,(\s*[}\]])/g, '$1') // 移除对象/数组末尾的逗号
                            .replace(/\s+/g, ' ') // 压缩多个空白字符为单个空格
                            .trim();
                          
                          console.log('🔍 [GameLayout] 清理后的JSON:', assessmentJson);
                          
                          const assessmentData = JSON.parse(assessmentJson);
                          return <div dangerouslySetInnerHTML={{ 
                            __html: formatAssessmentContent(assessmentData) 
                          }} />;
                        } else {
                          // 如果没有有效的JSON内容，显示原始内容
                          return <div className="text-sm text-gray-600 dark:text-gray-400">
                            <div className="font-bold mb-2">系统评估</div>
                            <div className="whitespace-pre-wrap">{content.assessment}</div>
                          </div>;
                        }
                      } catch (e) {
                        console.warn('解析评估JSON失败:', e);
                        console.warn('原始评估内容:', content.assessment);
                        // 如果JSON解析失败，尝试作为纯文本处理
                        return <div className="text-sm text-gray-600 dark:text-gray-400">
                          <div className="font-bold mb-2">系统评估</div>
                          <div className="whitespace-pre-wrap">{content.assessment}</div>
                        </div>;
                      }
                    })()
                  ) : null}
                </div>
              </CardContent>
            </Card>
          )}
        </div>

      {/* 中列：对话和选择 */}
      <div className="flex-1 min-w-0 max-w-[50%] flex flex-col space-y-4">
        {/* 解析后的对话内容 */}
        {content.dialogue && (
          <Card className={`flex-1 ${getCardStyle('dialogue')}`}>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-sm font-bold">
                <MessageCircle className="w-4 h-4" />
                对话与叙述
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0 flex-1 overflow-hidden">
              <div className="text-sm leading-relaxed whitespace-pre-wrap h-full overflow-y-auto">
                {content.dialogue}
              </div>
            </CardContent>
          </Card>
        )}

        {/* 行动选择 */}
        {choices.length > 0 && (
          <Card className={`${getCardStyle('choices')}`}>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-sm font-bold">
                <ArrowRight className="w-4 h-4" />
                行动选择
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0">
              <div className="grid grid-cols-1 gap-2 max-h-[300px] overflow-y-auto">
                {choices.map((choice, index) => {
                  const isFreeAction = choice.title.includes('自由行动') || 
                                     choice.title.toLowerCase().includes('free action') || 
                                     choice.title.includes('其他行动');
                  
                  return (
                    <button
                      key={index}
                      onClick={() => {
                        if (!isLoading) {
                          if (isFreeAction) {
                            onFreeActionModeChange(true);
                          } else {
                            onChoiceSelect(choice.title);
                          }
                        }
                      }}
                      disabled={isLoading}
                      className={`p-3 rounded-lg text-left transition-all duration-200 border-2 ${
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
                          {choice.title}
                        </span>
                        {choice.description && (
                          <span className={`text-xs opacity-80 ${
                            isFreeAction
                              ? isDark ? 'text-orange-200' : 'text-orange-700'
                              : isDark ? 'text-purple-200' : 'text-purple-700'
                          }`}>
                            {choice.description}
                          </span>
                        )}
                      </div>
                    </button>
                  );
                })}
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {/* 右列：世界状态和角色状态 */}
      <div className="flex-1 min-w-0 flex flex-col space-y-4">
        {/* 世界状态 */}
        <Card className={`${getCardStyle('world')}`}>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-sm font-bold">
              <Globe className="w-4 h-4" />
              世界状态
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-0">
            <div className="text-sm leading-relaxed whitespace-pre-wrap max-h-[200px] overflow-y-auto">
              {content.world ? content.world : formatWorldStatus()}
            </div>
          </CardContent>
        </Card>

        {/* 角色状态 */}
        <Card className={`flex-1 ${getCardStyle('character')}`}>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-sm font-bold">
              <User className="w-4 h-4" />
              角色状态
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-0 flex-1 overflow-hidden">
            <div className="text-sm leading-relaxed h-full overflow-y-auto">
              {formatCharacterStatus()}
            </div>
          </CardContent>
        </Card>
      </div>
      </div>
    </div>
  );
});

GameLayout.displayName = 'GameLayout';

export default GameLayout;

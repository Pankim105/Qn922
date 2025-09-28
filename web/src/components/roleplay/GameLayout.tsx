import React, { memo, useMemo, useCallback } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from 'modern-ui-components';
import { 
  ScrollText, 
  MessageCircle, 
  // BookOpen, // 已注释掉系统评估卡片，不再需要
  ArrowRight, 
  Globe, 
  User
} from 'lucide-react';
import type { GameLayoutProps } from './types';
import { formatQuestContent, formatQuestTextContent, /* formatAssessmentContent, */ formatStatusContent, formatDialogueContent } from './messageFormatter';
import { ASSESSMENT_PATTERNS } from './constants';


const GameLayout: React.FC<GameLayoutProps> = memo(({
  isDark,
  isLoading,
  // isAssessing = false, // 已注释掉系统评估卡片，不再需要
  onChoiceSelect,
  onFreeActionModeChange,
  skillsState,
  structuredContent
}) => {
  // 使用useMemo缓存解析结果，避免重复计算
  const content = useMemo(() => {
    if (structuredContent) {
      return {
        dialogue: structuredContent.dialogue || '',
        world: structuredContent.world || '',
        quests: structuredContent.quests || '',
        choices: structuredContent.choices || '',
        assessment: structuredContent.assessment || ''
      };
    }
    return {
      dialogue: '',
      world: '',
      quests: '',
      choices: '',
      assessment: ''
    };
  }, [structuredContent]);
  

  // 使用useMemo缓存选择项解析结果
  const choices = useMemo(() => {
    if (!content.choices) return [];
    
    const choices = [];
    
    // 支持多种分号：英文分号(;)、中文分号(；)、全角分号(；)
    const semicolonPattern = /[;；]/;
    
    if (semicolonPattern.test(content.choices)) {
      // 使用正则表达式分割，支持所有类型的分号
      const choiceItems = content.choices.split(semicolonPattern).map(item => item.trim()).filter(item => item);
      choices.push(...choiceItems.map(item => {
        // 处理带编号的格式：1. **标题** - 描述
        const match = item.match(/^(\d+)\.\s*\*\*(.*?)\*\*\s*-\s*(.+)$/);
        if (match) {
          return { title: match[2], description: match[3] };
        }
        // 处理无编号的格式：**标题** - 描述
        const noNumberMatch = item.match(/^\*\*(.*?)\*\*\s*-\s*(.+)$/);
        if (noNumberMatch) {
          return { title: noNumberMatch[1], description: noNumberMatch[2] };
        }
        // 处理简单格式：直接是标题
        return { title: item, description: '' };
      }));
    } else {
      // 如果没有分号，按行分割
      const choiceLines = content.choices.split('\n').filter(line => line.trim());
      choices.push(...choiceLines.map(line => {
        const match = line.match(/^\d+\.\s*(.+)/);
        return { title: match ? match[1] : line, description: '' };
      }));
    }
    
    return choices;
  }, [content.choices]);

  // 使用useMemo缓存角色状态格式化结果
  const characterStatusElement = useMemo(() => {
    if (!skillsState) {
      return '暂无角色状态';
    }
    
    // 如果skillsState是字符串，尝试解析为JSON
    let parsedSkillsState = skillsState;
    if (typeof skillsState === 'string') {
      try {
        parsedSkillsState = JSON.parse(skillsState);
      } catch (e) {
        console.warn('解析skillsState JSON失败:', e);
        return '角色状态数据格式错误';
      }
    }
    
    const { 
      characterName, 
      profession, 
      selectedSkills, 
      skillLevels, 
      attributes,
      level, 
      experience, 
      gold, 
      inventory,
      生命值,
      魔力值

    } = parsedSkillsState;

    // 职业名称映射
    const professionMap: { [key: string]: string } = {
      'private_detective': '私家侦探',
      'detective': '侦探',
      'police': '警察',
      'investigator': '调查员',
      'scientist': '科学家',
      'doctor': '医生',
      'lawyer': '律师',
      'journalist': '记者'
    };

    const displayProfession = professionMap[profession] || profession || '未知';

    // 属性名称映射
    const attributeMap: { [key: string]: string } = {
      'strength': '力量',
      'dexterity': '敏捷',
      'constitution': '体质',
      'intelligence': '智力',
      'wisdom': '智慧',
      'charisma': '魅力'
    };

    // 技能名称映射 - 基于数据库世界模板中的技能ID
    const skillMap: { [key: string]: string } = {
      // 异世界探险 (fantasy_adventure)
      'sword_mastery': '剑术精通',
      'magic_affinity': '魔法亲和',
      'stealth': '潜行',
      'healing': '治疗术',
      'archery': '弓箭术',
      'alchemy': '炼金术',
      'beast_taming': '野兽驯服',
      'lockpicking': '开锁术',
      
      // 龙与魔法 (dragon_magic)
      'holy_magic': '神圣魔法',
      'elemental_magic': '元素魔法',
      'sword_techniques': '剑技',
      'nature_lore': '自然知识',
      'divine_protection': '神圣护佑',
      'beast_communication': '野兽沟通',
      
      // 武侠世界 (wuxia_world)
      'sword_art': '剑法',
      'internal_energy': '内功',
      'light_footwork': '轻功',
      'poison_resistance': '毒抗',
      'acupuncture': '点穴',
      'meditation': '冥想',
      
      // 现代都市 (modern_city) - 测试数据中的技能
      'deduction': '推理',
      'surveillance': '监视',
      'investigation': '调查',
      'combat': '战斗',
      'magic': '魔法',
      'persuasion': '说服',
      'intimidation': '恐吓',
      'perception': '感知',
      'athletics': '运动',
      'acrobatics': '杂技',
      'sleight_of_hand': '巧手',
      'arcana': '奥秘',
      'history': '历史',
      'nature': '自然',
      'religion': '宗教',
      'animal_handling': '动物驯养',
      'insight': '洞察',
      'medicine': '医药',
      'survival': '生存',
      'deception': '欺骗',
      'performance': '表演'
    };
    
    return (
      <div className="h-full flex flex-col space-y-2">
        {/* 角色基本信息 */}
        <div className="bg-gradient-to-r from-blue-50 to-blue-100 dark:from-blue-900/30 dark:to-blue-800/30 rounded-lg p-3 border border-blue-200 dark:border-blue-700">
          <div className="grid grid-cols-2 gap-2 text-sm">
            <div className="flex justify-between items-center">
              <span className="font-semibold text-blue-600 dark:text-blue-400">角色:</span>
              <span className="font-bold text-blue-700 dark:text-blue-300 text-lg">{characterName || '未知'}</span>
            </div>
            
            <div className="flex justify-between items-center">
              <span className="font-semibold text-blue-600 dark:text-blue-400">职业:</span>
              <span className="font-bold text-blue-700 dark:text-blue-300 text-sm">{displayProfession}</span>
            </div>
            
            <div className="flex justify-between items-center">
              <span className="font-semibold text-blue-600 dark:text-blue-400">等级:</span>
              <span className="font-bold text-blue-700 dark:text-blue-300 text-lg">{level || 1}</span>
            </div>
            
            <div className="flex justify-between items-center">
              <span className="font-semibold text-green-600 dark:text-green-400">经验:</span>
              <span className="font-bold text-green-700 dark:text-green-300 text-sm">{experience || 0}</span>
            </div>
            
            <div className="flex justify-between items-center col-span-2">
              <span className="font-semibold text-yellow-600 dark:text-yellow-400">金币:</span>
              <span className="font-bold text-yellow-700 dark:text-yellow-300 text-sm">💰 {gold || 0}</span>
            </div>
          </div>
        </div>
        
        {/* 生命值和魔力值 */}
        {(生命值 || 魔力值) && (
          <div className="bg-gradient-to-r from-red-50 to-pink-100 dark:from-red-900/30 dark:to-pink-800/30 rounded-lg p-3 border border-red-200 dark:border-red-700">
            <div className="grid grid-cols-2 gap-2 text-sm">
              {生命值 && (
                <div className="flex justify-between items-center">
                  <span className="font-semibold text-red-600 dark:text-red-400">❤️ 生命值:</span>
                  <span className="font-bold text-red-700 dark:text-red-300 text-sm">{生命值}</span>
                </div>
              )}
              {魔力值 && (
                <div className="flex justify-between items-center">
                  <span className="font-semibold text-red-600 dark:text-red-400">💙 魔力值:</span>
                  <span className="font-bold text-red-700 dark:text-red-300 text-sm">{魔力值}</span>
                </div>
              )}
            </div>
          </div>
        )}
        
        {/* 技能 */}
        <div className="bg-gradient-to-r from-orange-50 to-orange-100 dark:from-orange-900/30 dark:to-orange-800/30 rounded-lg p-3 border border-orange-200 dark:border-orange-700">
          <span className="font-semibold text-orange-600 dark:text-orange-400 block mb-2 text-sm">技能:</span>
          <div className="text-sm">
            {selectedSkills && selectedSkills.length > 0 ? (
              <div className="grid grid-cols-1 gap-1">
                {selectedSkills.map((skill: any, index: number) => {
                  const skillLevel = skillLevels && skillLevels[skill] ? skillLevels[skill] : 1;
                  return (
                    <div key={index} className="bg-orange-50/80 dark:bg-orange-900/30 px-2 py-1 rounded border border-orange-200 dark:border-orange-600 text-gray-700 dark:text-gray-300 flex justify-between items-center">
                      <span className="font-bold text-gray-800 dark:text-gray-200">⚡ {skillMap[skill] || skill}</span>
                      <span className="font-bold text-orange-700 dark:text-orange-300 text-sm">Lv.{skillLevel}</span>
                    </div>
                  );
                })}
              </div>
            ) : (
              <span className="text-gray-500 dark:text-gray-400 italic">暂无技能</span>
            )}
          </div>
        </div>
        
        {/* 属性 */}
        <div className="bg-gradient-to-r from-red-50 to-red-100 dark:from-red-900/30 dark:to-red-800/30 rounded-lg p-3 border border-red-200 dark:border-red-700">
          <span className="font-semibold text-red-600 dark:text-red-400 block mb-2 text-sm">属性:</span>
          <div className="text-sm">
            {attributes ? (
              <div className="grid grid-cols-2 gap-1">
                {Object.entries(attributes).map(([key, value]) => (
                  <div key={key} className="bg-red-50/80 dark:bg-red-900/30 px-2 py-1 rounded border border-red-200 dark:border-red-600 flex justify-between items-center">
                    <span className="font-semibold text-gray-600 dark:text-gray-400">{attributeMap[key] || key}:</span>
                    <span className="font-bold text-red-700 dark:text-red-300">{String(value)}</span>
                  </div>
                ))}
              </div>
            ) : (
              <span className="text-gray-500 dark:text-gray-400 italic">暂无属性</span>
            )}
          </div>
        </div>
        
        {/* 物品 */}
        <div className="bg-gradient-to-r from-purple-50 to-purple-100 dark:from-purple-900/30 dark:to-purple-800/30 rounded-lg p-3 border border-purple-200 dark:border-purple-700">
          <span className="font-semibold text-purple-600 dark:text-purple-400 block mb-2 text-sm">物品:</span>
          <div className="text-sm">
            {inventory && inventory.length > 0 ? (
              <div className="grid grid-cols-1 gap-1">
                {inventory.map((item: any, index: number) => (
                  <div key={index} className="bg-purple-50/80 dark:bg-purple-900/30 px-2 py-1 rounded border border-purple-200 dark:border-purple-600 text-gray-700 dark:text-gray-300">
                    <span className="font-bold">📦 {item}</span>
                  </div>
                ))}
              </div>
            ) : (
              <span className="text-gray-500 dark:text-gray-400 italic">暂无物品</span>
            )}
          </div>
        </div>
      </div>
    );
  }, [skillsState]);

  // 使用useMemo缓存世界状态格式化结果
  const worldStatusElement = useMemo(() => {
    return formatStatusContent(content.world);
  }, [content.world]);

  // 使用useMemo缓存卡片样式
  const getCardStyle = useCallback((type: string) => {
    const baseStyle = "rounded-lg border-2 shadow-lg";
    
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
  }, [isDark]);


  return (
    <div className="w-full h-full flex flex-col">
      
      <div className="flex gap-4 flex-1 min-h-0">
        {/* 左列：任务列表和系统评估 */}
        <div className="flex-1 min-w-0 flex flex-col">
          <Card className={`flex-1 ${getCardStyle('quests')}`}>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-sm font-bold">
                <ScrollText className="w-4 h-4" />
                任务列表
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0 flex-1 overflow-hidden">
              <div className="text-sm leading-relaxed h-full overflow-y-auto">
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
                                questContent += `<div class="border-l-4 border-green-400 bg-green-50/80 dark:bg-green-900/30 p-3 rounded-r-lg shadow-md"><div class="font-bold text-sm mb-2 text-gray-800 dark:text-gray-200">任务 ${quest.questId}</div><div class="text-gray-600 dark:text-gray-300 text-sm">进度: ${quest.progress}</div></div>`;
                              });
                              questContent += '</div></div>';
                            }
                            
                            // 处理新任务
                            if (questUpdates.created && questUpdates.created.length > 0) {
                              questContent += '<div class="mb-4"><h5 class="font-bold text-sm mb-3 flex items-center gap-2 text-purple-600 dark:text-purple-400"><div class="w-3 h-3 rounded-full bg-purple-500 shadow-sm"></div><span class="text-shadow-sm">新任务</span></h5><div class="space-y-3">';
                              questUpdates.created.forEach((quest: any) => {
                                questContent += `<div class="border-l-4 border-purple-400 bg-purple-50/80 dark:bg-purple-900/30 p-3 rounded-r-lg shadow-md"><div class="font-bold text-sm mb-2 text-gray-800 dark:text-gray-200">${quest.title || '新任务'}</div><div class="text-gray-600 dark:text-gray-300 text-sm">${quest.description || ''}</div></div>`;
                              });
                              questContent += '</div></div>';
                            }
                            
                            // 处理已完成任务
                            if (questUpdates.completed && questUpdates.completed.length > 0) {
                              questContent += '<div class="mb-4"><h5 class="font-bold text-sm mb-3 flex items-center gap-2 text-blue-600 dark:text-blue-400"><div class="w-3 h-3 rounded-full bg-blue-500 shadow-sm"></div><span class="text-shadow-sm">已完成任务</span></h5><div class="space-y-3">';
                              questUpdates.completed.forEach((quest: any) => {
                                questContent += `<div class="border-l-4 border-blue-400 bg-blue-50/80 dark:bg-blue-900/30 p-3 rounded-r-lg shadow-md"><div class="font-bold text-sm mb-2 text-gray-800 dark:text-gray-200">${quest.title || '已完成任务'}</div><div class="text-gray-600 dark:text-gray-300 text-sm">${quest.description || ''}</div></div>`;
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
                
                {/* 系统评估 - 已注释掉 */}
                {/* {(content.assessment || isAssessing) && (
                  <div className="mt-4 pt-4 border-t border-gray-200/30 dark:border-gray-600/30">
                    <div className="flex items-center gap-2 mb-3">
                      <BookOpen className="w-4 h-4" />
                      <span className="font-bold text-sm">系统评估</span>
                    </div>
                    <div className="text-sm leading-relaxed">
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
                  </div>
                )} */}
              </div>
            </CardContent>
          </Card>
        </div>

      {/* 第二列：对话与叙述 */}
      <div className="flex-1 min-w-0 flex flex-col">
        <Card className={`flex-1 ${getCardStyle('dialogue')}`}>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-sm font-bold">
              <MessageCircle className="w-4 h-4" />
              对话与叙述
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-0 flex-1 overflow-hidden">
            <div className="text-sm leading-relaxed h-full overflow-y-auto" dangerouslySetInnerHTML={{ 
              __html: content.dialogue ? formatDialogueContent(content.dialogue) : '<div class="text-center py-8 text-gray-500 dark:text-gray-400">暂无对话内容</div>'
            }} />
          </CardContent>
        </Card>
      </div>

      {/* 第三列：世界状态 */}
      <div className="flex-1 min-w-0 flex flex-col">
        <Card className={`flex-1 ${getCardStyle('world')}`}>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-sm font-bold">
              <Globe className="w-4 h-4" />
              世界状态
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-0 flex-1 overflow-hidden">
            <div className="text-sm leading-relaxed h-full overflow-y-auto">
              <div dangerouslySetInnerHTML={{ __html: worldStatusElement }} />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 第四列：角色状态 */}
      <div className="flex-1 min-w-0 flex flex-col">
        <Card className={`flex-1 ${getCardStyle('character')}`}>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-sm font-bold">
              <User className="w-4 h-4" />
              角色状态
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-0 flex-1 overflow-hidden">
            <div className="text-sm leading-relaxed h-full overflow-y-auto">
              {characterStatusElement}
            </div>
          </CardContent>
        </Card>
      </div>
      </div>

      {/* 行动选择 - 单独一行 */}
      {choices.length > 0 && (
        <div className="mt-4">
          <Card className={`${getCardStyle('choices')}`}>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-sm font-bold">
                <ArrowRight className="w-4 h-4" />
                行动选择
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-0">
              <div className="flex gap-2 overflow-x-auto overflow-y-hidden">
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
                      className={`p-3 rounded-lg text-left border-2  max-w-[200px] ${
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
        </div>
      )}
    </div>
  );
});

GameLayout.displayName = 'GameLayout';

export default GameLayout;

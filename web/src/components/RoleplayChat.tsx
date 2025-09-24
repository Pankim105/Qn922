import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Button } from 'modern-ui-components';
import { Send, Dice6, MapPin, BookOpen, Sparkles, Crown, Sword, GraduationCap, Heart, Wand2, Shield, Star, Zap, User, ScrollText, Globe, MessageCircle, ArrowRight, ChevronDown } from 'lucide-react';

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}

interface WorldTemplate {
  worldId: string;
  worldName: string;
  description: string;
}

interface MessageSection {
  type: 'dialogue' | 'status' | 'world' | 'choices' | 'quests' | 'assessment' | 'plain';
  content: string;
  title?: string;
  icon?: React.ReactNode;
}

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  worldType?: string;
  roleName?: string; // AI角色名称
  sections?: MessageSection[]; // 结构化内容
  isStructured?: boolean; // 是否为结构化消息
  lastParsedLength?: number; // 上次解析时的内容长度
}

interface DiceResult {
  diceType: number;
  modifier: number;
  result: number;
  finalResult: number;
  context: string;
  isSuccessful: boolean;
}

interface RoleplayChatProps {
  isAuthenticated: boolean;
  user: User | null;
  onAuthFailure: () => void;
}

const RoleplayChat: React.FC<RoleplayChatProps> = ({ isAuthenticated, user: _user, onAuthFailure }) => {
  const [worlds, setWorlds] = useState<WorldTemplate[]>([]);
  const [selectedWorld, setSelectedWorld] = useState<string>('');
  const [sessionId, setSessionId] = useState<string>('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isInitialized, setIsInitialized] = useState(false);
  const [isDark, setIsDark] = useState(false);
  const [visibleMessageCount, setVisibleMessageCount] = useState(10);
  const [isFreeActionMode, setIsFreeActionMode] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // 世界类型图标映射
  const worldIcons = {
    fantasy_adventure: <Sword className="w-4 h-4" />,
    western_magic: <Wand2 className="w-4 h-4" />,
    martial_arts: <Crown className="w-4 h-4" />,
    japanese_school: <Heart className="w-4 h-4" />,
    educational: <GraduationCap className="w-4 h-4" />
  };

  // 世界类型颜色映射 - 浅色模式友好
  const worldColors = {
    fantasy_adventure: {
      light: 'from-purple-100 to-indigo-200 text-purple-900',
      dark: 'from-purple-600 to-indigo-700 text-white'
    },
    western_magic: {
      light: 'from-blue-100 to-cyan-200 text-blue-900',
      dark: 'from-blue-600 to-cyan-700 text-white'
    },
    martial_arts: {
      light: 'from-red-100 to-orange-200 text-red-900',
      dark: 'from-red-600 to-orange-700 text-white'
    },
    japanese_school: {
      light: 'from-pink-100 to-rose-200 text-pink-900',
      dark: 'from-pink-600 to-rose-700 text-white'
    },
    educational: {
      light: 'from-green-100 to-emerald-200 text-green-900',
      dark: 'from-green-600 to-emerald-700 text-white'
    }
  };

  // AI角色颜色映射 - 为不同角色提供不同颜色
  const aiRoleColors = {
    '游戏主持人': {
      light: 'from-violet-100 to-purple-200 text-violet-900',
      dark: 'from-violet-600 to-purple-700 text-white'
    },
    '贤者向导': {
      light: 'from-indigo-100 to-blue-200 text-indigo-900',
      dark: 'from-indigo-600 to-blue-700 text-white'
    },
    '江湖前辈': {
      light: 'from-amber-100 to-orange-200 text-amber-900',
      dark: 'from-amber-600 to-orange-700 text-white'
    },
    '校园向导': {
      light: 'from-rose-100 to-pink-200 text-rose-900',
      dark: 'from-rose-600 to-pink-700 text-white'
    },
    '智慧导师': {
      light: 'from-emerald-100 to-green-200 text-emerald-900',
      dark: 'from-emerald-600 to-green-700 text-white'
    },
    '向导': {
      light: 'from-gray-100 to-slate-200 text-gray-900',
      dark: 'from-gray-600 to-slate-700 text-white'
    }
  };

  // AI角色图标映射
  const aiRoleIcons = {
    '游戏主持人': <Shield className="w-3 h-3" />,
    '贤者向导': <Star className="w-3 h-3" />,
    '江湖前辈': <Sword className="w-3 h-3" />,
    '校园向导': <Heart className="w-3 h-3" />,
    '智慧导师': <GraduationCap className="w-3 h-3" />,
    '向导': <Zap className="w-3 h-3" />
  };

  // 解析结构化消息内容 - 支持后端AI返回格式
  const parseStructuredMessage = useCallback((content: string): MessageSection[] => {
    const sections: MessageSection[] = [];

    // 定义后端AI返回的标记模式
    const patterns = [
      { type: 'dialogue', regex: /\/\*DIALOGUE:(.*?)\*\//gs, title: '对话与叙述', icon: <MessageCircle className="w-4 h-4" /> },
      { type: 'status', regex: /\/\*STATUS:(.*?)\*\//gs, title: '角色状态', icon: <User className="w-4 h-4" /> },
      { type: 'world', regex: /\/\*WORLD:(.*?)\*\//gs, title: '世界状态', icon: <Globe className="w-4 h-4" /> },
      { type: 'quests', regex: /\/\*QUESTS\s*(.*?)\*\//gs, title: '任务信息', icon: <ScrollText className="w-4 h-4" /> },
      { type: 'choices', regex: /\/\*CHOICES:(.*?)\*\//gs, title: '行动选择', icon: <ArrowRight className="w-4 h-4" /> },
      { type: 'assessment', regex: /\/\*ASSESSMENT:(.*?)\*\//gs, title: '评估信息', icon: <BookOpen className="w-4 h-4" /> }
    ];

    // 提取指令标记（如[DICE:...]、[QUEST:...]、[MEMORY:...]等）
    const extractInstructions = (text: string) => {
      const instructions: { type: string; content: string; original: string }[] = [];

      // 匹配各种指令格式
      const instructionPatterns = [
        { pattern: /\[DICE:[^\]]+\]/g, type: 'dice' },
        { pattern: /\[QUEST:[^\]]+\]/g, type: 'quest' },
        { pattern: /\[MEMORY:[^\]]+\]/g, type: 'memory' },
        { pattern: /\[STATE:[^\]]+\]/g, type: 'state' },
        { pattern: /\[CHARACTER:[^\]]+\]/g, type: 'character' },
        { pattern: /\[WORLD:[^\]]+\]/g, type: 'world_expand' }
      ];

      for (const { pattern, type } of instructionPatterns) {
        const matches = [...text.matchAll(pattern)];
        for (const match of matches) {
          instructions.push({
            type,
            content: match[0],
            original: match[0]
          });
        }
      }

      return instructions;
    };

    let remainingContent = content;
    let hasStructuredContent = false;
    const extractedInstructions: { type: string; content: string; original: string }[] = [];

    // 提取结构化内容
    for (const pattern of patterns) {
      const matches = [...content.matchAll(pattern.regex)];
      // 只在开发模式下输出调试信息，并减少频率
      
      for (const match of matches) {
        const sectionContent = match[1].trim();

        // 提取该段落中的指令
        const instructions = extractInstructions(sectionContent);
        extractedInstructions.push(...instructions);

        // 从内容中移除指令标记（保留纯文本）
        let cleanContent = sectionContent;
        for (const instruction of instructions) {
          cleanContent = cleanContent.replace(instruction.original, '');
        }

        sections.push({
          type: pattern.type as MessageSection['type'],
          content: cleanContent.trim(),
          title: pattern.title
        });

        // 从剩余内容中移除已匹配的部分
        remainingContent = remainingContent.replace(match[0], '');
        hasStructuredContent = true;
      }
    }

    // 处理评估JSON（被§包裹）- 优先处理，避免与/*ASSESSMENT:冲突
    const assessmentMatch = content.match(/§({.*?})§/s);
    if (assessmentMatch) {
      try {
        const assessmentData = JSON.parse(assessmentMatch[1]);
        sections.push({
          type: 'assessment',
          content: JSON.stringify(assessmentData, null, 2),
          title: '系统评估'
        });
        remainingContent = remainingContent.replace(assessmentMatch[0], '');
        hasStructuredContent = true;
      } catch (e) {
        console.warn('解析评估JSON失败:', e);
      }
    }

    // 如果有剩余的普通内容，添加为plain类型
    const plainContent = remainingContent.trim();
    if (plainContent) {
      // 提取普通内容中的指令
      const instructions = extractInstructions(plainContent);
      extractedInstructions.push(...instructions);

      let cleanPlainContent = plainContent;
      for (const instruction of instructions) {
        cleanPlainContent = cleanPlainContent.replace(instruction.original, '');
      }

      sections.unshift({
        type: 'plain',
        content: cleanPlainContent.trim()
      });
    }

    // 如果没有结构化内容，整个消息作为plain处理
    if (!hasStructuredContent) {
      const instructions = extractInstructions(content);
      extractedInstructions.push(...instructions);

      let cleanContent = content;
      for (const instruction of instructions) {
        cleanContent = cleanContent.replace(instruction.original, '');
      }

      return [{
        type: 'plain',
        content: cleanContent.trim()
      }];
    }

    return sections;
  }, []);

  // 获取卡片尺寸配置 - 游戏UI风格
  const getCardConfig = (type: MessageSection['type']) => {
    switch (type) {
      case 'dialogue':
        return { cols: 'col-span-full', size: 'large', priority: 1 };
      case 'choices':
        return { cols: 'col-span-full', size: 'large', priority: 2 };
      case 'quests':
        return { cols: 'col-span-2', size: 'medium', priority: 3 };
      case 'assessment':
        return { cols: 'col-span-2', size: 'medium', priority: 3 };
      case 'status':
        return { cols: 'col-span-1', size: 'small', priority: 4 };
      case 'world':
        return { cols: 'col-span-1', size: 'small', priority: 4 };
      default:
        return { cols: 'col-span-1', size: 'small', priority: 5 };
    }
  };

  // 缓存解析结果，避免重复计算
  const parseSectionContent = useCallback(() => {
    const cache = new Map<string, any>();
    
    return (section: MessageSection) => {
      const cacheKey = `${section.type}-${section.content.substring(0, 100)}-${section.content.length}`;
      
      if (cache.has(cacheKey)) {
        return cache.get(cacheKey);
      }
      
      let result: any = null;
      
      // 任务信息解析
      if (section.type === 'quests') {
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
      
      cache.set(cacheKey, result);
      return result;
    };
  }, []);

  // 检查内容是否为空或只有空白字符
  const isContentEmpty = (content: string) => {
    if (!content) return true;
    const trimmed = content.trim();
    if (!trimmed) return true;
    
    // 检查是否只包含常见的空内容标识
    const emptyPatterns = [
      '暂无评估信息',
      '暂无活跃任务',
      '任务信息正在加载中...',
      '[无活跃任务]',
      '无活跃任务',
      '目前无活跃任务',
      '暂无任务',
      '评估信息正在加载中',
      '{}', // 空的JSON对象
      '[]', // 空的JSON数组
      'null',
      'undefined'
    ];
    
    // 检查是否匹配空内容模式
    if (emptyPatterns.some(pattern => trimmed === pattern)) {
      return true;
    }
    
    // 检查是否是空的JSON对象或数组
    if (trimmed === '{}' || trimmed === '[]' || trimmed === 'null') {
      return true;
    }
    
    // 检查是否只包含空白字符和标点符号
    if (trimmed.length < 3 && /^[\s\.,;:!?\-_]*$/.test(trimmed)) {
      return true;
    }
    
    return false;
  };

  // 渲染消息部分 - 游戏信息面板风格
  const renderMessageSection = useCallback((section: MessageSection, index: number) => {
    // 检查内容是否为空，如果为空则不渲染
    if (isContentEmpty(section.content)) {
      return null;
    }

    const getSectionIcon = (type: MessageSection['type']) => {
      switch (type) {
        case 'status': return <User className="w-3 h-3" />;
        case 'world': return <Globe className="w-3 h-3" />;
        case 'choices': return <ArrowRight className="w-3 h-3" />;
        case 'dialogue': return <MessageCircle className="w-3 h-3" />;
        case 'quests': return <ScrollText className="w-3 h-3" />;
        case 'assessment': return <BookOpen className="w-3 h-3" />;
        default: return <ScrollText className="w-3 h-3" />;
      }
    };

    const getSectionStyle = (type: MessageSection['type']) => {
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
            return `${baseStyle} bg-gradient-to-br from-amber-900/60 to-amber-800/40 border-amber-400/60 text-amber-100 shadow-amber-500/20`;
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
            return `${baseStyle} bg-gradient-to-br from-amber-100/90 to-amber-50/70 border-amber-500/70 text-amber-900 shadow-amber-500/30`;
          case 'quests':
            return `${baseStyle} bg-gradient-to-br from-indigo-100/90 to-indigo-50/70 border-indigo-500/70 text-indigo-900 shadow-indigo-500/30`;
          case 'assessment':
            return `${baseStyle} bg-gradient-to-br from-gray-200/90 to-gray-100/70 border-gray-500/70 text-gray-900 shadow-gray-500/30`;
          default:
            return `${baseStyle} bg-gradient-to-br from-gray-200/90 to-gray-100/70 border-gray-500/70 text-gray-900 shadow-gray-500/30`;
        }
      }
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
      const parsedContent = parseSectionContent()(section);
      
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
      const parsedContent = parseSectionContent()(section);
      
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
      const choiceLines = section.content.split('\n').filter(line => line.trim());
      const choices = choiceLines.map(line => {
        const match = line.match(/^\d+\.\s*(.+)/);
        return match ? match[1] : line;
      }).filter(choice => choice.trim()); // 过滤掉空选择项
      
      // 如果没有有效的选择项，不渲染选择卡片
      if (choices.length === 0) {
        return null;
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
                        setInputMessage('');
                        setIsFreeActionMode(true);
                        // 滚动到输入框并聚焦
                        setTimeout(() => {
                          if (inputRef.current) {
                            inputRef.current.focus();
                          }
                        }, 100);
                      } else {
                        // 普通选择：直接发送
                        setInputMessage(choiceTitle);
                        sendMessage();
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

    // 特殊处理markdown格式的状态和世界信息
    if (section.type === 'status' || section.type === 'world') {
      const parsedContent = parseSectionContent()(section);
      
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
  }, [parseSectionContent, isDark, isLoading, setInputMessage, inputRef]);

  // 格式化纯文本任务内容
  const formatQuestTextContent = (content: string) => {
    // 处理纯文本格式的任务信息
    let formattedContent = content;
    
    // 处理常见的任务状态文本
    if (content.includes('目前无活跃任务') || content.includes('暂无任务') || 
        content.includes('[无活跃任务]') || content.includes('无活跃任务')) {
      return `<div class="text-center py-4">
        <div class="text-gray-500 dark:text-gray-400 text-sm">
          <div class="w-8 h-8 mx-auto mb-2 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
            <span class="text-gray-400 dark:text-gray-500">📋</span>
          </div>
          暂无活跃任务
        </div>
      </div>`;
    }
    
    // 处理结构化的任务格式（活跃任务、已完成任务等）
    if (content.includes('**活跃任务**') || content.includes('**已完成任务**') || content.includes('活跃任务')) {
      let sections = '';
      
      // 分割不同的任务类型
      const activeMatch = content.match(/\*\*活跃任务\*\*(.*?)(?=\*\*|$)/s) || 
                         content.match(/活跃任务\s*(.*?)(?=已完成任务|$)/s);
      const completedMatch = content.match(/\*\*已完成任务\*\*(.*?)(?=\*\*|$)/s) ||
                           content.match(/已完成任务\s*(.*?)(?=活跃任务|$)/s);
      
      if (activeMatch) {
        const activeContent = activeMatch[1].trim();
        
        if (activeContent) {
          sections += `
            <div class="mb-4">
              <h5 class="font-bold text-sm mb-3 flex items-center gap-2 text-green-600 dark:text-green-400">
                <div class="w-3 h-3 rounded-full bg-green-500 shadow-sm"></div>
                <span class="text-shadow-sm">活跃任务</span>
              </h5>
              <div class="space-y-3">
                ${formatTaskItems(activeContent, 'active')}
              </div>
            </div>
          `;
        } else {
          // 没有具体任务内容时显示占位符
          sections += `
            <div class="mb-4">
              <h5 class="font-bold text-sm mb-3 flex items-center gap-2 text-green-600 dark:text-green-400">
                <div class="w-3 h-3 rounded-full bg-green-500 shadow-sm"></div>
                <span class="text-shadow-sm">活跃任务</span>
              </h5>
              <div class="text-center py-4">
                <div class="text-gray-500 dark:text-gray-400 text-sm">
                  <div class="w-8 h-8 mx-auto mb-2 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
                    <span class="text-gray-400 dark:text-gray-500">📋</span>
                  </div>
                  暂无活跃任务
                </div>
              </div>
            </div>
          `;
        }
      }
      
      if (completedMatch) {
        const completedContent = completedMatch[1].trim();
        
        if (completedContent) {
          sections += `
            <div class="mb-4">
              <h5 class="font-bold text-sm mb-3 flex items-center gap-2 text-blue-600 dark:text-blue-400">
                <div class="w-3 h-3 rounded-full bg-blue-500 shadow-sm"></div>
                <span class="text-shadow-sm">已完成任务</span>
              </h5>
              <div class="space-y-3">
                ${formatTaskItems(completedContent, 'completed')}
              </div>
            </div>
          `;
        }
      }
      
      // 如果只有标题没有内容，显示默认消息
      if (!sections) {
        sections = `
          <div class="text-center py-4">
            <div class="text-gray-500 dark:text-gray-400 text-sm">
              <div class="w-8 h-8 mx-auto mb-2 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
                <span class="text-gray-400 dark:text-gray-500">📋</span>
              </div>
              任务信息正在加载中...
            </div>
          </div>
        `;
      }
      
      return `<div class="space-y-1">${sections}</div>`;
    }
    
    // 处理新的简化任务格式（数字开头的任务列表）
    if (/^\d+\.\s/.test(content.trim())) {
      const lines = content.split('\n').map(line => line.trim()).filter(line => line);
      let sections = '';
      
      if (lines.length > 0) {
        sections += `
          <div class="mb-4">
            <h5 class="font-bold text-sm mb-3 flex items-center gap-2 text-green-600 dark:text-green-400">
              <div class="w-3 h-3 rounded-full bg-green-500 shadow-sm"></div>
              <span class="text-shadow-sm">活跃任务</span>
            </h5>
            <div class="space-y-3">
              ${formatSimplifiedTaskItems(lines)}
            </div>
          </div>
        `;
      }
      
      return `<div class="space-y-1">${sections}</div>`;
    }
    
    // 处理列表格式的任务信息
    if (content.includes('- ')) {
      formattedContent = formattedContent.replace(/^- (.+)$/gm, (_, item) => {
        return `<div class="flex items-start gap-2 py-1">
          <div class="w-1.5 h-1.5 rounded-full bg-indigo-500 mt-2 flex-shrink-0"></div>
          <span class="text-gray-700 dark:text-gray-300">${item}</span>
        </div>`;
      });
    }
    
    // 处理简单的任务标题（如纯文本"活跃任务"）
    if (content.trim() === '活跃任务' || content.trim() === '已完成任务') {
      return `
        <div class="text-center py-4">
          <div class="text-gray-500 dark:text-gray-400 text-sm">
            <div class="w-8 h-8 mx-auto mb-2 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
              <span class="text-gray-400 dark:text-gray-500">📋</span>
            </div>
            ${content.trim() === '活跃任务' ? '暂无活跃任务' : '暂无已完成任务'}
          </div>
        </div>
      `;
    }
    
    // 处理粗体文本
    formattedContent = formattedContent.replace(/\*\*(.*?)\*\*/g, '<span class="font-bold text-gray-800 dark:text-gray-200">$1</span>');
    
    return `<div class="space-y-1">${formattedContent}</div>`;
  };

  // 格式化简化的任务项目
  const formatSimplifiedTaskItems = (lines: string[]) => {
    let result = '';
    for (const line of lines) {
      // 解析格式：1. 任务名：任务描述（奖励：...）
      const match = line.match(/^(\d+)\.\s*([^：:]+)[：:](.+)$/);
      if (match) {
        let taskTitle = match[2].trim();
        const taskDescription = match[3].trim();
        
        // 处理任务标题中的markdown格式（去除**）
        taskTitle = taskTitle.replace(/\*\*/g, '');
        
        result += `
          <div class="border-l-4 border-green-400 bg-green-50 dark:bg-green-900/20 p-3 rounded-r-lg shadow-md">
            <div class="font-bold text-sm mb-2">${taskTitle}</div>
            <div class="text-gray-600 dark:text-gray-400 text-sm">${taskDescription}</div>
          </div>
        `;
      } else if (line.startsWith('*') && (line.includes('提示') || line.includes('奖励'))) {
        // 处理提示和奖励行，使用更淡的样式
        const content = line.replace(/^\*/, '').replace(/\*$/, '');
        result += `
          <div class="text-gray-500 dark:text-gray-400 text-xs italic ml-4 mt-1">
            ${content}
          </div>
        `;
      } else if (line.trim()) {
        // 其他非空行作为普通任务显示
        result += `
          <div class="border-l-4 border-green-400 bg-green-50 dark:bg-green-900/20 p-3 rounded-r-lg shadow-md">
            <div class="text-gray-600 dark:text-gray-400 text-sm">${line}</div>
          </div>
        `;
      }
    }
    
    return result;
  };

  // 格式化任务项目
  const formatTaskItems = (content: string, type: 'active' | 'completed') => {
    // 如果内容为空，返回空结果
    if (!content || !content.trim()) {
      return '';
    }
    
    const lines = content.split('\n').map(line => line.trim()).filter(line => line);
    let result = '';
    let currentTask = '';
    
    for (const line of lines) {
      // 任务标题（数字开头的粗体文本）
      if (/^\d+\.\s*\*\*(.*?)\*\*/.test(line)) {
        if (currentTask) {
          result += formatSingleTask(currentTask, type);
        }
        currentTask = line;
      } else if (line.startsWith('-') && currentTask) {
        // 处理任务详情行
        currentTask += '\n' + line;
      } else if (line.startsWith('-') && !currentTask) {
        // 如果没有当前任务但找到了任务详情，可能是格式问题
        // 忽略孤立的详情行
      } else if (currentTask) {
        // 其他内容也添加到当前任务中
        currentTask += '\n' + line;
      }
    }
    
    // 处理最后一个任务
    if (currentTask) {
      result += formatSingleTask(currentTask, type);
    }
    
    return result;
  };

  // 格式化单个任务
  const formatSingleTask = (taskText: string, type: 'active' | 'completed') => {
    const lines = taskText.split('\n');
    const titleMatch = lines[0].match(/^\d+\.\s*\*\*(.*?)\*\*/);
    const title = titleMatch ? titleMatch[1] : '未命名任务';
    
    let details = '';
    for (let i = 1; i < lines.length; i++) {
      const line = lines[i].trim();
      if (line.startsWith('- ')) {
        const detail = line.substring(2);
        details += `<div class="text-gray-600 dark:text-gray-400 text-sm mb-1">${detail}</div>`;
      }
    }
    
    const borderColor = type === 'active' ? 'border-green-400' : 'border-blue-400';
    const bgColor = type === 'active' ? 'bg-green-50 dark:bg-green-900/20' : 'bg-blue-50 dark:bg-blue-900/20';
    
    const result = `
      <div class="border-l-4 ${borderColor} ${bgColor} p-3 rounded-r-lg shadow-md">
        <div class="font-bold text-sm mb-2">${title}</div>
        ${details}
      </div>
    `;
    
    return result;
  };

  // 格式化任务内容 - 游戏UI风格
  const formatQuestContent = (questData: any) => {
    if (!questData) return '暂无任务信息';

    const formatQuestList = (quests: any[], title: string, type: string) => {
      if (!quests || quests.length === 0) return '';

      const getTypeColor = (type: string) => {
        switch (type) {
          case 'activeQuests': return 'border-l-green-400 bg-green-900/20 dark:bg-green-900/30';
          case 'completed': return 'border-l-blue-400 bg-blue-900/20 dark:bg-blue-900/30';
          case 'created': return 'border-l-purple-400 bg-purple-900/20 dark:bg-purple-900/30';
          case 'expired': return 'border-l-red-400 bg-red-900/20 dark:bg-red-900/30';
          default: return 'border-l-gray-400 bg-gray-900/20 dark:bg-gray-900/30';
        }
      };

      return `
        <div class="mb-4">
          <h5 class="font-bold text-sm mb-3 flex items-center gap-2">
            <div class="w-3 h-3 rounded-full bg-current shadow-sm"></div>
            <span class="text-shadow-sm">${title}</span>
          </h5>
          <div class="space-y-3">
            ${quests.map((quest, idx) => `
              <div class="border-l-4 ${getTypeColor(type)} p-3 rounded-r-lg shadow-md">
                <div class="font-bold text-sm mb-2">${idx + 1}. ${quest.title || quest.description || '未命名任务'}</div>
                ${quest.description ? `<div class="text-gray-500 dark:text-gray-300 mb-2 text-sm">${quest.description}</div>` : ''}
                <div class="flex flex-wrap gap-3 text-sm">
                  ${quest.progress ? `<span class="font-semibold text-green-600 dark:text-green-400 bg-green-100 dark:bg-green-900/30 px-2 py-1 rounded">进度: ${quest.progress}</span>` : ''}
                  ${quest.rewards ? `<span class="text-gray-600 dark:text-gray-400 bg-gray-100 dark:bg-gray-800/30 px-2 py-1 rounded">奖励: ${JSON.stringify(quest.rewards).replace(/"/g, '')}</span>` : ''}
                  ${quest.status ? `<span class="px-2 py-1 rounded text-sm font-bold ${quest.status === 'ACTIVE' ? 'bg-green-200 dark:bg-green-800 text-green-800 dark:text-green-200 shadow-sm' : quest.status === 'COMPLETED' ? 'bg-blue-200 dark:bg-blue-800 text-blue-800 dark:text-blue-200 shadow-sm' : 'bg-gray-200 dark:bg-gray-800 text-gray-800 dark:text-gray-200 shadow-sm'}"">${quest.status}</span>` : ''}
                </div>
              </div>
            `).join('')}
          </div>
        </div>
      `;
    };

    let content = '<div class="space-y-1">';

    if (questData.activeQuests && questData.activeQuests.length > 0) {
      content += formatQuestList(questData.activeQuests, '活跃任务', 'activeQuests');
    }

    if (questData.completed && questData.completed.length > 0) {
      content += formatQuestList(questData.completed, '已完成任务', 'completed');
    }

    if (questData.created && questData.created.length > 0) {
      content += formatQuestList(questData.created, '新任务', 'created');
    }

    if (questData.expired && questData.expired.length > 0) {
      content += formatQuestList(questData.expired, '过期任务', 'expired');
    }

    content += '</div>';
    return content || '暂无任务信息';
  };

  // 格式化状态内容 - 处理markdown格式
  const formatStatusContent = (content: string) => {
    // 处理markdown格式的状态信息
    let formattedContent = content;
    
    // 处理新格式的键值对（**键**: 值）
    formattedContent = formattedContent.replace(/^\*\*([^*]+)\*\*:\s*(.+)$/gm, (_, key, value) => {
      // 添加表情符号支持
      const keyWithEmoji = key.replace(/^(📍|🌅|🌤️|🔮|👥|⚡)?\s*/, '');
      const emoji = key.match(/^(📍|🌅|🌤️|🔮|👥|⚡)/)?.[0] || '';
      
      return `<div class="flex justify-between items-start py-2 border-b border-gray-200/30 dark:border-gray-600/30 last:border-b-0">
        <span class="font-bold text-gray-800 dark:text-gray-200 flex-shrink-0">
          ${emoji ? `<span class="mr-1">${emoji}</span>` : ''}${keyWithEmoji}:
        </span>
        <span class="text-gray-600 dark:text-gray-400 text-right ml-2">${value}</span>
      </div>`;
    });
    
    // 处理列表项（以 - 开头）
    formattedContent = formattedContent.replace(/^- (.+)$/gm, (_, item) => {
      // 检查是否是键值对格式（如 "等级：1"）
      if (item.includes('：')) {
        const [key, value] = item.split('：', 2);
        return `<div class="flex justify-between items-start py-2 border-b border-gray-200/30 dark:border-gray-600/30 last:border-b-0">
          <span class="font-bold text-gray-800 dark:text-gray-200 flex-shrink-0">${key}：</span>
          <span class="text-gray-600 dark:text-gray-400 text-right ml-2">${value}</span>
        </div>`;
      } else {
        // 普通列表项
        return `<div class="flex items-start gap-2 py-1">
          <div class="w-1.5 h-1.5 rounded-full bg-blue-500 mt-2 flex-shrink-0"></div>
          <span class="text-gray-700 dark:text-gray-300">${item}</span>
        </div>`;
      }
    });
    
    // 处理粗体文本（**text**）- 但排除已处理的键值对
    formattedContent = formattedContent.replace(/\*\*([^*]+)\*\*(?!:)/g, '<span class="font-bold text-gray-800 dark:text-gray-200">$1</span>');
    
    // 处理行内代码（`code`）
    formattedContent = formattedContent.replace(/`(.*?)`/g, '<code class="bg-gray-200 dark:bg-gray-700 px-1 py-0.5 rounded text-xs font-mono text-gray-800 dark:text-gray-200">$1</code>');
    
    // 处理特殊格式的括号内容（如 "（带暗格）"）
    formattedContent = formattedContent.replace(/（([^）]+)）/g, '<span class="text-gray-500 dark:text-gray-400 text-xs">（$1）</span>');
    
    return `<div class="space-y-0">${formattedContent}</div>`;
  };

  // 格式化纯文本评估内容
  const formatAssessmentTextContent = (content: string) => {
    // 处理纯文本格式的评估信息
    let formattedContent = content;
    
    // 处理常见的评估文本格式
    if (content.includes('综合评分:') || content.includes('评估策略:')) {
      // 处理键值对格式
      formattedContent = formattedContent.replace(/^(.+?):\s*(.+)$/gm, (_, key, value) => {
        return `<div class="flex justify-between items-center py-2 border-b border-gray-200/30 dark:border-gray-600/30 last:border-b-0">
          <span class="font-bold text-gray-800 dark:text-gray-200">${key}:</span>
          <span class="text-gray-600 dark:text-gray-400">${value}</span>
        </div>`;
      });
    }
    
    // 处理粗体文本
    formattedContent = formattedContent.replace(/\*\*(.*?)\*\*/g, '<span class="font-bold text-gray-800 dark:text-gray-200">$1</span>');
    
    // 处理列表项
    if (content.includes('- ')) {
      formattedContent = formattedContent.replace(/^- (.+)$/gm, (_, item) => {
        return `<div class="flex items-start gap-2 py-1">
          <div class="w-1.5 h-1.5 rounded-full bg-gray-500 mt-2 flex-shrink-0"></div>
          <span class="text-gray-700 dark:text-gray-300">${item}</span>
        </div>`;
      });
    }
    
    return `<div class="space-y-1">${formattedContent}</div>`;
  };

  // 格式化评估内容 - 游戏UI风格
  const formatAssessmentContent = (assessmentData: any) => {
    if (!assessmentData) return '';

    let content = `<div class="space-y-2">`;

    // 主要评分信息
    if (assessmentData.overallScore !== undefined || assessmentData.strategy) {
      content += `
        <div class="grid grid-cols-2 gap-3">
          ${assessmentData.overallScore !== undefined ? `
            <div class="bg-gradient-to-br from-blue-100/90 to-blue-50/70 dark:from-blue-900/40 dark:to-blue-800/30 p-3 rounded-lg border border-blue-300/50 dark:border-blue-600/50 shadow-md text-center">
              <div class="text-sm font-bold text-blue-600 dark:text-blue-400 mb-2">综合评分</div>
              <div class="text-lg font-bold ${(assessmentData.overallScore >= 0.8 ? 'text-green-600 dark:text-green-400' : assessmentData.overallScore >= 0.6 ? 'text-yellow-600 dark:text-yellow-400' : 'text-red-600 dark:text-red-400')}">
                ${Math.round(assessmentData.overallScore * 100)}%
              </div>
            </div>
          ` : ''}

          ${assessmentData.strategy ? `
            <div class="bg-gradient-to-br from-purple-100/90 to-purple-50/70 dark:from-purple-900/40 dark:to-purple-800/30 p-3 rounded-lg border border-purple-300/50 dark:border-purple-600/50 shadow-md text-center">
              <div class="text-sm font-bold text-purple-600 dark:text-purple-400 mb-2">评估策略</div>
              <div class="text-sm font-bold px-2 py-1 rounded ${
                assessmentData.strategy === 'ACCEPT' ? 'bg-green-200 dark:bg-green-800 text-green-800 dark:text-green-200 shadow-sm' :
                assessmentData.strategy === 'ADJUST' ? 'bg-yellow-200 dark:bg-yellow-800 text-yellow-800 dark:text-yellow-200 shadow-sm' :
                'bg-red-200 dark:bg-red-800 text-red-800 dark:text-red-200 shadow-sm'
              }">
                ${assessmentData.strategy}
              </div>
            </div>
          ` : ''}
        </div>
      `;
    }

    // 评估说明
    if (assessmentData.assessmentNotes) {
      content += `
        <div class="bg-gradient-to-br from-gray-100/90 to-gray-50/70 dark:from-gray-800/50 dark:to-gray-700/40 p-3 rounded-lg border border-gray-300/50 dark:border-gray-600/50 shadow-md">
          <div class="text-sm font-bold mb-2 text-gray-700 dark:text-gray-300">评估说明</div>
          <div class="text-sm text-gray-600 dark:text-gray-400 leading-relaxed">
            ${assessmentData.assessmentNotes}
          </div>
        </div>
      `;
    }

    // 建议行动
    if (assessmentData.suggestedActions && assessmentData.suggestedActions.length > 0) {
      content += `
        <div class="bg-gradient-to-br from-amber-100/90 to-amber-50/70 dark:from-amber-900/40 dark:to-amber-800/30 p-3 rounded-lg border border-amber-300/50 dark:border-amber-600/50 shadow-md">
          <div class="text-sm font-bold mb-2 text-amber-700 dark:text-amber-400">建议行动</div>
          <div class="space-y-2">
            ${assessmentData.suggestedActions.map((action: string) => `
              <div class="text-sm flex items-start gap-2">
                <div class="w-2 h-2 rounded-full bg-amber-500 mt-1.5 flex-shrink-0 shadow-sm"></div>
                <span class="text-gray-700 dark:text-gray-300">${action}</span>
              </div>
            `).join('')}
          </div>
        </div>
      `;
    }

    // 收敛提示
    if (assessmentData.convergenceHints && assessmentData.convergenceHints.length > 0) {
      content += `
        <div class="bg-gradient-to-br from-indigo-100/90 to-indigo-50/70 dark:from-indigo-900/40 dark:to-indigo-800/30 p-3 rounded-lg border border-indigo-300/50 dark:border-indigo-600/50 shadow-md">
          <div class="text-sm font-bold mb-2 text-indigo-700 dark:text-indigo-400">收敛提示</div>
          <div class="space-y-2">
            ${assessmentData.convergenceHints.map((hint: string) => `
              <div class="text-sm flex items-start gap-2">
                <div class="w-2 h-2 rounded-full bg-indigo-500 mt-1.5 flex-shrink-0 shadow-sm"></div>
                <span class="text-gray-700 dark:text-gray-300">${hint}</span>
              </div>
            `).join('')}
          </div>
        </div>
      `;
    }

    content += `</div>`;
    
    // 检查是否有实际内容，如果没有则返回空字符串
    const hasContent = assessmentData.overallScore !== undefined || 
                      assessmentData.strategy || 
                      assessmentData.assessmentNotes ||
                      (assessmentData.suggestedActions && assessmentData.suggestedActions.length > 0) ||
                      (assessmentData.convergenceHints && assessmentData.convergenceHints.length > 0);
    
    return hasContent ? content : '';
  };

  // 获取API令牌
  const getAuthHeaders = () => {
    const token = localStorage.getItem('accessToken');
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    };
  };

  // 监听主题变化
  useEffect(() => {
    const checkTheme = () => {
      const isDarkMode = document.documentElement.classList.contains('dark');
      console.log('Theme check:', { isDarkMode, classList: document.documentElement.classList.toString() });
      setIsDark(isDarkMode);
    };
    
    // 初始检查
    checkTheme();
    
    // 监听主题变化
    const observer = new MutationObserver(checkTheme);
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class']
    });
    
    return () => observer.disconnect();
  }, []);

  // 获取世界模板列表
  useEffect(() => {
    if (isAuthenticated) {
      fetchWorldTemplates();
    }
  }, [isAuthenticated]);

  const fetchWorldTemplates = async () => {
    try {
      const token = localStorage.getItem('accessToken');
      if (!token) {
        console.log('未找到访问令牌，跳过世界模板获取');
        return;
      }

      const response = await fetch('http://localhost:8080/api/roleplay/worlds', {
        headers: getAuthHeaders()
      });

      if (response.status === 401) {
        console.log('访问令牌已过期，触发重新认证');
        onAuthFailure();
        return;
      }

      if (response.ok) {
        const data = await response.json();
        setWorlds(data.data || []);
      } else {
        console.log('获取世界模板失败，状态码:', response.status);
      }
    } catch (error) {
      console.error('获取世界模板失败:', error);
    }
  };

  // 初始化角色扮演会话
  const initializeSession = async (worldType: string) => {
    if (!worldType) return;

    const newSessionId = `roleplay_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    setSessionId(newSessionId);
    setIsLoading(true);

    try {
      const response = await fetch(`http://localhost:8080/api/roleplay/sessions/${newSessionId}/initialize`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          worldType: worldType,
          godModeRules: "{}"
        })
      });

      if (response.status === 401) {
        onAuthFailure();
        return;
      }

      if (response.ok) {
        setIsInitialized(true);
        const roleName = getAIRoleName(worldType);
        setMessages([{
          id: '1',
          role: 'assistant',
          content: `欢迎来到${worlds.find(w => w.worldId === worldType)?.worldName || '角色扮演世界'}！我是您的${roleName}。让我们开始这场精彩的冒险吧！`,
          timestamp: new Date(),
          worldType: worldType,
          roleName: roleName
        }]);
      }
    } catch (error) {
      console.error('初始化会话失败:', error);
    } finally {
      setIsLoading(false);
    }
  };

  // 获取AI角色名称
  const getAIRoleName = (worldType: string) => {
    const roleNames = {
      fantasy_adventure: '游戏主持人',
      western_magic: '贤者向导',
      martial_arts: '江湖前辈',
      japanese_school: '校园向导',
      educational: '智慧导师'
    };
    return roleNames[worldType as keyof typeof roleNames] || '向导';
  };

  // 发送消息
  const sendMessage = async () => {
    if (!inputMessage.trim() || !sessionId || isLoading) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: inputMessage.trim(),
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setIsFreeActionMode(false); // 退出自由行动模式
    setIsLoading(true);
    
    // 确保新消息可见
    if (messages.length + 1 > visibleMessageCount) {
      setVisibleMessageCount(prev => prev + 1);
    }

    try {
      const response = await fetch('http://localhost:8080/api/roleplay/chat/stream', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          message: userMessage.content,
          sessionId: sessionId,
          worldType: selectedWorld,
          systemPrompt: `你是${getAIRoleName(selectedWorld)}，请用角色扮演的方式回应。`
        })
      });

      if (response.status === 401) {
        onAuthFailure();
        return;
      }

      if (response.ok) {
        // 创建临时的AI消息用于实时更新
        const roleName = getAIRoleName(selectedWorld);
        const tempMessageId = (Date.now() + 1).toString();
        const tempMessage: Message = {
          id: tempMessageId,
          role: 'assistant',
          content: '',
          timestamp: new Date(),
          worldType: selectedWorld,
          roleName: roleName
        };
        
        // 立即添加空的AI消息
        setMessages(prev => [...prev, tempMessage]);
        
        // 确保AI回复可见
        if (messages.length + 1 > visibleMessageCount) {
          setVisibleMessageCount(prev => prev + 1);
        }
        
        const reader = response.body?.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        let hasReceivedData = false;

        if (reader) {
          try {
          while (true) {
            const { done, value } = await reader.read();
              if (done) {
                break;
              }

              hasReceivedData = true;
              buffer += decoder.decode(value, { stream: true });
              const lines = buffer.split('\n');
              buffer = lines.pop() || '';

            for (const line of lines) {
                if (line.trim() === '') continue;

                if (line.startsWith('data:')) {
                  const data = line.substring(5).trim();
                  if (data === '[DONE]') continue;

                  try {
                    const event = JSON.parse(data);
                    if (event.content) {
                      setMessages(prev => prev.map(msg => {
                        if (msg.id === tempMessageId) {
                          const newContent = msg.content + event.content;

                          // 只在内容长度增加超过100个字符时才重新解析，减少解析频率
                          const shouldReparse = !msg.lastParsedLength || 
                                              newContent.length - msg.lastParsedLength > 100;

                          if (shouldReparse) {
                            const sections = parseStructuredMessage(newContent);
                            const isStructured = sections.length > 1 || sections[0].type !== 'plain';

                            return {
                              ...msg,
                              content: newContent,
                              sections: sections,
                              isStructured: isStructured,
                              lastParsedLength: newContent.length
                            };
                          }

                          return {
                            ...msg,
                            content: newContent
                          };
                        }
                        return msg;
                      }));
                  }
                } catch (e) {
                    console.warn('解析事件失败:', data, e);
                  }
                }
              }
            }
          } catch (readerError) {
            // 如果已经接收到数据并且流已经开始，网络错误可能是正常结束
            if (hasReceivedData) {
              console.log('流式连接结束 (可能是正常结束)');
            } else {
              throw readerError;
            }
          }
        }

        // 最终解析完整内容
        setMessages(prev => prev.map(msg => {
          if (msg.id === tempMessageId) {
            const sections = parseStructuredMessage(msg.content);
            const isStructured = sections.length > 1 || sections[0].type !== 'plain';

            return {
              ...msg,
              sections: sections,
              isStructured: isStructured
            };
          }
          return msg;
        }));
      }
    } catch (error) {
      console.error('发送消息失败:', error);
    } finally {
      setIsLoading(false);
    }
  };

  // 执行骰子检定
  const rollDice = async (diceType: number = 20) => {
    if (!sessionId) return;

    try {
      const response = await fetch(`http://localhost:8080/api/roleplay/sessions/${sessionId}/dice-roll`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          diceType: diceType,
          modifier: 0,
          context: "随机检定",
          difficultyClass: 15
        })
      });

      if (response.ok) {
        const data = await response.json();
        const result: DiceResult = data.data;
        
        const roleName = getAIRoleName(selectedWorld);
        const diceMessage: Message = {
          id: Date.now().toString(),
          role: 'assistant',
          content: `🎲 骰子检定结果：${result.result} (d${result.diceType}) + ${result.modifier} = ${result.finalResult} ${result.isSuccessful ? '✅ 成功' : '❌ 失败'}`,
          timestamp: new Date(),
          worldType: selectedWorld,
          roleName: roleName
        };
        
        setMessages(prev => [...prev, diceMessage]);
      }
    } catch (error) {
      console.error('骰子检定失败:', error);
    }
  };

  // 手动滚动到底部
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // 获取要显示的消息（只显示最近的指定数量）
  const getVisibleMessages = () => {
    if (messages.length <= visibleMessageCount) {
      return messages;
    }
    return messages.slice(-visibleMessageCount);
  };

  // 显示更多消息
  const showMoreMessages = () => {
    setVisibleMessageCount(prev => Math.min(prev + 10, messages.length));
  };

  // 显示全部消息
  const showAllMessages = () => {
    setVisibleMessageCount(messages.length);
  };

  // 为旧消息添加角色名称
  useEffect(() => {
    if (messages.length > 0 && selectedWorld) {
      const needsUpdate = messages.some(msg => 
        msg.role === 'assistant' && !msg.roleName && msg.worldType
      );
      
      if (needsUpdate) {
        setMessages(prevMessages => 
          prevMessages.map(msg => {
            if (msg.role === 'assistant' && !msg.roleName && msg.worldType) {
              return {
                ...msg,
                roleName: getAIRoleName(msg.worldType)
              };
            }
            return msg;
          })
        );
      }
    }
  }, [selectedWorld, messages.length]);

  // 获取消息卡片样式
  const getMessageCardStyle = (message: Message) => {
    const darkMode = isDark;

    if (message.role === 'user') {
      // 用户消息保持一致的蓝色样式，宽度适中
      return darkMode
        ? 'bg-gradient-to-r from-blue-600 to-blue-700 text-white ml-auto max-w-[40%]'
        : 'bg-gradient-to-r from-blue-100 to-blue-200 text-blue-900 ml-auto max-w-[40%]';
    } else {
      // AI消息根据角色类型使用不同颜色，宽度更宽
      const worldType = message.worldType || selectedWorld;
      const roleName = message.roleName || getAIRoleName(worldType);

      const roleColor = aiRoleColors[roleName as keyof typeof aiRoleColors];

      if (roleColor) {
        const colorScheme = darkMode ? roleColor.dark : roleColor.light;
        return `bg-gradient-to-r ${colorScheme} mr-auto max-w-[85%]`;
      } else {
        // 降级到世界类型颜色
        const worldColor = worldColors[worldType as keyof typeof worldColors];
        if (worldColor) {
          const colorScheme = darkMode ? worldColor.dark : worldColor.light;
          return `bg-gradient-to-r ${colorScheme} mr-auto max-w-[85%]`;
        } else {
          // 最终降级到默认颜色
          return darkMode
            ? 'bg-gradient-to-r from-gray-600 to-gray-700 text-white mr-auto max-w-[85%]'
            : 'bg-gradient-to-r from-gray-100 to-gray-200 text-gray-900 mr-auto max-w-[85%]';
        }
      }
    }
  };

  if (!isAuthenticated) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Sparkles className="w-5 h-5" />
            角色扮演世界
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-gray-600 dark:text-gray-400">请先登录以开始角色扮演冒险</p>
        </CardContent>
      </Card>
    );
  }

  return (
      <Card className="h-[600px] flex flex-col" key={`roleplay-${selectedWorld}-${isDark}`}>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Sparkles className="w-5 h-5" />
          角色扮演世界
        </CardTitle>
        
        {!isInitialized && (
          <div className="space-y-3">
            <p className="text-sm text-gray-600 dark:text-gray-400">选择一个世界开始冒险：</p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {worlds.map((world) => (
                <Button
                  key={world.worldId}
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    setSelectedWorld(world.worldId);
                    initializeSession(world.worldId);
                  }}
                  className="flex items-center gap-2 justify-start p-3 h-auto"
                  disabled={isLoading}
                >
                  {worldIcons[world.worldId as keyof typeof worldIcons]}
                  <div className="text-left">
                    <div className="font-medium">{world.worldName}</div>
                    <div className="text-xs text-gray-500 dark:text-gray-400 truncate">
                      {world.description}
                    </div>
                  </div>
                </Button>
              ))}
            </div>
          </div>
        )}

        {isInitialized && selectedWorld && (
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              {worldIcons[selectedWorld as keyof typeof worldIcons]}
              <span className="text-sm font-medium">
                {worlds.find(w => w.worldId === selectedWorld)?.worldName}
              </span>
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => rollDice(20)}
                className="flex items-center gap-1"
              >
                <Dice6 className="w-4 h-4" />
                d20
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setMessages([]);
                  setVisibleMessageCount(10); // 重置显示数量
                  initializeSession(selectedWorld);
                }}
                className="mr-2"
              >
                重置对话
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setIsInitialized(false);
                  setMessages([]);
                  setSessionId('');
                  setSelectedWorld('');
                  setVisibleMessageCount(10); // 重置显示数量
                }}
              >
                切换世界
              </Button>
            </div>
          </div>
        )}
      </CardHeader>

      {isInitialized && (
        <>
          <CardContent 
            ref={messagesContainerRef}
            className="flex-1 overflow-y-auto space-y-3 p-4"
          >
            {/* 显示更多消息按钮 */}
            {messages.length > visibleMessageCount && (
              <div className="flex justify-center gap-2 mb-4">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={showMoreMessages}
                  className={`transition-all duration-200 ${
                    isDark 
                      ? 'bg-gray-800 border-gray-600 text-gray-200 hover:bg-gray-700' 
                      : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  查看更多 (+10条)
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={showAllMessages}
                  className={`transition-all duration-200 ${
                    isDark 
                      ? 'bg-gray-800 border-gray-600 text-gray-200 hover:bg-gray-700' 
                      : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  显示全部 ({messages.length - visibleMessageCount} 条)
                </Button>
              </div>
            )}
            
            {getVisibleMessages().map((message, index) => (
              <div
                key={message.id}
                className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'} animate-in slide-in-from-bottom-2 duration-300`}
                style={{ animationDelay: `${index * 50}ms` }}
              >
                <div className={`rounded-lg p-4 shadow-md transition-all duration-200 hover:shadow-lg ${getMessageCardStyle(message)} max-w-[85%]`}>
                  {message.role === 'user' ? (
                    <div className="flex items-center gap-2 text-xs font-semibold mb-2 opacity-90">
                      <User className="w-3 h-3" />
                      <span>你</span>
                    </div>
                  ) : message.roleName && (
                    <div className="flex items-center gap-2 text-xs font-semibold mb-2 opacity-90">
                      {aiRoleIcons[message.roleName as keyof typeof aiRoleIcons]}
                      <span>{message.roleName}</span>
                    </div>
                  )}
                   <div className="text-sm leading-relaxed">
                     {message.isStructured && message.sections ? (
                       <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
                         {message.sections.map((section, sectionIndex) =>
                           renderMessageSection(section, sectionIndex)
                         )}
                       </div>
                     ) : (
                       <div className="whitespace-pre-wrap">
                    {message.content}
                  </div>
                     )}
                   </div>
                  <div className="text-xs opacity-70 mt-3 text-right">
                    {message.timestamp.toLocaleTimeString()}
                  </div>
                </div>
              </div>
            ))}
            
            {isLoading && (
              <div className="flex justify-start">
                <div className={`rounded-lg p-3 max-w-sm ${isDark ? 'bg-gray-700' : 'bg-gray-200'}`}>
                  <div className="flex items-center gap-2">
                    <div className="animate-spin rounded-full h-4 w-4 border-2 border-gray-400 border-t-transparent"></div>
                    <span className={`text-sm ${isDark ? 'text-gray-400' : 'text-gray-600'}`}>
                      {getAIRoleName(selectedWorld)}正在思考...
                    </span>
                  </div>
                </div>
              </div>
            )}
            
            <div ref={messagesEndRef} />
            
            {/* 滚动到底部按钮 */}
            <div className="fixed bottom-24 right-8 z-10">
              <Button
                variant="outline"
                size="sm"
                onClick={scrollToBottom}
                className={`rounded-full shadow-lg hover:shadow-xl transition-all duration-200 ${
                  isDark 
                    ? 'bg-gray-800 border-gray-600 text-gray-200 hover:bg-gray-700' 
                    : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'
                }`}
                title="滚动到底部"
              >
                <ChevronDown className="w-4 h-4" />
              </Button>
            </div>
          </CardContent>

          <div className="p-4 border-t border-gray-200 dark:border-gray-700">
            {isFreeActionMode && (
              <div className="mb-2 p-2 bg-orange-50 dark:bg-orange-900/20 border border-orange-200 dark:border-orange-800 rounded-md">
                <div className="flex items-center gap-2 text-sm text-orange-700 dark:text-orange-300">
                  <span>✏️</span>
                  <span>自由行动模式：请详细描述你想要进行的行动</span>
                </div>
              </div>
            )}
            <div className="flex gap-2">
              <input
                ref={inputRef}
                type="text"
                value={inputMessage}
                onChange={(e) => {
                  setInputMessage(e.target.value);
                  // 用户开始输入时退出自由行动模式
                  if (isFreeActionMode && e.target.value.length > 0) {
                    setIsFreeActionMode(false);
                  }
                }}
                placeholder={isFreeActionMode ? "✏️ 请详细描述你想要进行的行动..." : "输入你的行动或对话..."}
                onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
                className={`flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md 
                  bg-white dark:bg-gray-800 text-gray-900 dark:text-white
                  focus:outline-none focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400
                  disabled:bg-gray-100 dark:disabled:bg-gray-700 disabled:cursor-not-allowed
                  placeholder:text-gray-500 dark:placeholder:text-gray-400
                  ${isFreeActionMode ? 'ring-2 ring-orange-400 dark:ring-orange-500 border-orange-400 dark:border-orange-500' : ''}
                `}
                disabled={isLoading}
              />
              <Button
                onClick={sendMessage}
                disabled={isLoading || !inputMessage.trim()}
                variant="solid"
              >
                <Send className="w-4 h-4" />
              </Button>
            </div>
            
            <div className="flex gap-2 mt-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setInputMessage("我想查看当前状态")}
                className="text-xs"
              >
                <MapPin className="w-3 h-3 mr-1" />
                查看状态
              </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    // 测试后端AI返回格式的结构化消息渲染
                    const testMessage: Message = {
                      id: Date.now().toString(),
                      role: 'assistant',
                      content: `/*DIALOGUE:
🧙‍♂️ *我轻轻挥动法杖，一道淡金色的魔法符文在空中浮现，符文缓缓旋转着，散发着温暖的金光*

"年轻的冒险者，欢迎来到这个充满奇迹的世界！我是你的${getAIRoleName(selectedWorld)}。让我为你展示当前的状态和可用的选择。"
*/

/*STATUS:
{
  "level": 1,
  "experience": 0,
  "health": 100,
  "maxHealth": 100,
  "mana": 50,
  "maxMana": 50,
  "inventory": [],
  "abilities": ["基础冥想"],
  "stats": {
    "strength": 10,
    "dexterity": 10,
    "intelligence": 15,
    "wisdom": 12
  }
}
*/

/*WORLD:
📍 **当前位置**：神秘的起始石坛
🌄 这是一片被远古力量守护的圣地，四周环绕着十二根刻满神秘符文的巨石柱。石柱表面闪烁着微弱的魔法光芒，空气中弥漫着古老魔法的气息。远处隐约传来森林的低语和溪水的潺潺声。
*/

/*QUESTS:
{
  "activeQuests": [
    {
      "questId": "tutorial_001",
      "title": "初学者教程",
      "description": "学习基础的魔法和冒险知识",
      "status": "ACTIVE",
      "progress": "0/3",
      "rewards": {
        "exp": 100,
        "gold": 50,
        "items": ["学徒法杖x1", "魔法入门书x1"]
      }
    }
  ],
  "completed": [],
  "created": [],
  "expired": []
}
*/

/*CHOICES:
请选择你的第一个行动：
1. 🔥 学习基础火焰魔法 - 掌握最基础的元素操控能力
2. 🗺️ 探索神秘森林 - 寻找隐藏的宝藏和秘密
3. 📚 阅读古代典籍 - 了解这个世界的历史和传说
4. 🎲 进行属性检定 - 测试你的天赋和运气
5. 💬 与我对话 - 询问关于这个世界的任何问题
*/

§{
  "ruleCompliance": 1.0,
  "contextConsistency": 1.0,
  "convergenceProgress": 0.1,
  "overallScore": 0.9,
  "strategy": "ACCEPT",
  "assessmentNotes": "用户选择了标准的开局方式，完全符合世界规则和故事逻辑",
  "suggestedActions": ["引导用户完成教程任务", "介绍世界背景", "提供新手指导"],
  "convergenceHints": ["完成教程后解锁更多区域", "等级提升将开放新技能", "与NPC互动会触发支线剧情"],
  "questUpdates": {
    "created": [{
      "questId": "tutorial_001",
      "title": "初学者教程",
      "description": "学习基础的魔法和冒险知识",
      "rewards": {"exp": 100, "gold": 50, "items": ["学徒法杖x1", "魔法入门书x1"]}
    }]
  },
  "worldStateUpdates": {
    "currentLocation": "神秘的起始石坛",
    "environment": "宁静祥和，充满魔法气息",
    "npcs": [{"name": "导师精灵", "status": "友好且乐于助人"}],
    "worldEvents": ["新冒险者抵达仪式完成"]
  },
  "skillsStateUpdates": {
    "level": 1,
    "experience": 0,
    "gold": 0,
    "inventory": [],
    "abilities": ["基础冥想"],
    "stats": {"strength": 10, "dexterity": 10, "intelligence": 15, "wisdom": 12}
  }
}§`,
                      timestamp: new Date(),
                      worldType: selectedWorld,
                      roleName: getAIRoleName(selectedWorld),
                      isStructured: true
                    };
                    
                    // 解析并添加结构化内容
                    const sections = parseStructuredMessage(testMessage.content);
                    testMessage.sections = sections;
                    
                    setMessages(prev => [...prev, testMessage]);
                  }}
                  className="text-xs"
                >
                  <ScrollText className="w-3 h-3 mr-1" />
                  测试UI
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setInputMessage("我想学习新技能")}
                className="text-xs"
              >
                <BookOpen className="w-3 h-3 mr-1" />
                学习技能
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  // 测试任务信息渲染
                  const testMessage: Message = {
                    id: Date.now().toString(),
                    role: 'assistant',
                    content: `/*DIALOGUE:
雨滴敲打着雾港市老城区的街角，湿冷的风卷起你大衣的下摆。你在昏黄路灯下站定，从内袋中取出那本皮质封面的侦探笔记——边缘磨损，页角微卷，显然已被翻阅过无数次。你轻轻翻开第一页，字迹工整却透着一丝仓促：
> "10月3日，晴。'黑鸦俱乐部'的账目有异，资金流向不明……"
> "10月7日，雨。目击者失踪，警局档案被调换……注意怀表时间。"
> "最后一页夹着一张泛黄的照片：一座钟楼，指针停在11:55。"
你又取出银质怀表，表面刻着一行小字："时间不会说谎，但人会。" 打开表盖，指针停在——11:55。奇怪，你记得刚才还是午夜。
你轻敲表背，一声细微"咔哒"响起。背面弹出一道暗格，藏有一张微型胶片，上面模糊印着一串数字：**23-1-14-9-7-5-18**。
远处传来钟声，仿佛回应着什么。
*/

/*STATUS:
- 等级：1
- 装备：侦探大衣、皮质侦探笔记、银质怀表（带暗格）
- 物品：微型胶片（23-1-14-9-7-5-18）、泛黄照片（钟楼）
- 技能：观察力（基础）、密码学直觉（觉醒中）
*/

/*WORLD:
- 位置：雾港市·老城区街角
- 环境：深夜降雨，路灯昏黄，空气中弥漫着铁锈与海水的咸味
- 时间异常：怀表始终指向11:55，与现实时间不符
- 隐藏线索：微型胶片上的数字可能对应字母（A=1, B=2...）
*/

/*QUESTS:
{
  "activeQuests": [
    {
      "questId": "detective_case_001",
      "title": "黑鸦俱乐部谜案",
      "description": "调查黑鸦俱乐部可疑的资金流向，寻找失踪目击者的下落",
      "status": "ACTIVE",
      "progress": "1/5",
      "rewards": {
        "exp": 500,
        "gold": 200,
        "items": ["情报笔记x3", "神秘钥匙x1"]
      }
    },
    {
      "questId": "time_puzzle_001",
      "title": "时间悖论",
      "description": "调查怀表时间异常现象，找出11:55的秘密含义",
      "status": "ACTIVE",
      "progress": "0/3",
      "rewards": {
        "exp": 300,
        "gold": 150,
        "items": ["时间碎片x1"]
      }
    }
  ],
  "completed": [],
  "created": [],
  "expired": []
}
*/

/*CHOICES:
请选择你的行动：

1. 尝试解密数字序列：23-1-14-9-7-5-18（建议进行一次语言或密码检定）

2. 前往附近警局档案室，查询"黑鸦俱乐部"相关记录

3. 根据照片寻找钟楼位置，实地勘察

4. 对怀表进行更深入检查，测试其机械结构是否还有其他机关
*/

§{
  "ruleCompliance": 0.95,
  "contextConsistency": 0.98,
  "convergenceProgress": 0.85,
  "overallScore": 0.89,
  "strategy": "ACCEPT",
  "assessmentNotes": "行为高度契合侦探世界设定，触发关键线索，推动主线收敛",
  "suggestedActions": ["优先处理时间悖论线索", "注意收集更多证据", "保持对异常现象的警惕"],
  "convergenceHints": ["黑鸦俱乐部与时间异常有关联", "钟楼是下一个重要地点", "数字序列可能指向关键人物"],
  "questUpdates": {
    "progress": [{"questId": "detective_case_001", "progress": "1/5"}]
  },
  "worldStateUpdates": {
    "currentLocation": "雾港市·老城区街角",
    "environment": "深夜降雨，充满神秘气息",
    "npcs": [{"name": "神秘路人", "status": "可疑"}],
    "worldEvents": ["发现时间异常", "获得关键线索"]
  },
  "skillsStateUpdates": {
    "level": 1,
    "experience": 50,
    "gold": 0,
    "inventory": ["侦探大衣", "皮质侦探笔记", "银质怀表（带暗格）", "微型胶片", "泛黄照片"],
    "abilities": ["观察力（基础）", "密码学直觉（觉醒中）"],
    "stats": {"strength": 12, "dexterity": 14, "intelligence": 16, "wisdom": 15}
  }
}§`,
                    timestamp: new Date(),
                    worldType: selectedWorld,
                    roleName: getAIRoleName(selectedWorld),
                    isStructured: true
                  };

                  // 解析并添加结构化内容
                  const sections = parseStructuredMessage(testMessage.content);
                  testMessage.sections = sections;

                  setMessages(prev => [...prev, testMessage]);
                }}
                className="text-xs"
              >
                <BookOpen className="w-3 h-3 mr-1" />
                任务测试
              </Button>
            </div>
          </div>
        </>
      )}
    </Card>
  );
};

export default RoleplayChat;
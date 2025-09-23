import React, { useState, useEffect, useRef } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Button, ModernInput } from 'modern-ui-components';
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
  type: 'dialogue' | 'status' | 'world' | 'choices' | 'plain';
  content: string;
  title?: string;
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
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);

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

  // 解析结构化消息内容
  const parseStructuredMessage = (content: string): MessageSection[] => {
    const sections: MessageSection[] = [];
    
    // 定义标记模式
    const patterns = [
      { type: 'status', regex: /\/\*STATUS:(.*?)\*\//gs, title: '角色状态' },
      { type: 'world', regex: /\/\*WORLD:(.*?)\*\//gs, title: '世界状态' },
      { type: 'choices', regex: /\/\*CHOICES:(.*?)\*\//gs, title: '行动选择' },
      { type: 'dialogue', regex: /\/\*DIALOGUE:(.*?)\*\//gs, title: '对话' }
    ];
    
    let remainingContent = content;
    let hasStructuredContent = false;
    
    // 提取结构化内容
    for (const pattern of patterns) {
      const matches = [...content.matchAll(pattern.regex)];
      for (const match of matches) {
        sections.push({
          type: pattern.type as MessageSection['type'],
          content: match[1].trim(),
          title: pattern.title
        });
        // 从剩余内容中移除已匹配的部分
        remainingContent = remainingContent.replace(match[0], '');
        hasStructuredContent = true;
      }
    }
    
    // 如果有剩余的普通内容，添加为plain类型
    const plainContent = remainingContent.trim();
    if (plainContent) {
      sections.unshift({
        type: 'plain',
        content: plainContent
      });
    }
    
    // 如果没有结构化内容，整个消息作为plain处理
    if (!hasStructuredContent) {
      return [{
        type: 'plain',
        content: content
      }];
    }
    
    return sections;
  };

  // 渲染消息部分
  const renderMessageSection = (section: MessageSection, index: number) => {
    const getSectionIcon = (type: MessageSection['type']) => {
      switch (type) {
        case 'status': return <User className="w-4 h-4" />;
        case 'world': return <Globe className="w-4 h-4" />;
        case 'choices': return <ArrowRight className="w-4 h-4" />;
        case 'dialogue': return <MessageCircle className="w-4 h-4" />;
        default: return <ScrollText className="w-4 h-4" />;
      }
    };

    const getSectionStyle = (type: MessageSection['type']) => {
      const baseStyle = "rounded-lg p-3 mb-3 border-l-4";
      
      if (isDark) {
        switch (type) {
          case 'status':
            return `${baseStyle} bg-blue-900 border-l-blue-400 text-blue-100`;
          case 'world':
            return `${baseStyle} bg-green-900 border-l-green-400 text-green-100`;
          case 'choices':
            return `${baseStyle} bg-purple-900 border-l-purple-400 text-purple-100`;
          case 'dialogue':
            return `${baseStyle} bg-amber-900 border-l-amber-400 text-amber-100`;
          default:
            return `${baseStyle} bg-gray-700 border-l-gray-400 text-gray-100`;
        }
      } else {
        switch (type) {
          case 'status':
            return `${baseStyle} bg-blue-50 border-l-blue-500 text-blue-900`;
          case 'world':
            return `${baseStyle} bg-green-50 border-l-green-500 text-green-900`;
          case 'choices':
            return `${baseStyle} bg-purple-50 border-l-purple-500 text-purple-900`;
          case 'dialogue':
            return `${baseStyle} bg-amber-50 border-l-amber-500 text-amber-900`;
          default:
            return `${baseStyle} bg-gray-50 border-l-gray-400 text-gray-900`;
        }
      }
    };

    if (section.type === 'plain') {
      return (
        <div key={index} className="whitespace-pre-wrap text-sm leading-relaxed">
          {section.content}
        </div>
      );
    }

    return (
      <div key={index} className={getSectionStyle(section.type)}>
        {section.title && (
          <div className="flex items-center gap-2 font-semibold text-sm mb-2 opacity-90">
            {getSectionIcon(section.type)}
            <span>{section.title}</span>
          </div>
        )}
        <div className="whitespace-pre-wrap text-sm leading-relaxed">
          {section.content}
        </div>
      </div>
    );
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
                      setMessages(prev => prev.map(msg =>
                        msg.id === tempMessageId
                          ? { ...msg, content: msg.content + event.content }
                          : msg
                      ));
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

        // 标记AI消息为完成，并解析结构化内容
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
      // 用户消息保持一致的蓝色样式
      return darkMode 
        ? 'bg-gradient-to-r from-blue-600 to-blue-700 text-white ml-auto max-w-xs'
        : 'bg-gradient-to-r from-blue-100 to-blue-200 text-blue-900 ml-auto max-w-xs';
    } else {
      // AI消息根据角色类型使用不同颜色
      const worldType = message.worldType || selectedWorld;
      const roleName = message.roleName || getAIRoleName(worldType);
      
      const roleColor = aiRoleColors[roleName as keyof typeof aiRoleColors];
      
      if (roleColor) {
        const colorScheme = darkMode ? roleColor.dark : roleColor.light;
        return `bg-gradient-to-r ${colorScheme} mr-auto max-w-sm`;
      } else {
        // 降级到世界类型颜色
        const worldColor = worldColors[worldType as keyof typeof worldColors];
        if (worldColor) {
          const colorScheme = darkMode ? worldColor.dark : worldColor.light;
          return `bg-gradient-to-r ${colorScheme} mr-auto max-w-sm`;
        } else {
          // 最终降级到默认颜色
          return darkMode 
            ? 'bg-gradient-to-r from-gray-600 to-gray-700 text-white mr-auto max-w-sm'
            : 'bg-gradient-to-r from-gray-100 to-gray-200 text-gray-900 mr-auto max-w-sm';
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
                <div className={`rounded-lg p-3 shadow-md transition-all duration-200 hover:shadow-lg ${getMessageCardStyle(message)}`}>
                  {message.role === 'user' ? (
                    <div className="flex items-center gap-1 text-xs font-semibold mb-2 opacity-90">
                      <User className="w-3 h-3" />
                      <span>你</span>
                    </div>
                  ) : message.roleName && (
                    <div className="flex items-center gap-1 text-xs font-semibold mb-2 opacity-90">
                      {aiRoleIcons[message.roleName as keyof typeof aiRoleIcons]}
                      <span>{message.roleName}</span>
                    </div>
                  )}
                   <div className="text-sm leading-relaxed">
                     {message.isStructured && message.sections ? (
                       message.sections.map((section, index) => 
                         renderMessageSection(section, index)
                       )
                     ) : (
                       <div className="whitespace-pre-wrap">
                    {message.content}
                  </div>
                     )}
                   </div>
                  <div className="text-xs opacity-70 mt-2 text-right">
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
            <div className="flex gap-2">
              <ModernInput
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                placeholder="输入你的行动或对话..."
                onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
                className="flex-1"
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
                    // 测试结构化消息渲染
                    const testMessage: Message = {
                      id: Date.now().toString(),
                      role: 'assistant',
                      content: `/*DIALOGUE:
🧙‍♂️ *我轻轻挥动法杖，一道淡金色的魔法符文在空中浮现* 

"年轻的旅者，让我为你展示当前的状态..."
*/

/*STATUS:
👤 **角色状态**：
- 🌟 等级：1（初出茅庐的冒险者）
- ❤️ 生命值：100/100
- 💙 魔力值：50/50
- 🎒 背包：空荡荡的行囊
*/

/*WORLD:
📍 **当前位置**：起始地点
🌄 这是一片宁静的古老石坛，四周环绕着刻满符文的巨石柱，微风中隐约传来远古贤者的低语。
*/

/*CHOICES:
你想做什么？
1. 🔥 学习元素魔法
2. 🗺️ 探索周边区域  
3. 📜 查看世界传说
4. 🎲 进行属性检定
*/`,
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
            </div>
          </div>
        </>
      )}
    </Card>
  );
};

export default RoleplayChat;

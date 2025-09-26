import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from 'modern-ui-components';
import type { RoleplayChatProps, Message, WorldTemplate } from './types';
import { STRUCTURED_CONTENT_PATTERNS, ASSESSMENT_PATTERNS } from './constants';
import { streamChatRequest } from '../../utils/api';
import api from '../../utils/api';
import GameLayout from './GameLayout';
import WorldSelector from './WorldSelector';
import MessageList from './MessageList';
import MessageInput from './MessageInput';

// 流式内容缓存器
class StreamingBuffer {
  private buffer: string = '';
  private structuredContent: Record<string, string> = {};
  private onContentUpdate: (content: Record<string, string>) => void;
  private onBufferUpdate: (buffer: string) => void;

  constructor(
    onContentUpdate: (content: Record<string, string>) => void,
    onBufferUpdate: (buffer: string) => void
  ) {
    this.onContentUpdate = onContentUpdate;
    this.onBufferUpdate = onBufferUpdate;
  }

  // 添加新内容到buffer
  addContent(newContent: string) {
    this.buffer += newContent;
    this.checkForStructuredContent();
    this.onBufferUpdate(this.buffer);
  }

  // 检查并提取结构化内容
  private checkForStructuredContent() {
    let hasUpdates = false;
    const patterns = [
      { key: 'dialogue', regex: STRUCTURED_CONTENT_PATTERNS.dialogue },
      { key: 'status', regex: STRUCTURED_CONTENT_PATTERNS.status },
      { key: 'world', regex: STRUCTURED_CONTENT_PATTERNS.world },
      { key: 'quests', regex: STRUCTURED_CONTENT_PATTERNS.quests },
      { key: 'choices', regex: STRUCTURED_CONTENT_PATTERNS.choices },
      { key: 'assessment', regex: STRUCTURED_CONTENT_PATTERNS.assessment }
    ];

    // 检查评估JSON模式
    const assessmentMatch = this.buffer.match(ASSESSMENT_PATTERNS.full);
    if (assessmentMatch) {
      try {
        const assessmentData = JSON.parse(assessmentMatch[1]);
        this.structuredContent.assessment = JSON.stringify(assessmentData, null, 2);
        this.buffer = this.buffer.replace(assessmentMatch[0], '');
        hasUpdates = true;
        console.log('✅ [StreamingBuffer] 提取到评估内容:', assessmentData);
      } catch (e) {
        console.warn('❌ [StreamingBuffer] 解析评估JSON失败:', e);
      }
    }

    // 检查其他结构化内容
    for (const pattern of patterns) {
      const matches = [...this.buffer.matchAll(pattern.regex)];
      for (const match of matches) {
        const content = match[1].trim();
        if (content && content !== this.structuredContent[pattern.key]) {
          this.structuredContent[pattern.key] = content;
          this.buffer = this.buffer.replace(match[0], '');
          hasUpdates = true;
          console.log(`✅ [StreamingBuffer] 提取到${pattern.key}内容:`, content);
        }
      }
    }

    if (hasUpdates) {
      this.onContentUpdate({ ...this.structuredContent });
    }
  }

  // 获取当前buffer内容
  getBuffer(): string {
    return this.buffer;
  }

  // 获取结构化内容
  getStructuredContent(): Record<string, string> {
    return { ...this.structuredContent };
  }

  // 清空buffer和结构化内容
  clear() {
    this.buffer = '';
    this.structuredContent = {};
    this.onBufferUpdate('');
    this.onContentUpdate({});
  }

  // 完成流式输入，处理剩余内容
  finalize() {
    // 如果buffer中还有内容，将其作为dialogue处理
    if (this.buffer.trim() && !this.structuredContent.dialogue) {
      this.structuredContent.dialogue = this.buffer.trim();
      this.buffer = '';
      this.onContentUpdate({ ...this.structuredContent });
      this.onBufferUpdate('');
    }
  }
}

const RoleplayChat: React.FC<RoleplayChatProps> = () => {
  // 基础状态
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [selectedWorld, setSelectedWorld] = useState<string>('');
  const [sessionId, setSessionId] = useState<string>('');
  const [isFreeActionMode, setIsFreeActionMode] = useState(false);
  const [visibleMessageCount, setVisibleMessageCount] = useState(10);
  const [skillsState, setSkillsState] = useState<any>(null);
  const [worldState, setWorldState] = useState<any>(null);
  
  // 世界模板相关状态
  const [worldTemplates, setWorldTemplates] = useState<WorldTemplate[]>([]);
  const [isLoadingWorlds, setIsLoadingWorlds] = useState(false);

  // 流式输入相关状态
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamingBuffer, setStreamingBuffer] = useState('');
  const [structuredContent, setStructuredContent] = useState<Record<string, string>>({});
  const [currentStreamingMessage, setCurrentStreamingMessage] = useState<Message | null>(null);

  // 引用
  const streamingBufferRef = useRef<StreamingBuffer | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 初始化流式buffer
  useEffect(() => {
    streamingBufferRef.current = new StreamingBuffer(
      (content) => setStructuredContent(content),
      (buffer) => setStreamingBuffer(buffer)
    );
  }, []);

  // 获取世界模板列表
  const fetchWorldTemplates = useCallback(async () => {
    setIsLoadingWorlds(true);
    try {
      const response = await api.get('/roleplay/worlds');
      if (response.data.success) {
        setWorldTemplates(response.data.data || []);
      } else {
        console.error('获取世界模板失败:', response.data.message);
      }
    } catch (error) {
      console.error('获取世界模板失败:', error);
    } finally {
      setIsLoadingWorlds(false);
    }
  }, []);

  // 组件挂载时获取世界模板
  useEffect(() => {
    fetchWorldTemplates();
  }, [fetchWorldTemplates]);

  // 滚动到底部
  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  // 当消息更新时滚动到底部
  useEffect(() => {
    if (messages.length > 0) {
      scrollToBottom();
    }
  }, [messages, scrollToBottom]);

  // 发送消息
  const handleSendMessage = useCallback(async (messageText?: string) => {
    const textToSend = messageText || inputMessage.trim();
    if (!textToSend || isLoading || isStreaming) return;
    
    // 检查是否有有效的会话ID
    if (!sessionId) {
      console.error('❌ [RoleplayChat] 没有有效的会话ID，无法发送消息');
      return;
    }

    // 创建用户消息
    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: textToSend,
      timestamp: new Date(),
      worldType: selectedWorld
    };

    // 添加用户消息到列表
    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setIsLoading(true);
    setIsStreaming(true);

    // 清空之前的流式内容
    streamingBufferRef.current?.clear();
    setStreamingBuffer('');
    setStructuredContent({});

    // 创建AI消息占位符
    const aiMessage: Message = {
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      worldType: selectedWorld,
      roleName: getAIRoleName(selectedWorld)
    };

    setCurrentStreamingMessage(aiMessage);

    // 创建取消控制器
    abortControllerRef.current = new AbortController();

    try {
      await streamChatRequest(
        {
          message: textToSend,
          sessionId: sessionId,
          worldType: selectedWorld
        },
        (content: string) => {
          // 添加内容到buffer
          streamingBufferRef.current?.addContent(content);
          
          // 更新AI消息内容
          setCurrentStreamingMessage(prev => {
            if (!prev) return null;
            return {
              ...prev,
              content: prev.content + content
            };
          });
        },
        () => {
          // 流式输入完成
          console.log('✅ [RoleplayChat] 流式输入完成');
          streamingBufferRef.current?.finalize();
          
          // 将最终消息添加到消息列表
          setMessages(prev => {
            const finalMessage = {
              ...aiMessage,
              content: currentStreamingMessage?.content || '',
              isStructured: Object.keys(structuredContent).length > 0
            };
            return [...prev, finalMessage];
          });

          // 清理状态
          setCurrentStreamingMessage(null);
          setIsStreaming(false);
          setIsLoading(false);
        },
        abortControllerRef.current.signal
      );
    } catch (error) {
      console.error('❌ [RoleplayChat] 流式请求失败:', error);
      
      // 添加错误消息
      const errorMessage: Message = {
        id: (Date.now() + 2).toString(),
        role: 'assistant',
        content: '抱歉，发生了错误，请稍后重试。',
        timestamp: new Date(),
        worldType: selectedWorld,
        roleName: getAIRoleName(selectedWorld)
      };

      setMessages(prev => [...prev, errorMessage]);
      setCurrentStreamingMessage(null);
      setIsStreaming(false);
      setIsLoading(false);
    }
  }, [inputMessage, selectedWorld, sessionId, isFreeActionMode, isLoading, isStreaming, currentStreamingMessage, structuredContent]);

  // 处理选择项选择
  const handleChoiceSelect = useCallback((choice: string) => {
    if (isLoading || isStreaming) return;
    
    setInputMessage(choice);
    handleSendMessage(choice);
  }, [isLoading, isStreaming, handleSendMessage]);

  // 处理自由行动模式切换
  const handleFreeActionModeChange = useCallback((enabled: boolean) => {
    setIsFreeActionMode(enabled);
  }, []);

  // 获取AI角色名称
  const getAIRoleName = (worldType: string): string => {
    const roleNames = {
      fantasy_adventure: '游戏主持人',
      western_magic: '贤者向导',
      martial_arts: '江湖前辈',
      japanese_school: '校园向导',
      educational: '智慧导师'
    };
    return roleNames[worldType as keyof typeof roleNames] || '向导';
  };

  // 处理世界选择
  const handleWorldSelect = useCallback(async (worldId: string) => {
    setSelectedWorld(worldId);
    setMessages([]);
    setSessionId('');
    setSkillsState(null);
    setWorldState(null);
    streamingBufferRef.current?.clear();
    
    // 创建新的会话
    try {
      const response = await api.post('/roleplay/sessions', {
        worldId: worldId,
        godModeRules: null // 暂时不设置特殊规则
      });
      
      if (response.data.success) {
        const sessionData = response.data.data;
        setSessionId(sessionData.sessionId);
        console.log('✅ [RoleplayChat] 会话创建成功:', sessionData.sessionId);
      } else {
        console.error('❌ [RoleplayChat] 创建会话失败:', response.data.message);
      }
    } catch (error) {
      console.error('❌ [RoleplayChat] 创建会话失败:', error);
    }
  }, []);

  // 处理显示更多消息
  const handleShowMore = useCallback(() => {
    setVisibleMessageCount(prev => Math.min(prev + 10, messages.length));
  }, [messages.length]);

  // 处理显示所有消息
  const handleShowAll = useCallback(() => {
    setVisibleMessageCount(messages.length);
  }, [messages.length]);

  // 处理滚动到底部
  const handleScrollToBottom = useCallback(() => {
    scrollToBottom();
  }, [scrollToBottom]);

  // 组件卸载时清理
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, []);

  // 如果没有选择世界，显示世界选择器
  if (!selectedWorld) {
    return (
      <Card className="w-full">
        <CardHeader>
          <CardTitle className="text-center">选择你的冒险世界</CardTitle>
        </CardHeader>
        <CardContent>
          <WorldSelector
            worlds={worldTemplates}
            onWorldSelect={handleWorldSelect}
            isLoading={isLoadingWorlds}
          />
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="w-full h-full flex flex-col">
      {/* 消息列表 */}
      <div className="flex-1 overflow-hidden">
        <MessageList
          messages={messages}
          visibleMessageCount={visibleMessageCount}
          isLoading={isLoading}
          selectedWorld={selectedWorld}
          isDark={false} // 这里应该从主题状态获取
          onShowMore={handleShowMore}
          onShowAll={handleShowAll}
          onScrollToBottom={handleScrollToBottom}
          hasMoreMessages={false}
          totalMessageCount={messages.length}
          onChoiceSelect={handleChoiceSelect}
          onFreeActionModeChange={handleFreeActionModeChange}
          skillsState={skillsState}
          worldState={worldState}
          structuredContent={structuredContent}
        />

        {/* 当前流式消息 */}
        {currentStreamingMessage && (
          <div className="mb-4">
            <GameLayout
              isDark={false}
              isLoading={isStreaming}
              onChoiceSelect={handleChoiceSelect}
              onFreeActionModeChange={handleFreeActionModeChange}
              skillsState={skillsState}
              structuredContent={structuredContent}
            />
          </div>
        )}

        {/* 流式buffer显示 */}
        {streamingBuffer && (
          <div className="mb-4 p-4 bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 rounded-lg border border-blue-200 dark:border-blue-700">
            <div className="flex items-center gap-2 mb-2">
              <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse"></div>
              <span className="text-sm font-semibold text-blue-600 dark:text-blue-400">正在生成内容...</span>
            </div>
            <div className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap max-h-[200px] overflow-y-auto">
              {streamingBuffer}
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* 消息输入 */}
      <div className="mt-4">
        <MessageInput
          inputMessage={inputMessage}
          onInputChange={setInputMessage}
          onSendMessage={() => handleSendMessage()}
          isLoading={isLoading || isStreaming}
          isFreeActionMode={isFreeActionMode}
          onFreeActionModeChange={handleFreeActionModeChange}
        />
      </div>
    </div>
  );
};

export default RoleplayChat;

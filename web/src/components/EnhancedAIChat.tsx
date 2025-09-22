import React, { useState, useEffect, useRef } from 'react';
import { Button, Card, CardHeader, CardTitle, CardContent } from 'modern-ui-components';
import { Send, Bot, User, Loader2, AlertCircle, RefreshCw, History, MessageSquare, Trash2, Clock } from 'lucide-react';

interface ChatMessage {
  id: string;
  type: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  isComplete?: boolean;
  sequenceNumber?: number;
}

interface ChatSession {
  sessionId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
}

interface EnhancedAIChatProps {
  isAuthenticated: boolean;
  onAuthFailure?: () => void;
}

interface ChatHistoryMessage {
  role: 'user' | 'assistant';
  content: string;
}

const EnhancedAIChat: React.FC<EnhancedAIChatProps> = ({ isAuthenticated, onAuthFailure }) => {
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: '1',
      type: 'assistant',
      content: '你好！我是你的AI助手。很高兴见到你！有什么我可以帮助你的吗？',
      timestamp: new Date(),
      isComplete: true,
    }
  ]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sessionId, setSessionId] = useState(() => `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`);
  const [showHistory, setShowHistory] = useState(false);
  const [chatSessions, setChatSessions] = useState<ChatSession[]>([]);
  const [loadingSessions, setLoadingSessions] = useState(false);
  const [currentSessionTitle, setCurrentSessionTitle] = useState('新对话');
  
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // 自动滚动到底部
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // 调整文本框高度
  const adjustTextareaHeight = () => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 120)}px`;
    }
  };

  useEffect(() => {
    adjustTextareaHeight();
  }, [inputMessage]);

  // 加载聊天会话列表
  const loadChatSessions = async () => {
    console.log('loadChatSessions 被调用');
    console.log('isAuthenticated:', isAuthenticated);
    console.log('localStorage token:', localStorage.getItem('accessToken') ? '存在' : '不存在');
    
    if (!isAuthenticated) {
      console.log('用户未认证，跳过加载会话列表');
      return;
    }
    
    console.log('开始加载会话列表...');
    setLoadingSessions(true);
    try {
      const token = localStorage.getItem('accessToken');
      console.log('使用token:', token ? token.substring(0, 20) + '...' : '无token');
      
      const response = await fetch('http://localhost:8080/api/chat/session/list', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      console.log('会话列表响应状态:', response.status);
      if (response.ok) {
        console.log('i am here');
        const data = await response.json();
        console.log('会话列表响应数据:', JSON.stringify(data, null, 2));
        console.log('data.data:', data.data);
        console.log('data.data.sessions:', data.data?.sessions);
        
        const sessions = data.data?.sessions || [];
        console.log('解析后的会话数组:', sessions);
        console.log('会话数组类型:', Array.isArray(sessions));
        console.log('会话数组长度:', sessions.length);
        
        setChatSessions(sessions);
        console.log('设置会话列表完成，数量:', sessions.length);
      } else {
        const errorText = await response.text();
        console.error('加载会话列表失败:', response.status, errorText);
      }
    } catch (error) {
      console.error('加载会话列表异常:', error);
    } finally {
      setLoadingSessions(false);
      console.log('会话列表加载完成');
    }
  };

  // 加载指定会话的消息
  const loadSessionMessages = async (targetSessionId: string) => {
    if (!isAuthenticated) return;
    
    try {
      const token = localStorage.getItem('accessToken');
      const response = await fetch(`http://localhost:8080/api/chat/session/${targetSessionId}/messages`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        console.log('会话消息响应:', data); // 调试日志
        const sessionMessages = data.data?.messages || [];
        
        // 转换为前端消息格式
        const convertedMessages: ChatMessage[] = sessionMessages.map((msg: any) => ({
          id: msg.id.toString(),
          type: msg.role === 'user' ? 'user' : 'assistant',
          content: msg.content,
          timestamp: new Date(msg.createdAt),
          isComplete: true,
          sequenceNumber: msg.sequenceNumber,
        }));

        setMessages(convertedMessages);
        setSessionId(targetSessionId);
        
        // 更新当前会话标题
        const currentSession = chatSessions.find(s => s.sessionId === targetSessionId);
        setCurrentSessionTitle(currentSession?.title || '对话');
        
        setShowHistory(false);
      } else {
        console.error('加载会话消息失败:', response.status);
      }
    } catch (error) {
      console.error('加载会话消息失败:', error);
    }
  };

  // 删除会话
  const deleteSession = async (targetSessionId: string) => {
    if (!isAuthenticated) return;
    
    try {
      const token = localStorage.getItem('accessToken');
      const response = await fetch(`http://localhost:8080/api/chat/session/${targetSessionId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.ok) {
        // 重新加载会话列表
        loadChatSessions();
        
        // 如果删除的是当前会话，创建新会话
        if (targetSessionId === sessionId) {
          startNewChat();
        }
      } else {
        console.error('删除会话失败:', response.status);
      }
    } catch (error) {
      console.error('删除会话失败:', error);
    }
  };

  // 开始新对话
  const startNewChat = () => {
    const newSessionId = `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    setSessionId(newSessionId);
    setCurrentSessionTitle('新对话');
    setMessages([{
      id: Date.now().toString(),
      type: 'assistant',
      content: '你好！我是你的AI助手。很高兴见到你！有什么我可以帮助你的吗？',
      timestamp: new Date(),
      isComplete: true,
    }]);
    setError(null);
    setShowHistory(false);
  };

  // 发送消息的逻辑保持不变
  const sendMessage = async () => {
    if (!inputMessage.trim() || !isAuthenticated || isLoading) return;

    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      type: 'user',
      content: inputMessage.trim(),
      timestamp: new Date(),
      isComplete: true,
    };

    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setIsLoading(true);
    setError(null);

    // 创建AI消息容器
    const aiMessageId = (Date.now() + 1).toString();
    const aiMessage: ChatMessage = {
      id: aiMessageId,
      type: 'assistant',
      content: '',
      timestamp: new Date(),
      isComplete: false,
    };

    setMessages(prev => [...prev, aiMessage]);

    let streamCompleted = false;
    let hasReceivedData = false;

    try {
      const token = localStorage.getItem('accessToken');
      
      // 构建对话历史 (排除当前消息和初始欢迎消息)
      const history: ChatHistoryMessage[] = messages
        .filter(msg => msg.id !== '1' && msg.id !== userMessage.id && msg.isComplete) // 排除欢迎消息、当前消息和未完成的消息
        .map(msg => ({
          role: msg.type === 'user' ? 'user' as const : 'assistant' as const,
          content: msg.content
        }));

      const response = await fetch('http://localhost:8080/api/chat/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          message: userMessage.content,
          sessionId: sessionId,
          history: history,
          systemPrompt: '你是一个智能助手，可以帮助用户解答各种问题。请用友好、专业的语气回答。记住之前的对话内容，保持对话的连贯性。',
        }),
      });

      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          throw new Error('认证失败，请重新登录');
        }
        throw new Error(`请求失败: ${response.status}`);
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      if (reader) {
        try {
          while (true) {
            const { done, value } = await reader.read();
            if (done) {
              streamCompleted = true;
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
                      msg.id === aiMessageId
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
            streamCompleted = true;
          } else {
            throw readerError;
          }
        }
      }

      // 标记AI消息为完成
      setMessages(prev => prev.map(msg =>
        msg.id === aiMessageId
          ? { ...msg, isComplete: true }
          : msg
      ));

      // 如果是新会话的第一条消息，更新标题
      if (messages.length <= 1) { // 只有欢迎消息
        setCurrentSessionTitle(userMessage.content.length > 15 ? 
          userMessage.content.substring(0, 15) + '...' : 
          userMessage.content
        );
      }

    } catch (error) {
      console.error('发送消息失败:', error);
      
      // 只有在没有接收到任何数据或者不是流完成的情况下才显示错误
      if (!hasReceivedData && !streamCompleted) {
        const errorMessage = error instanceof Error ? error.message : '发送消息失败';
        setError(errorMessage);

        // 更新AI消息为错误状态
        setMessages(prev => prev.map(msg =>
          msg.id === aiMessageId
            ? { ...msg, content: '抱歉，我暂时无法回复您的消息。请稍后重试。', isComplete: true }
            : msg
        ));
      } else {
        // 如果已经接收到数据，只是标记为完成
        setMessages(prev => prev.map(msg =>
          msg.id === aiMessageId
            ? { ...msg, isComplete: true }
            : msg
        ));
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  // 格式化时间
  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffMins < 60) return `${diffMins}分钟前`;
    if (diffHours < 24) return `${diffHours}小时前`;
    if (diffDays < 7) return `${diffDays}天前`;
    return date.toLocaleDateString();
  };

  return (
    <Card className="w-full h-[600px] flex flex-col">
      <CardHeader className="flex-shrink-0">
        <div className="flex items-center justify-between">
          <div className="flex flex-col">
            <CardTitle className="flex items-center gap-2">
              <Bot className="w-5 h-5 text-primary" />
              AI 智能助手
            </CardTitle>
            <p className="text-xs text-muted-foreground mt-1">
              {currentSessionTitle} • 会话ID: {sessionId.split('_')[2]}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button
              onClick={() => {
                if (showHistory) {
                  setShowHistory(false);
                } else {
                  loadChatSessions();
                  setShowHistory(true);
                }
              }}
              variant="outline"
              size="sm"
              className="flex items-center gap-2"
            >
              <History className="w-4 h-4" />
              历史对话
            </Button>
            <Button
              onClick={startNewChat}
              variant="outline"
              size="sm"
              className="flex items-center gap-2"
            >
              <MessageSquare className="w-4 h-4" />
              新对话
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent className="flex-1 flex flex-col p-0">
        {showHistory ? (
          // 历史会话列表
          <div className="flex-1 p-4">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold">历史对话</h3>
              <Button
                onClick={() => setShowHistory(false)}
                variant="ghost"
                size="sm"
              >
                返回
              </Button>
            </div>
            
            {loadingSessions ? (
              <div className="flex items-center justify-center h-32">
                <Loader2 className="w-6 h-6 animate-spin" />
              </div>
            ) : chatSessions.length === 0 ? (
              <div className="text-center text-muted-foreground py-8">
                暂无历史对话
                <div className="text-xs mt-2">
                  调试信息: chatSessions = {JSON.stringify(chatSessions)}
                </div>
              </div>
            ) : (
              <div className="space-y-2">
                <div className="text-xs text-muted-foreground mb-2">
                  显示 {chatSessions.length} 个会话
                </div>
                {chatSessions.map((session) => (
                  <div
                    key={session.sessionId}
                    className="flex items-center justify-between p-3 border rounded-lg hover:bg-muted/50 cursor-pointer"
                    onClick={() => loadSessionMessages(session.sessionId)}
                  >
                    <div className="flex-1 min-w-0">
                      <div className="font-medium truncate">{session.title}</div>
                      <div className="text-sm text-muted-foreground flex items-center gap-2">
                        <Clock className="w-3 h-3" />
                        {formatTime(session.updatedAt)}
                        <span>•</span>
                        <span>{session.messageCount} 条消息</span>
                      </div>
                    </div>
                    <Button
                      onClick={(e) => {
                        e.stopPropagation();
                        deleteSession(session.sessionId);
                      }}
                      variant="ghost"
                      size="sm"
                      className="text-destructive hover:text-destructive"
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>
        ) : (
          // 消息列表
          <>
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {messages.map((message) => (
                <div
                  key={message.id}
                  className={`flex ${message.type === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-[80%] rounded-2xl px-4 py-3 ${
                      message.type === 'user'
                        ? 'bg-primary text-primary-foreground'
                        : message.isComplete
                          ? 'bg-muted text-muted-foreground'
                          : 'bg-muted/50 text-muted-foreground border border-primary/20'
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <div className={`w-6 h-6 rounded-full flex items-center justify-center flex-shrink-0 ${
                        message.type === 'user'
                          ? 'bg-primary-foreground/20'
                          : 'bg-primary/20'
                      }`}>
                        {message.type === 'user' ? (
                          <User className="w-3 h-3" />
                        ) : (
                          <Bot className="w-3 h-3" />
                        )}
                      </div>
                      <div className="flex-1">
                        <div className="text-sm leading-relaxed whitespace-pre-wrap">
                          {message.content}
                          {!message.isComplete && (
                            <span className="inline-block w-2 h-4 bg-primary animate-pulse ml-1" />
                          )}
                        </div>
                        <div className="text-xs opacity-70 mt-1">
                          {message.timestamp.toLocaleTimeString()}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>

            {/* 输入区域 */}
            <div className="border-t p-4 space-y-3">
              {error && (
                <div className="flex items-center gap-2 text-sm text-destructive bg-destructive/10 p-2 rounded-lg">
                  <AlertCircle className="w-4 h-4" />
                  {error}
                </div>
              )}

              <div className="flex gap-3">
                <div className="flex-1 relative">
                  <textarea
                    ref={textareaRef}
                    value={inputMessage}
                    onChange={(e) => setInputMessage(e.target.value)}
                    onKeyPress={handleKeyPress}
                    placeholder={isAuthenticated ? "输入你的消息..." : "请先登录以开始对话"}
                    disabled={!isAuthenticated || isLoading}
                    className="w-full px-4 py-3 border rounded-xl resize-none min-h-[44px] max-h-[120px] focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary disabled:opacity-50 disabled:cursor-not-allowed"
                    rows={1}
                  />
                </div>
                <Button
                  onClick={sendMessage}
                  disabled={!inputMessage.trim() || !isAuthenticated || isLoading}
                  size="lg"
                  className="px-6"
                >
                  {isLoading ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <Send className="w-4 h-4" />
                  )}
                </Button>
              </div>

              {!isAuthenticated && (
                <p className="text-sm text-muted-foreground text-center">
                  请先登录以使用AI对话功能
                </p>
              )}
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
};

export default EnhancedAIChat;


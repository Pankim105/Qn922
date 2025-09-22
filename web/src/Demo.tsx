import React, { useState, useEffect, useRef } from 'react'
import ReactDOM from 'react-dom/client'
import { 
  Button, 
  Card, 
  CardHeader, 
  CardTitle, 
  CardDescription, 
  CardContent, 
  ModernInput,
  SearchInput,
  PasswordInput,
  Accordion,
  AccordionItem,
  AccordionTrigger,
  AccordionContent,
  WaterGlassCard,
  WaterGlassCardHeader,
  WaterGlassCardTitle,
  WaterGlassCardDescription,
  WaterGlassCardContent,
  FlipCard,
  Modal,
  ModalHeader,
  ModalTitle,
  ModalDescription,
  ModalBody,
  ModalFooter,
  ModalClose,
  ModalSteps,
  Table,
  THead,
  TBody,
  TR,
  TH,
  TD,
  TFoot,
  TableEmpty,
  TableLoading,
  TablePagination,
  DataTable,
  Form,
  FormField,
  FormLabel,
  FormControl,
  FormHelpText,
  ModernSelect,
  NativeSelect,
  ModernTextarea,
  RichTextarea,
  ThemeSwitcher
} from 'modern-ui-components'
import { 
  Heart, 
  Star, 
  Settings, 
  User,
  Send, 
  Bot, 
  Loader2, 
  AlertCircle, 
  RefreshCw, 
  History, 
  MessageSquare, 
  Trash2, 
  Clock
} from 'lucide-react'

// 聊天消息接口
interface ChatMessage {
  id: string;
  type: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  isComplete?: boolean;
  sequenceNumber?: number;
}

// 聊天会话接口
interface ChatSession {
  sessionId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
}

// 聊天历史消息接口
interface ChatHistoryMessage {
  role: 'user' | 'assistant';
  content: string;
}

// 增强版AI聊天组件
const EnhancedAIChat: React.FC<{ isAuthenticated: boolean }> = ({ isAuthenticated }) => {
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
    if (!isAuthenticated) return;
    
    setLoadingSessions(true);
    try {
      const token = localStorage.getItem('accessToken');
      const response = await fetch('http://localhost:8080/api/chat/session/list', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        setChatSessions(data.sessions || []);
      } else {
        console.error('加载会话列表失败:', response.status);
      }
    } catch (error) {
      console.error('加载会话列表失败:', error);
    } finally {
      setLoadingSessions(false);
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
        const sessionMessages = data.messages || [];
        
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

  // 发送消息
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
        .filter(msg => msg.id !== '1' && msg.id !== userMessage.id && msg.isComplete)
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
      if (messages.length <= 1) {
        setCurrentSessionTitle(userMessage.content.length > 15 ? 
          userMessage.content.substring(0, 15) + '...' : 
          userMessage.content
        );
      }

    } catch (error) {
      console.error('发送消息失败:', error);
      
      if (!hasReceivedData && !streamCompleted) {
        const errorMessage = error instanceof Error ? error.message : '发送消息失败';
        setError(errorMessage);

        setMessages(prev => prev.map(msg =>
          msg.id === aiMessageId
            ? { ...msg, content: '抱歉，我暂时无法回复您的消息。请稍后重试。', isComplete: true }
            : msg
        ));
      } else {
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
              </div>
            ) : (
              <div className="space-y-2">
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

// AI聊天演示组件
const AIChatDemo = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  // 检查认证状态
  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    setIsAuthenticated(!!token);
  }, []);

  return (
    <Card>
      <CardHeader>
        <CardTitle>AI 聊天组件 (Enhanced AIChat)</CardTitle>
        <CardDescription>支持对话历史、会话管理和恢复功能的AI聊天组件</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="mb-4 p-3 bg-muted/50 rounded-lg">
          <p className="text-sm text-muted-foreground">
            <strong>功能特性：</strong>
          </p>
          <ul className="text-sm text-muted-foreground mt-2 space-y-1">
            <li>• 支持查看和恢复历史对话</li>
            <li>• 会话管理（新建、删除、切换）</li>
            <li>• 实时流式AI回复</li>
            <li>• 数据库持久化存储</li>
            <li>• JWT认证保护</li>
          </ul>
          <div className="mt-3 flex items-center gap-2">
            <div className={`w-2 h-2 rounded-full ${isAuthenticated ? 'bg-green-500' : 'bg-red-500'}`} />
            <span className="text-xs">
              认证状态: {isAuthenticated ? '已登录' : '未登录'}
            </span>
          </div>
        </div>
        <EnhancedAIChat isAuthenticated={isAuthenticated} />
      </CardContent>
    </Card>
  );
};

// 主题管理组件
const ThemeManager = () => {
  const [theme, setTheme] = useState('green')
  const [isDark, setIsDark] = useState(false)

  React.useEffect(() => {
    document.documentElement.className = `${isDark ? 'dark' : ''} theme-${theme}`
  }, [theme, isDark])

  return (
    <ThemeSwitcher
      className="fixed top-4 right-4"
      currentTheme={theme}
      isDarkMode={isDark}
      onThemeChange={setTheme}
      onDarkModeChange={setIsDark}
      minimizable={true}
      defaultMinimized={false}
    />
  )
}

// 按钮演示组件
const ButtonDemo = () => {
  const [loading, setLoading] = useState(false)

  const handleLoading = () => {
    setLoading(true)
    setTimeout(() => setLoading(false), 2000)
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>按钮组件 (Button)</CardTitle>
        <CardDescription>多种样式、尺寸和状态的按钮组件</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* 基础变体 */}
        <div>
          <h4 className="text-sm font-medium mb-3">基础变体</h4>
          <div className="flex flex-wrap gap-2">
            <Button variant="solid">Solid</Button>
            <Button variant="outline">Outline</Button>
            <Button variant="ghost">Ghost</Button>
            <Button variant="glass">Glass</Button>
            <Button variant="destructive">Destructive</Button>
            <Button variant="link">Link</Button>
          </div>
        </div>

        {/* 尺寸变体 */}
        <div>
          <h4 className="text-sm font-medium mb-3">尺寸变体</h4>
          <div className="flex flex-wrap items-center gap-2">
            <Button size="sm">Small</Button>
            <Button size="md">Medium</Button>
            <Button size="lg">Large</Button>
            <Button size="xl">Extra Large</Button>
            <Button size="icon">
              <Heart className="h-4 w-4" />
            </Button>
          </div>
        </div>

        {/* 形状变体 */}
        <div>
          <h4 className="text-sm font-medium mb-3">形状变体</h4>
          <div className="flex flex-wrap gap-2">
            <Button shape="rounded">Rounded</Button>
            <Button shape="pill">Pill</Button>
            <Button shape="square">Square</Button>
          </div>
        </div>

        {/* 状态变体 */}
        <div>
          <h4 className="text-sm font-medium mb-3">状态变体</h4>
          <div className="flex flex-wrap gap-2">
            <Button>Default</Button>
            <Button loading>Loading</Button>
            <Button state="active">Active</Button>
            <Button disabled>Disabled</Button>
          </div>
        </div>

        {/* 预设配置 */}
        <div>
          <h4 className="text-sm font-medium mb-3">预设配置</h4>
          <div className="flex flex-wrap gap-2">
            <Button preset="primary">Primary</Button>
            <Button preset="secondary">Secondary</Button>
            <Button preset="danger">Danger</Button>
            <Button preset="iconButton">
              <Settings className="h-4 w-4" />
            </Button>
            <Button preset="linkButton">Link Button</Button>
            <Button preset="navButton">Nav Button</Button>
          </div>
        </div>

        {/* 交互演示 */}
        <div>
          <h4 className="text-sm font-medium mb-3">交互演示</h4>
          <div className="flex flex-wrap gap-2">
            <Button onClick={handleLoading} loading={loading}>
              {loading ? 'Loading...' : 'Click to Load'}
            </Button>
            <Button asChild>
              <a href="#cards">As Link</a>
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

// 卡片演示组件
const CardDemo = () => {
  return (
    <Card>
      <CardHeader>
        <CardTitle>卡片组件 (Card)</CardTitle>
        <CardDescription>多种样式和效果的卡片容器组件</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* 基础变体 */}
        <div>
          <h4 className="text-sm font-medium mb-3">基础变体</h4>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <Card variant="default" size="sm">
              <CardHeader>
                <CardTitle className="text-sm">Default</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground">基础卡片样式</p>
              </CardContent>
            </Card>

            <Card variant="elevated" size="sm">
              <CardHeader>
                <CardTitle className="text-sm">Elevated</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground">突出卡片样式</p>
              </CardContent>
            </Card>

            <Card variant="primary" size="sm">
              <CardHeader>
                <CardTitle className="text-sm">Primary</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground">主题色卡片</p>
              </CardContent>
            </Card>

            <Card variant="glass" size="sm">
              <CardHeader>
                <CardTitle className="text-sm">Glass</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground">玻璃效果卡片</p>
              </CardContent>
            </Card>

            <Card variant="gradient" size="sm">
              <CardHeader>
                <CardTitle className="text-sm">Gradient</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground">渐变卡片</p>
              </CardContent>
            </Card>

            <Card variant="outline" size="sm">
              <CardHeader>
                <CardTitle className="text-sm">Outline</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground">轮廓卡片</p>
              </CardContent>
            </Card>
          </div>
        </div>

        {/* 交互效果 */}
        <div>
          <h4 className="text-sm font-medium mb-3">交互效果</h4>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <Card preset="product">
              <CardHeader>
                <CardTitle className="text-sm">Product Card</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground">悬停浮动效果</p>
              </CardContent>
            </Card>

            <Card preset="feature">
              <CardHeader>
                <CardTitle className="text-sm">Feature Card</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground">发光效果</p>
              </CardContent>
            </Card>

            <Card preset="media">
              <CardHeader>
                <CardTitle className="text-sm">Media Card</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground">倾斜效果</p>
              </CardContent>
            </Card>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

// 输入框演示组件
const InputDemo = () => {
  const [value, setValue] = useState('')
  const [password, setPassword] = useState('')

  return (
    <Card>
      <CardHeader>
        <CardTitle>输入框组件 (Input)</CardTitle>
        <CardDescription>多种样式和功能的输入框组件</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* 基础变体 */}
        <div>
          <h4 className="text-sm font-medium mb-3">基础变体</h4>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <ModernInput
              variant="default"
              label="默认输入框"
              placeholder="请输入内容"
              value={value}
              onChange={(e) => setValue(e.target.value)}
            />
            <ModernInput
              variant="filled"
              label="填充输入框"
              placeholder="请输入内容"
            />
            <ModernInput
              variant="outline"
              label="轮廓输入框"
              placeholder="请输入内容"
            />
            <ModernInput
              variant="ghost"
              label="幽灵输入框"
              placeholder="请输入内容"
            />
            <ModernInput
              variant="glass"
              label="玻璃输入框"
              placeholder="请输入内容"
            />
            <ModernInput
              variant="primary"
              label="主题色输入框"
              placeholder="请输入内容"
            />
          </div>
        </div>

        {/* 尺寸变体 */}
        <div>
          <h4 className="text-sm font-medium mb-3">尺寸变体</h4>
          <div className="space-y-3">
            <ModernInput size="sm" placeholder="小尺寸输入框" />
            <ModernInput size="md" placeholder="中尺寸输入框" />
            <ModernInput size="lg" placeholder="大尺寸输入框" />
          </div>
        </div>

        {/* 状态变体 */}
        <div>
          <h4 className="text-sm font-medium mb-3">状态变体</h4>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <ModernInput
              label="错误状态"
              error="请输入有效内容"
              placeholder="错误输入框"
            />
            <ModernInput
              label="成功状态"
              success="输入正确"
              placeholder="成功输入框"
            />
            <ModernInput
              label="警告状态"
              state="warning"
              helperText="请注意输入格式"
              placeholder="警告输入框"
            />
            <ModernInput
              label="加载状态"
              loading
              placeholder="加载中..."
            />
          </div>
        </div>

        {/* 功能变体 */}
        <div>
          <h4 className="text-sm font-medium mb-3">功能变体</h4>
          <div className="space-y-3">
            <SearchInput placeholder="搜索内容..." />
            <PasswordInput
              label="密码输入框"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="请输入密码"
            />
            <ModernInput
              label="可清除输入框"
              clearable
              value={value}
              onChange={(e) => setValue(e.target.value)}
              onClear={() => setValue('')}
              placeholder="输入后可清除"
            />
            <ModernInput
              label="带图标输入框"
              leftIcon={<User className="h-4 w-4" />}
              rightIcon={<Settings className="h-4 w-4" />}
              placeholder="带图标的输入框"
            />
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

// 水波纹玻璃卡片演示
const WaterGlassCardDemo = () => {
  return (
    <Card>
      <CardHeader>
        <CardTitle>水波纹玻璃卡片 (WaterGlassCard)</CardTitle>
        <CardDescription>具有水波纹效果和玻璃拟态的卡片组件</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <WaterGlassCard
            variant="default"
            size="md"
            themeColor="#3b82f6"
            className="h-48"
          >
            <WaterGlassCardHeader>
              <WaterGlassCardTitle>默认样式</WaterGlassCardTitle>
              <WaterGlassCardDescription>
                具有水波纹效果的玻璃卡片
              </WaterGlassCardDescription>
            </WaterGlassCardHeader>
            <WaterGlassCardContent>
              <p className="text-sm text-white/90">
                移动鼠标查看水波纹效果
              </p>
            </WaterGlassCardContent>
          </WaterGlassCard>

          <WaterGlassCard
            variant="gradient"
            size="md"
            themeColor="#10b981"
            className="h-48"
          >
            <WaterGlassCardHeader>
              <WaterGlassCardTitle>渐变样式</WaterGlassCardTitle>
              <WaterGlassCardDescription>
                渐变背景的玻璃卡片
              </WaterGlassCardDescription>
            </WaterGlassCardHeader>
            <WaterGlassCardContent>
              <p className="text-sm text-white/90">
                绿色主题渐变效果
              </p>
            </WaterGlassCardContent>
          </WaterGlassCard>

          <WaterGlassCard
            variant="primary"
            size="md"
            themeColor="#f59e0b"
            className="h-48"
          >
            <WaterGlassCardHeader>
              <WaterGlassCardTitle>主题色样式</WaterGlassCardTitle>
              <WaterGlassCardDescription>
                橙色主题的玻璃卡片
              </WaterGlassCardDescription>
            </WaterGlassCardHeader>
            <WaterGlassCardContent>
              <p className="text-sm text-white/90">
                橙色主题水波纹效果
              </p>
            </WaterGlassCardContent>
          </WaterGlassCard>
        </div>
      </CardContent>
    </Card>
  )
}

// 手风琴演示组件
const AccordionDemo = () => {
  return (
    <Card>
      <CardHeader>
        <CardTitle>手风琴组件 (Accordion)</CardTitle>
        <CardDescription>可折叠展开的内容容器组件</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* 单选模式 */}
        <div>
          <h4 className="text-sm font-medium mb-3">单选模式</h4>
          <Accordion type="single" collapsible defaultValue="item-1">
            <AccordionItem value="item-1">
              <AccordionTrigger>什么是Modern UI Components？</AccordionTrigger>
              <AccordionContent>
                Modern UI Components是一个基于React + TypeScript + Tailwind CSS构建的现代化UI组件库，
                提供丰富的组件变体、主题系统和视觉效果。
              </AccordionContent>
            </AccordionItem>
            <AccordionItem value="item-2">
              <AccordionTrigger>支持哪些主题？</AccordionTrigger>
              <AccordionContent>
                组件库内置5种预设主题：绿色、橙色、紫色、蓝色、玫瑰色，
                每种主题都支持明暗模式切换。
              </AccordionContent>
            </AccordionItem>
            <AccordionItem value="item-3">
              <AccordionTrigger>如何快速开始？</AccordionTrigger>
              <AccordionContent>
                安装依赖后，导入所需组件即可使用。所有组件都提供了丰富的变体和预设配置，
                可以快速构建现代化的用户界面。
              </AccordionContent>
            </AccordionItem>
          </Accordion>
        </div>

        {/* 多选模式 */}
        <div>
          <h4 className="text-sm font-medium mb-3">多选模式</h4>
          <Accordion type="multiple" defaultValue={["feature-1", "feature-2"]}>
            <AccordionItem value="feature-1">
              <AccordionTrigger>丰富的组件系统</AccordionTrigger>
              <AccordionContent>
                包含Button、Card、Input、Accordion、WaterGlassCard、FlipCard等核心组件，
                每个组件都支持多种样式变体和功能配置。
              </AccordionContent>
            </AccordionItem>
            <AccordionItem value="feature-2">
              <AccordionTrigger>视觉效果</AccordionTrigger>
              <AccordionContent>
                提供水波纹效果、玻璃拟态、3D翻转等现代化视觉效果，
                所有效果都支持主题色响应和自定义配置。
              </AccordionContent>
            </AccordionItem>
            <AccordionItem value="feature-3">
              <AccordionTrigger>类型安全</AccordionTrigger>
              <AccordionContent>
                完整的TypeScript类型定义，提供良好的开发体验和代码提示，
                确保类型安全和减少运行时错误。
              </AccordionContent>
            </AccordionItem>
            <AccordionItem value="feature-4">
              <AccordionTrigger>无障碍支持</AccordionTrigger>
              <AccordionContent>
                基于最佳实践的无障碍设计，支持键盘导航、屏幕阅读器，
                确保所有用户都能良好使用。
              </AccordionContent>
            </AccordionItem>
          </Accordion>
        </div>
      </CardContent>
    </Card>
  )
}

// 翻转卡片演示
const FlipCardDemo = () => {
  return (
    <Card>
      <CardHeader>
        <CardTitle>翻转卡片 (FlipCard)</CardTitle>
        <CardDescription>支持悬停和点击触发的3D翻转卡片</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <FlipCard
            trigger="hover"
            direction="horizontal"
            width={280}
            height={200}
            frontContent={
              <Card className="h-full">
                <CardHeader>
                  <CardTitle className="text-center">悬停翻转</CardTitle>
                </CardHeader>
                <CardContent className="text-center">
                  <Heart className="h-12 w-12 mx-auto text-red-500" />
                  <p className="text-sm text-muted-foreground mt-2">
                    悬停查看背面
                  </p>
                </CardContent>
              </Card>
            }
            backContent={
              <Card className="h-full">
                <CardHeader>
                  <CardTitle className="text-center">背面内容</CardTitle>
                </CardHeader>
                <CardContent className="text-center">
                  <Star className="h-12 w-12 mx-auto text-yellow-500" />
                  <p className="text-sm text-muted-foreground mt-2">
                    这是卡片的背面
                  </p>
                </CardContent>
              </Card>
            }
          />

          <FlipCard
            trigger="click"
            direction="vertical"
            width={280}
            height={200}
            frontContent={
              <Card className="h-full">
                <CardHeader>
                  <CardTitle className="text-center">点击翻转</CardTitle>
                </CardHeader>
                <CardContent className="text-center">
                  <Settings className="h-12 w-12 mx-auto text-blue-500" />
                  <p className="text-sm text-muted-foreground mt-2">
                    点击查看背面
                  </p>
                </CardContent>
              </Card>
            }
            backContent={
              <Card className="h-full">
                <CardHeader>
                  <CardTitle className="text-center">垂直翻转</CardTitle>
                </CardHeader>
                <CardContent className="text-center">
                  <User className="h-12 w-12 mx-auto text-green-500" />
                  <p className="text-sm text-muted-foreground mt-2">
                    垂直方向翻转效果
                  </p>
                </CardContent>
              </Card>
            }
          />

          <FlipCard
            trigger="hover"
            direction="horizontal"
            duration={300}
            width={280}
            height={200}
            frontContent={
              <Card className="h-full">
                <CardHeader>
                  <CardTitle className="text-center">快速翻转</CardTitle>
                </CardHeader>
                <CardContent className="text-center">
                  <div className="h-12 w-12 mx-auto bg-gradient-to-r from-purple-500 to-pink-500 rounded-full" />
                  <p className="text-sm text-muted-foreground mt-2">
                    300ms 快速翻转
                  </p>
                </CardContent>
              </Card>
            }
            backContent={
              <Card className="h-full">
                <CardHeader>
                  <CardTitle className="text-center">快速效果</CardTitle>
                </CardHeader>
                <CardContent className="text-center">
                  <div className="h-12 w-12 mx-auto bg-gradient-to-r from-blue-500 to-cyan-500 rounded-full" />
                  <p className="text-sm text-muted-foreground mt-2">
                    快速翻转动画
                  </p>
                </CardContent>
              </Card>
            }
          />
        </div>
      </CardContent>
    </Card>
  )
}

// 模态框演示
const ModalDemo = () => {
  const [open, setOpen] = useState(false)
  const [step, setStep] = useState(1)
  const total = 3
  return (
    <Card>
      <CardHeader>
        <CardTitle>模态框组件 (Modal)</CardTitle>
        <CardDescription>支持预设、玻璃风格、可拖拽/可缩放与多步流程</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex gap-2">
          <Button onClick={() => { setStep(1); setOpen(true) }}>打开默认</Button>
          <Button variant="outline" onClick={() => { setStep(1); setOpen(true) }}>Outline</Button>
          <Button onClick={() => { setStep(1); setOpen(true) }} preset="primary">Primary</Button>
        </div>
        <div className="flex gap-2">
          <Button onClick={() => { setStep(1); setOpen(true) }}>多步流程</Button>
          <Button onClick={() => { setStep(1); setOpen(true) }}>拖拽/缩放</Button>
        </div>
      </CardContent>
      <Modal open={open} onOpenChange={setOpen} preset="glass" draggable resizable>
        <ModalHeader>
          <div className="flex items-center justify-between">
            <div>
              <ModalTitle>创建项目</ModalTitle>
              <ModalDescription>通过 3 步完成创建</ModalDescription>
            </div>
            <ModalClose />
          </div>
          <ModalSteps current={step} total={total} />
        </ModalHeader>
        <ModalBody>
          {step === 1 && <p className="text-sm text-muted-foreground">步骤一：填写基本信息</p>}
          {step === 2 && <p className="text-sm text-muted-foreground">步骤二：配置参数</p>}
          {step === 3 && <p className="text-sm text-muted-foreground">步骤三：确认并创建</p>}
        </ModalBody>
        <ModalFooter>
          <Button variant="ghost" onClick={() => setOpen(false)}>取消</Button>
          <div className="flex-1" />
          {step > 1 && (
            <Button variant="outline" onClick={() => setStep(step - 1)}>上一步</Button>
          )}
          {step < total && (
            <Button onClick={() => setStep(step + 1)}>下一步</Button>
          )}
          {step === total && (
            <Button onClick={() => setOpen(false)}>完成</Button>
          )}
        </ModalFooter>
      </Modal>
    </Card>
  )
}

// 表格演示
const TableDemo = () => {
  const [sort, setSort] = useState<{ key: 'name' | 'count' | null; direction: 'asc' | 'desc' | null }>({ key: null, direction: null })
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const total = 42
  const loading = false
  const empty = false

  const data = Array.from({ length: pageSize }).map((_, i) => {
    const id = (page - 1) * pageSize + i + 1
    return { id, name: `项目 ${id}`, count: Math.floor(Math.random() * 100), status: id % 2 ? '启用' : '停用' }
  })

  const toggleSort = (key: 'name' | 'count') => {
    setSort((prev) => {
      if (prev.key !== key) return { key, direction: 'asc' }
      if (prev.direction === 'asc') return { key, direction: 'desc' }
      return { key: null, direction: null }
    })
  }

  const sortedData = React.useMemo(() => {
    if (!sort.key || !sort.direction) return data
    const arr = [...data]
    arr.sort((a, b) => {
      const dir = sort.direction === 'asc' ? 1 : -1
      if (sort.key === 'count') return ((a.count as number) - (b.count as number)) * dir
      return String(a.name).localeCompare(String(b.name)) * dir
    })
    return arr
  }, [data, sort])

  const sumCount = React.useMemo(() => sortedData.reduce((s, r) => s + r.count, 0), [sortedData])

  return (
    <Card>
      <CardHeader>
        <CardTitle>表格组件 (Table)</CardTitle>
        <CardDescription>排序、分页、空/加载态示例</CardDescription>
      </CardHeader>
      <CardContent className="space-y-2">
        <Table
          variant="bordered"
          rounded="xl"
          containerProps={{ shadow: 'md', maxHeight: 'sm' }}
        >
          <THead sticky>
            <TR>
              <TH
                sortable
                sortDirection={sort.key === 'name' ? sort.direction : null}
                onSort={() => toggleSort('name')}
              >
                名称
              </TH>
              <TH
                sortable
                sortDirection={sort.key === 'count' ? sort.direction : null}
                onSort={() => toggleSort('count')}
                align="right"
              >
                数量
              </TH>
              <TH>状态</TH>
            </TR>
          </THead>
          <TBody>
            <TableLoading colSpan={3} />
            <TableEmpty colSpan={3} />
            {sortedData.map((row) => (
              <TR key={row.id} selected={row.id % 7 === 0} variant="striped">
                <TD>{row.name}</TD>
                <TD align="right">{row.count}</TD>
                <TD>{row.status}</TD>
              </TR>
            ))}
          </TBody>
          <TFoot>
            <TR>
              <TD>合计</TD>
              <TD align="right">{sumCount}</TD>
              <TD />
            </TR>
          </TFoot>
        </Table>
        <TablePagination
          page={page}
          pageSize={pageSize}
          total={total}
          onPageChange={setPage}
          onPageSizeChange={setPageSize}
        />
      </CardContent>
    </Card>
  )
}
// DataTable 演示
const DataTableDemo = () => {
  type Row = { id: number; name: string; count: number; status: string }
  const rows: Row[] = Array.from({ length: 57 }).map((_, i) => ({
    id: i + 1,
    name: `项目 ${i + 1}`,
    count: Math.floor(Math.random() * 100),
    status: (i + 1) % 3 === 0 ? '停用' : '启用',
  }))
  const columns = [
    { key: 'name', title: '名称', sortable: true },
    { key: 'count', title: '数量', sortable: true, align: 'right' as const },
    { key: 'status', title: '状态' },
  ]
  const [selected, setSelected] = React.useState<string[]>([])
  return (
    <Card>
      <CardHeader>
        <CardTitle>数据表格 (DataTable)</CardTitle>
        <CardDescription>内置搜索、排序、分页、选择等能力</CardDescription>
      </CardHeader>
      <CardContent>
        <DataTable
          columns={columns}
          dataSource={rows}
          selectable
          selectedRowKeys={selected}
          onSelectChange={(keys: string[]) => setSelected(keys)}
          searchable
          pagination={{ current: 1, pageSize: 10, total: rows.length, showTotal: true, showSizeChanger: true, onChange: () => {} }}
          variant="bordered"
          rounded="xl"
          size="md"
          caption="演示用数据"
        />
      </CardContent>
    </Card>
  )
}


// 选择框演示组件
const SelectDemo = () => {
  const [value, setValue] = useState('')
  const [nativeValue, setNativeValue] = useState('')
  
  const options = [
    { value: 'react', label: 'React', description: '用于构建用户界面的 JavaScript 库' },
    { value: 'vue', label: 'Vue.js', description: '渐进式 JavaScript 框架' },
    { value: 'angular', label: 'Angular', description: '平台和框架，用于构建单页客户端应用程序' },
    { value: 'svelte', label: 'Svelte', description: '编译时优化的前端框架' },
    { value: 'solid', label: 'Solid', description: '声明式、高效且灵活的用于构建用户界面的 JavaScript 库' },
  ]

  return (
    <Card className="overflow-visible">
      <CardHeader>
        <CardTitle>选择框组件 (Select)</CardTitle>
        <CardDescription>现代化的选择框，支持搜索、清除等功能</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6 overflow-visible">
        {/* 基础变体 */}
        <div>
          <h4 className="text-sm font-medium mb-3">基础变体</h4>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <ModernSelect
              variant="default"
              label="默认选择框"
              placeholder="请选择框架"
              options={options}
              value={value}
              onValueChange={setValue}
            />
            <ModernSelect
              variant="filled"
              label="填充选择框"
              placeholder="请选择框架"
              options={options}
            />
            <ModernSelect
              variant="outline"
              label="轮廓选择框"
              placeholder="请选择框架"
              options={options}
            />
            <ModernSelect
              variant="ghost"
              label="幽灵选择框"
              placeholder="请选择框架"
              options={options}
            />
          </div>
        </div>

        {/* 功能演示 */}
        <div>
          <h4 className="text-sm font-medium mb-3">功能演示</h4>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <ModernSelect
              label="可搜索选择框"
              searchable
              placeholder="搜索并选择"
              options={options}
            />
            <ModernSelect
              label="可清除选择框"
              clearable
              placeholder="可清除的选择"
              options={options}
              value={value}
              onValueChange={setValue}
              onClear={() => setValue('')}
            />
            <ModernSelect
              label="错误状态"
              error="请选择一个框架"
              placeholder="错误状态"
              options={options}
            />
            <ModernSelect
              label="成功状态"
              success="选择正确"
              placeholder="成功状态"
              options={options}
              value="react"
            />
          </div>
        </div>

        {/* 原生选择框 */}
        <div>
          <h4 className="text-sm font-medium mb-3">原生选择框</h4>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <NativeSelect
              label="原生选择框"
              options={options}
              value={nativeValue}
              onChange={(e) => setNativeValue(e.target.value)}
            />
            <NativeSelect
              label="加载状态"
              loading
              options={options}
            />
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

// 文本区域演示组件
const TextareaDemo = () => {
  const [value, setValue] = useState('')
  const [richValue, setRichValue] = useState('')

  return (
    <Card>
      <CardHeader>
        <CardTitle>文本区域组件 (Textarea)</CardTitle>
        <CardDescription>支持自动调整高度、字数统计等功能的文本区域</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* 基础变体 */}
        <div>
          <h4 className="text-sm font-medium mb-3">基础变体</h4>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <ModernTextarea
              variant="default"
              label="默认文本区域"
              placeholder="请输入内容..."
              value={value}
              onChange={(e) => setValue(e.target.value)}
            />
            <ModernTextarea
              variant="filled"
              label="填充文本区域"
              placeholder="请输入内容..."
            />
            <ModernTextarea
              variant="outline"
              label="轮廓文本区域"
              placeholder="请输入内容..."
            />
            <ModernTextarea
              variant="ghost"
              label="幽灵文本区域"
              placeholder="请输入内容..."
            />
          </div>
        </div>

        {/* 功能演示 */}
        <div>
          <h4 className="text-sm font-medium mb-3">功能演示</h4>
          <div className="space-y-4">
            <ModernTextarea
              label="自动调整高度"
              placeholder="输入内容时高度会自动调整..."
              autoResize
              minRows={2}
              maxRows={6}
            />
            <ModernTextarea
              label="字数统计"
              placeholder="输入内容查看字数..."
              showWordCount
              maxLength={100}
              value={value}
              onChange={(e) => setValue(e.target.value)}
            />
            <ModernTextarea
              label="可复制和展开"
              placeholder="右上角有复制和展开按钮..."
              copyable
              expandable
              value="这是一些示例文本，你可以复制它或者展开查看更大的编辑区域。"
            />
          </div>
        </div>

        {/* 状态演示 */}
        <div>
          <h4 className="text-sm font-medium mb-3">状态演示</h4>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <ModernTextarea
              label="错误状态"
              error="内容不能为空"
              placeholder="错误状态文本区域"
            />
            <ModernTextarea
              label="成功状态"
              success="内容格式正确"
              placeholder="成功状态文本区域"
              value="正确的内容格式"
            />
          </div>
        </div>

        {/* 富文本编辑器 */}
        <div>
          <h4 className="text-sm font-medium mb-3">富文本编辑器</h4>
          <RichTextarea
            label="Markdown 编辑器"
            toolbar
            markdownMode
            preview
            placeholder="支持 Markdown 语法的富文本编辑器..."
            value={richValue}
            onChange={(e) => setRichValue(e.target.value)}
            helperText="使用工具栏按钮快速插入格式，或者点击预览按钮查看效果"
          />
        </div>
      </CardContent>
    </Card>
  )
}

// 表单演示（搭配 ModernInput）
const FormDemo = () => {
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [framework, setFramework] = useState('')
  const [description, setDescription] = useState('')
  
  const frameworkOptions = [
    { value: 'react', label: 'React' },
    { value: 'vue', label: 'Vue.js' },
    { value: 'angular', label: 'Angular' },
    { value: 'svelte', label: 'Svelte' },
  ]

  return (
    <Card className="overflow-visible">
      <CardHeader>
        <CardTitle>表单组件 (Form)</CardTitle>
        <CardDescription>密度与方向可配置的表单结构组件</CardDescription>
      </CardHeader>
      <CardContent className="overflow-visible">
        <Form preset="horizontal" onSubmit={(e) => e.preventDefault()}>
          <FormField>
            <FormLabel required>用户名</FormLabel>
            <FormControl>
              <ModernInput value={username} onChange={(e) => setUsername(e.target.value)} placeholder="请输入用户名" />
            </FormControl>
            <FormHelpText state={!username ? 'warning' : 'success'}>
              {!username ? '请填写用户名' : '看起来不错'}
            </FormHelpText>
          </FormField>
          <FormField>
            <FormLabel>邮箱</FormLabel>
            <FormControl>
              <ModernInput type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="name@example.com" />
            </FormControl>
            <FormHelpText>用于找回密码与通知</FormHelpText>
          </FormField>
          <FormField>
            <FormLabel>技术框架</FormLabel>
            <FormControl>
              <ModernSelect
                options={frameworkOptions}
                value={framework}
                onValueChange={setFramework}
                placeholder="选择你喜欢的框架"
                searchable
                clearable
                onClear={() => setFramework('')}
              />
            </FormControl>
            <FormHelpText>选择你最熟悉的前端框架</FormHelpText>
          </FormField>
          <FormField>
            <FormLabel>自我介绍</FormLabel>
            <FormControl>
              <ModernTextarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="简单介绍一下自己..."
                autoResize
                showWordCount
                maxLength={500}
                minRows={3}
                maxRows={8}
              />
            </FormControl>
            <FormHelpText>简单描述一下你的技能和经验</FormHelpText>
          </FormField>
        </Form>
      </CardContent>
    </Card>
  )
}

// 主演示应用
const Demo = () => {
  return (
    <div className="min-h-screen bg-background text-foreground">

      
      {/* 主题切换器 */}
      <ThemeManager />
      
      {/* 主内容 */}
      <div className="container mx-auto px-4 py-8 space-y-8">
        {/* 标题区域 */}
        <div className="text-center space-y-4">
          <h1 className="text-4xl font-bold bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
            Modern UI Components
          </h1>
          <p className="text-xl text-muted-foreground">
            基于 React + TypeScript + Tailwind CSS 的现代化UI组件库
          </p>
          <div className="flex justify-center gap-2">
            <Button preset="primary">开始使用</Button>
            <Button variant="outline">查看文档</Button>
          </div>
        </div>

        {/* 组件演示区域 */}
        <div className="space-y-8">
          <AIChatDemo />
          <ButtonDemo />
          <CardDemo />
          <InputDemo />
          <SelectDemo />
          <TextareaDemo />
          <AccordionDemo />
          <WaterGlassCardDemo />
          <FlipCardDemo />
          <ModalDemo />
          <TableDemo />
          <DataTableDemo />
          <FormDemo />
        </div>

        {/* 页脚 */}
        <footer className="text-center py-8 text-muted-foreground">
          <p>© 2024 Modern UI Components. 基于 MIT 许可证开源。</p>
        </footer>
      </div>
    </div>
  )
}

export default Demo


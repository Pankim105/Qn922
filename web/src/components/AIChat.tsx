import React, { useState, useEffect, useRef } from 'react';
import { Button, Card, CardHeader, CardTitle, CardContent } from 'modern-ui-components';
import { Send, Bot, User, Loader2, AlertCircle, RefreshCw } from 'lucide-react';

interface ChatMessage {
  id: string;
  type: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  isComplete?: boolean;
}

interface AIChatProps {
  isAuthenticated: boolean;
}

interface ChatHistoryMessage {
  role: 'user' | 'assistant';
  content: string;
}

const AIChat: React.FC<AIChatProps> = ({ isAuthenticated }) => {
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
  const [sessionId] = useState(() => `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`);
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

  const clearChat = async () => {
    try {
      const token = localStorage.getItem('accessToken');
      
      // 调用后端清空会话历史
      if (token) {
        await fetch(`http://localhost:8080/api/chat/session/${sessionId}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${token}`,
          },
        });
      }
    } catch (error) {
      console.warn('清空服务端会话历史失败:', error);
    }
    
    // 清空前端消息
    setMessages([{
      id: Date.now().toString(),
      type: 'assistant',
      content: '聊天记录已清空。你好！我是你的AI助手。很高兴见到你！有什么我可以帮助你的吗？',
      timestamp: new Date(),
      isComplete: true,
    }]);
    setError(null);
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
              会话ID: {sessionId.split('_')[2]} • 支持上下文记忆
            </p>
          </div>
          <Button
            onClick={clearChat}
            variant="outline"
            size="sm"
            className="flex items-center gap-2"
          >
            <RefreshCw className="w-4 h-4" />
            清空对话
          </Button>
        </div>
      </CardHeader>

      <CardContent className="flex-1 flex flex-col p-0">
        {/* 消息列表 */}
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
      </CardContent>
    </Card>
  );
};

export default AIChat;

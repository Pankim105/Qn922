import React, { useState, useEffect } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Button } from 'modern-ui-components';
import { Play, Square, RefreshCw, Download, MessageSquare, Trash2 } from 'lucide-react';
import GameLayout from './GameLayout';
import type { Message } from './types';

// 模拟StreamingBuffer类
class StreamingBuffer {
  private onContentUpdate: (content: Record<string, string>) => void;
  private onBufferUpdate: (buffer: string) => void;
  private content: Record<string, string> = {};
  private buffer: string = '';

  constructor(
    onContentUpdate: (content: Record<string, string>) => void,
    onBufferUpdate: (buffer: string) => void
  ) {
    this.onContentUpdate = onContentUpdate;
    this.onBufferUpdate = onBufferUpdate;
  }

  addChunk(chunk: string) {
    this.buffer += chunk;
    this.parseContent();
    this.onBufferUpdate(this.buffer);
  }

  private parseContent() {
    // 解析结构化内容 - 支持新的方括号格式和旧的星号格式
    const dialogueMatch = this.buffer.match(/(?:\[DIALOGUE\]|\\*DIALOGUE:?)\s*([\s\S]*?)(?:\[\/DIALOGUE\]|\\*\/)/);
    const worldMatch = this.buffer.match(/(?:\[WORLD\]|\\*WORLD:?)\s*([\s\S]*?)(?:\[\/WORLD\]|\\*\/)/);
    const questsMatch = this.buffer.match(/(?:\[QUESTS\]|\\*QUESTS:?)\s*([\s\S]*?)(?:\[\/QUESTS\]|\\*\/)/);
    const choicesMatch = this.buffer.match(/(?:\[CHOICES\]|\\*CHOICES:?)\s*([\s\S]*?)(?:\[\/CHOICES\]|\\*\/)/);
    const assessmentMatch = this.buffer.match(/§([\s\S]*?)§/);

    const newContent: Record<string, string> = {};
    
    if (dialogueMatch) newContent.dialogue = dialogueMatch[1].trim();
    if (worldMatch) newContent.world = worldMatch[1].trim();
    if (questsMatch) newContent.quests = questsMatch[1].trim();
    if (choicesMatch) newContent.choices = choicesMatch[1].trim();
    if (assessmentMatch) newContent.assessment = assessmentMatch[1].trim();

    this.content = newContent;
    this.onContentUpdate(this.content);
  }

  clear() {
    this.buffer = '';
    this.content = {};
    this.onContentUpdate(this.content);
    this.onBufferUpdate('');
  }

  finalize() {
    this.parseContent();
    this.onBufferUpdate('');
  }

  // 获取当前 buffer 内容
  getBuffer() {
    return this.buffer;
  }

  // 获取最终的结构化内容
  getFinalStructuredContent() {
    return { ...this.content };
  }
}

const RoleplayTestDemo: React.FC = () => {
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamingBuffer, setStreamingBuffer] = useState('');
  const [structuredContent, setStructuredContent] = useState<Record<string, string>>({});
  const [skillsState, setSkillsState] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isAssessing, setIsAssessing] = useState(false);
  const [streamingBufferRef, setStreamingBufferRef] = useState<StreamingBuffer | null>(null);
  const [eventSource, setEventSource] = useState<EventSource | null>(null);
  
  // 新增：消息历史记录
  const [messages, setMessages] = useState<Message[]>([]);
  const [currentMessageIndex, setCurrentMessageIndex] = useState<number>(-1);
  const [conversationCount, setConversationCount] = useState(0);

  // 获取最近三轮对话的消息
  const getRecentMessages = (allMessages: Message[]) => {
    // 计算三轮对话：每轮包含一个用户消息和一个AI消息
    // 所以三轮对话最多包含6条消息（3个用户消息 + 3个AI消息）
    const maxMessages = 6;
    
    const result = allMessages.length <= maxMessages 
      ? allMessages 
      : allMessages.slice(-maxMessages);
    
    // 计算已完成的对话轮数（完整的用户+AI消息对）
    const completedRounds = Math.floor(allMessages.length / 2);
    // 当前轮次应该是已完成的轮数，而不是下一轮
    const currentRound = Math.min(completedRounds, 5);
    
    // 调试对话轮数计算
    console.log('🔍 [getRecentMessages] 对话轮数分析:', {
      totalMessages: allMessages.length,
      maxMessages: maxMessages,
      returnedMessages: result.length,
      completedRounds: completedRounds,
      currentRound: currentRound,
      messageRoles: result.map(msg => msg.role),
      calculation: `总消息数: ${allMessages.length}, 已完成轮数: ${completedRounds}, 当前轮次: ${currentRound}`
    });
    
    return result;
  };

  // 初始化流式buffer
  useEffect(() => {
    const buffer = new StreamingBuffer(
      (content) => setStructuredContent(content),
      (buffer) => setStreamingBuffer(buffer)
    );
    setStreamingBufferRef(buffer);
    return () => buffer.clear();
  }, []);

  // 模拟获取会话状态
  const fetchMockSessionState = () => {
    // 模拟从数据库获取的角色状态
    const mockSkillsState = {
      "gold": 0,
      "level": 1,
      "stats": {},
      "abilities": [],
      "inventory": [],
      "attributes": {
        "wisdom": 12,
        "charisma": 12,
        "strength": 12,
        "dexterity": 12,
        "constitution": 12,
        "intelligence": 12
      },
      "experience": 0,
      "profession": "private_detective",
      "skillLevels": {
        "deduction": 1,
        "surveillance": 1
      },
      "characterName": "panzijian",
      "selectedSkills": ["deduction", "surveillance"],
      "生命值": "100/100",
      "魔力值": "50/50"
    };
    
    setSkillsState(mockSkillsState);
    console.log('✅ [RoleplayTestDemo] 模拟获取会话状态成功:', mockSkillsState);
  };

  // 开始流式测试
  const startStreamTest = () => {
    if (isStreaming) return;

    console.log('🚀 [startStreamTest] 开始流式测试:', {
      currentMessagesCount: messages.length,
      conversationCount: conversationCount
    });

    setIsStreaming(true);
    setIsLoading(true);
    setStructuredContent({});
    if (streamingBufferRef) {
      streamingBufferRef.clear();
    }

    // 模拟获取会话状态
    fetchMockSessionState();

    // 创建EventSource连接
    const es = new EventSource('http://localhost:8080/api/roleplay/test/stream');
    setEventSource(es);

    es.onmessage = (event) => {
      if (event.data && streamingBufferRef) {
        streamingBufferRef.addChunk(event.data);
      }
    };

    es.addEventListener('heartbeat', (event) => {
      console.log('💓 收到心跳:', event.data);
    });

    es.addEventListener('complete', () => {
      console.log('✅ 流式响应完成');
      
      // 检查 streamingBufferRef 是否存在
      if (!streamingBufferRef) {
        console.error('❌ streamingBufferRef 为 null，无法保存消息');
        setIsStreaming(false);
        setIsLoading(false);
        setIsAssessing(false);
        es.close();
        setEventSource(null);
        return;
      }
      
      // 在 finalize 之前保存当前的 buffer 内容
      const currentBuffer = streamingBufferRef.getBuffer() || '';
      streamingBufferRef.finalize();
      
      // 获取最终的结构化内容并保存到消息历史
      const finalStructuredContent = streamingBufferRef.getFinalStructuredContent() || {};
      const newMessage: Message = {
        id: `test-${Date.now()}`,
        role: 'assistant',
        content: currentBuffer,
        timestamp: new Date(),
        worldType: 'modern_city',
        roleName: '警探',
        isStructured: Object.keys(finalStructuredContent).length > 0,
        structuredContent: finalStructuredContent
      };
      
      setMessages(prev => {
        const newMessages = [...prev, newMessage];
        setCurrentMessageIndex(newMessages.length - 1);
        console.log('✅ [对话完成] 消息已保存:', {
          newMessageId: newMessage.id,
          newMessageRole: newMessage.role,
          newMessageContent: newMessage.content.substring(0, 100) + '...',
          hasStructuredContent: Object.keys(finalStructuredContent).length > 0,
          structuredContentKeys: Object.keys(finalStructuredContent),
          totalMessages: newMessages.length,
          conversationCount: conversationCount + 1
        });
        return newMessages;
      });
      setConversationCount(prev => prev + 1);
      
      setIsStreaming(false);
      setIsLoading(false);
      setIsAssessing(false);
      es.close();
      setEventSource(null);
    });

    es.onerror = (error) => {
      console.error('❌ EventSource错误:', error);
      setIsStreaming(false);
      setIsLoading(false);
      setIsAssessing(false);
      es.close();
      setEventSource(null);
    };

    // 模拟评估过程
    setTimeout(() => {
      setIsAssessing(true);
    }, 2000);

    setTimeout(() => {
      setIsAssessing(false);
    }, 5000);
  };

  // 停止流式测试
  const stopStreamTest = () => {
    if (eventSource) {
      eventSource.close();
      setEventSource(null);
    }
    setIsStreaming(false);
    setIsLoading(false);
    setIsAssessing(false);
    if (streamingBufferRef) {
      streamingBufferRef.clear();
    }
  };

  // 重置测试
  const resetTest = () => {
    stopStreamTest();
    setStructuredContent({});
    setStreamingBuffer('');
    setSkillsState(null);
    setMessages([]);
    setCurrentMessageIndex(-1);
    setConversationCount(0);
  };

  // 开始第二次对话
  const startSecondConversation = () => {
    console.log('🔍 [startSecondConversation] 检查条件:', {
      isStreaming,
      messagesLength: messages.length,
      messages: messages.map(m => ({ id: m.id, role: m.role, content: m.content.substring(0, 50) + '...' }))
    });
    
    if (isStreaming || messages.length === 0) {
      console.log('❌ [startSecondConversation] 条件不满足，无法开始第二次对话');
      return;
    }
    
    console.log('✅ [startSecondConversation] 开始第二次对话');
    
    // 添加用户消息
    const userMessage: Message = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: '我想继续探索这个案件，请给我更多线索。',
      timestamp: new Date(),
      worldType: 'modern_city'
    };
    
    console.log('👤 [用户消息] 添加用户消息:', {
      messageId: userMessage.id,
      content: userMessage.content,
      currentMessagesCount: messages.length
    });
    
    setMessages(prev => [...prev, userMessage]);
    
    // 开始新的流式对话
    startStreamTest();
  };

  // 开始第三轮对话
  const startThirdConversation = () => {
    if (isStreaming || messages.length < 2) return;
    
    const userMessage: Message = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: '我需要更深入地调查这个案件，有什么新的发现吗？',
      timestamp: new Date(),
      worldType: 'modern_city'
    };
    
    setMessages(prev => [...prev, userMessage]);
    startStreamTest();
  };

  // 开始第四轮对话
  const startFourthConversation = () => {
    if (isStreaming || messages.length < 4) return;
    
    const userMessage: Message = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: '这个案件变得越来越复杂了，我需要你的专业建议。',
      timestamp: new Date(),
      worldType: 'modern_city'
    };
    
    setMessages(prev => [...prev, userMessage]);
    startStreamTest();
  };

  // 开始第五轮对话
  const startFifthConversation = () => {
    if (isStreaming || messages.length < 6) return;
    
    const userMessage: Message = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: '让我们总结一下到目前为止的所有发现，并制定下一步的行动计划。',
      timestamp: new Date(),
      worldType: 'modern_city'
    };
    
    setMessages(prev => [...prev, userMessage]);
    startStreamTest();
  };

  // 批量发送所有五轮对话
  const startAllFiveConversations = async () => {
    if (isStreaming) return;
    
    const conversationMessages = [
      '我想继续探索这个案件，请给我更多线索。',
      '我需要更深入地调查这个案件，有什么新的发现吗？',
      '这个案件变得越来越复杂了，我需要你的专业建议。',
      '让我们总结一下到目前为止的所有发现，并制定下一步的行动计划。'
    ];
    
    // 先开始第一次对话
    startStreamTest();
    
    // 等待第一次对话完成，然后依次发送后续对话
    const waitForCompletion = () => {
      return new Promise<void>((resolve) => {
        const checkCompletion = () => {
          if (!isStreaming && !isLoading) {
            resolve();
          } else {
            setTimeout(checkCompletion, 1000);
          }
        };
        checkCompletion();
      });
    };
    
    // 依次发送后续对话
    for (let i = 0; i < conversationMessages.length; i++) {
      await waitForCompletion();
      
      const userMessage: Message = {
        id: `user-${Date.now()}-${i}`,
        role: 'user',
        content: conversationMessages[i],
        timestamp: new Date(),
        worldType: 'modern_city'
      };
      
      setMessages(prev => [...prev, userMessage]);
      startStreamTest();
    }
  };

  // 查看历史消息
  const viewMessage = (index: number) => {
    if (index >= 0 && index < messages.length) {
      const message = messages[index];
      setCurrentMessageIndex(index);
      if (message.structuredContent) {
        setStructuredContent(message.structuredContent as Record<string, string>);
      }
    }
  };

  // 清除消息历史
  const clearMessageHistory = () => {
    setMessages([]);
    setCurrentMessageIndex(-1);
    setConversationCount(0);
    setStructuredContent({});
  };

  // 处理选择项选择
  const handleChoiceSelect = (choice: string) => {
    console.log('🎯 选择项被点击:', choice);
    // 这里可以添加选择项处理逻辑
  };

  // 处理自由行动模式切换
  const handleFreeActionModeChange = (enabled: boolean) => {
    console.log('✏️ 自由行动模式:', enabled ? '开启' : '关闭');
    // 这里可以添加自由行动模式处理逻辑
  };

  // 下载测试数据
  const downloadTestData = () => {
    const testData = {
      skillsState,
      structuredContent,
      streamingBuffer,
      messages,
      conversationCount,
      currentMessageIndex,
      timestamp: new Date().toISOString()
    };
    
    const blob = new Blob([JSON.stringify(testData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `roleplay-test-data-${Date.now()}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="w-full max-w-7xl mx-auto p-6 space-y-6">
      {/* 测试控制面板 */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Play className="w-5 h-5" />
            角色扮演测试演示
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-4 mb-4">
            <Button
              onClick={startStreamTest}
              disabled={isStreaming}
              className="flex items-center gap-2"
            >
              <Play className="w-4 h-4" />
              {isStreaming ? '流式传输中...' : '开始第一次对话'}
            </Button>
            
            <Button
              onClick={startSecondConversation}
              disabled={isStreaming || messages.length === 0}
              variant="outline"
              className="flex items-center gap-2"
            >
              <MessageSquare className="w-4 h-4" />
              开始第二次对话
            </Button>

            <Button
              onClick={startThirdConversation}
              disabled={isStreaming || messages.length < 2}
              variant="outline"
              className="flex items-center gap-2"
            >
              <MessageSquare className="w-4 h-4" />
              开始第三轮对话
            </Button>

            <Button
              onClick={startFourthConversation}
              disabled={isStreaming || messages.length < 4}
              variant="outline"
              className="flex items-center gap-2"
            >
              <MessageSquare className="w-4 h-4" />
              开始第四轮对话
            </Button>

            <Button
              onClick={startFifthConversation}
              disabled={isStreaming || messages.length < 6}
              variant="outline"
              className="flex items-center gap-2"
            >
              <MessageSquare className="w-4 h-4" />
              开始第五轮对话
            </Button>

            <Button
              onClick={startAllFiveConversations}
              disabled={isStreaming}
              variant="solid"
              className="flex items-center gap-2 bg-gradient-to-r from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600 text-white"
            >
              <MessageSquare className="w-4 h-4" />
              批量发送五轮对话
            </Button>
            
            <Button
              onClick={stopStreamTest}
              disabled={!isStreaming}
              variant="outline"
              className="flex items-center gap-2"
            >
              <Square className="w-4 h-4" />
              停止测试
            </Button>
            
            <Button
              onClick={resetTest}
              variant="outline"
              className="flex items-center gap-2"
            >
              <RefreshCw className="w-4 h-4" />
              重置
            </Button>

            <Button
              onClick={clearMessageHistory}
              disabled={messages.length === 0}
              variant="outline"
              className="flex items-center gap-2"
            >
              <Trash2 className="w-4 h-4" />
              清除历史
            </Button>
            
            <Button
              onClick={downloadTestData}
              variant="outline"
              className="flex items-center gap-2"
            >
              <Download className="w-4 h-4" />
              下载测试数据
            </Button>



            <Button
              onClick={() => {
                // 测试对话内容格式化
                const testDialogueContent = `#场景描述# 你站在一座古老警局的台阶前，锈迹斑斑的铁门在微风中轻轻摇晃。; #角色动作# 你的手不自觉地按在腰间的配枪上，指尖触到冰冷的金属。; #NPC对话# "Inspector Panzijian..." 一个沙哑的声音从阴影中传来，"欢迎来到'雾都案卷馆'。"`;
                
                // 动态导入formatDialogueContent函数
                import('./messageFormatter').then(({ formatDialogueContent }) => {
                  const formatted = formatDialogueContent(testDialogueContent);
                  console.log("对话内容格式化测试:");
                  console.log("原始内容:", testDialogueContent);
                  console.log("格式化结果:", formatted);
                  
                  // 创建一个临时div来显示结果
                  const tempDiv = document.createElement('div');
                  tempDiv.innerHTML = formatted;
                  tempDiv.style.position = 'fixed';
                  tempDiv.style.top = '50%';
                  tempDiv.style.left = '50%';
                  tempDiv.style.transform = 'translate(-50%, -50%)';
                  tempDiv.style.zIndex = '9999';
                  tempDiv.style.maxWidth = '80vw';
                  tempDiv.style.maxHeight = '80vh';
                  tempDiv.style.overflow = 'auto';
                  tempDiv.style.background = 'white';
                  tempDiv.style.padding = '20px';
                  tempDiv.style.border = '2px solid #ccc';
                  tempDiv.style.borderRadius = '8px';
                  tempDiv.style.boxShadow = '0 4px 20px rgba(0,0,0,0.3)';
                  
                  document.body.appendChild(tempDiv);
                  
                  // 5秒后移除
                  setTimeout(() => {
                    document.body.removeChild(tempDiv);
                  }, 5000);
                });
              }}
              variant="outline"
              className="flex items-center gap-2"
            >
              💬 测试对话格式化
            </Button>

            <Button
              onClick={() => {
                // 调试实际对话内容
                console.log("当前结构化内容:", structuredContent);
                if (structuredContent.dialogue) {
                  console.log("对话内容:", structuredContent.dialogue);
                  
                  // 创建一个临时div来显示原始内容和格式化结果
                  const tempDiv = document.createElement('div');
                  tempDiv.style.position = 'fixed';
                  tempDiv.style.top = '50%';
                  tempDiv.style.left = '50%';
                  tempDiv.style.transform = 'translate(-50%, -50%)';
                  tempDiv.style.zIndex = '9999';
                  tempDiv.style.maxWidth = '90vw';
                  tempDiv.style.maxHeight = '90vh';
                  tempDiv.style.overflow = 'auto';
                  tempDiv.style.background = 'white';
                  tempDiv.style.padding = '20px';
                  tempDiv.style.border = '2px solid #ccc';
                  tempDiv.style.borderRadius = '8px';
                  tempDiv.style.boxShadow = '0 4px 20px rgba(0,0,0,0.3)';
                  
                  // 显示原始内容
                  tempDiv.innerHTML = `
                    <h3 style="margin-bottom: 15px; color: #333;">原始对话内容:</h3>
                    <div style="background: #f5f5f5; padding: 10px; border-radius: 4px; margin-bottom: 20px; font-family: monospace; white-space: pre-wrap; border: 1px solid #ddd;">
${structuredContent.dialogue}
                    </div>
                    <h3 style="margin-bottom: 15px; color: #333;">格式化结果:</h3>
                    <div id="formatted-result" style="border: 1px solid #ddd; padding: 10px; border-radius: 4px;">
                      正在格式化...
                    </div>
                    <button onclick="this.parentElement.remove()" style="margin-top: 15px; padding: 8px 16px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;">
                      关闭
                    </button>
                  `;
                  
                  document.body.appendChild(tempDiv);
                  
                  // 动态导入formatDialogueContent函数并显示结果
                  import('./messageFormatter').then(({ formatDialogueContent }) => {
                    const formatted = formatDialogueContent(structuredContent.dialogue);
                    console.log("实际对话内容格式化结果:", formatted);
                    
                    const resultDiv = tempDiv.querySelector('#formatted-result');
                    if (resultDiv) {
                      resultDiv.innerHTML = formatted;
                    }
                  });
                } else {
                  alert("没有对话内容，请先点击'开始测试'按钮");
                }
              }}
              variant="outline"
              className="flex items-center gap-2"
            >
              🔍 调试实际对话内容
            </Button>

            <Button
              onClick={() => {
                // 测试高级材质渲染效果
                const testContent = `# 场景描述# 你站在一座古老警局的台阶前，锈迹斑斑的铁门在微风中轻轻摇晃。空气中弥漫着潮湿的尘埃与旧纸张的气息，远处传来钟楼的滴答声，仿佛在倒数着某个即将揭晓的秘密;# 角色动作# 你的手不自觉地按在腰间的配枪上，指尖触到冰冷的金属——这是你作为警探的第一天;# NPC对话# "Inspector Panzijian..." 一个沙哑的声音从阴影中传来，"欢迎来到'雾都案卷馆'。这里每一份档案都藏着一条命案的影子，而今晚，有一具尸体正在等你去发现。";# 环境变化# 突然，警局大厅的灯闪烁了一下，一束惨白的光线照在墙上的老式挂钟上——时间停在了凌晨3:17;# 声音效果# 你听见走廊深处传来一声重物坠地的闷响，紧接着，是锁链拖地的声音……;# NPC低语# "有人在下面。"那声音低语道，"但没人能活着上来。"`;
                
                // 动态导入formatDialogueContent函数
                import('./messageFormatter').then(({ formatDialogueContent }) => {
                  const formatted = formatDialogueContent(testContent);
                  console.log("高级材质渲染测试:");
                  console.log("原始内容:", testContent);
                  console.log("格式化结果:", formatted);
                  
                  // 创建一个临时div来显示结果
                  const tempDiv = document.createElement('div');
                  tempDiv.innerHTML = formatted;
                  tempDiv.style.position = 'fixed';
                  tempDiv.style.top = '50%';
                  tempDiv.style.left = '50%';
                  tempDiv.style.transform = 'translate(-50%, -50%)';
                  tempDiv.style.zIndex = '9999';
                  tempDiv.style.maxWidth = '90vw';
                  tempDiv.style.maxHeight = '90vh';
                  tempDiv.style.overflow = 'auto';
                  tempDiv.style.background = 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';
                  tempDiv.style.padding = '30px';
                  tempDiv.style.border = 'none';
                  tempDiv.style.borderRadius = '16px';
                  tempDiv.style.boxShadow = '0 20px 40px rgba(0,0,0,0.3)';
                  tempDiv.style.backdropFilter = 'blur(10px)';
                  
                  // 添加关闭按钮
                  const closeBtn = document.createElement('button');
                  closeBtn.innerHTML = '✕';
                  closeBtn.style.position = 'absolute';
                  closeBtn.style.top = '10px';
                  closeBtn.style.right = '15px';
                  closeBtn.style.background = 'rgba(255,255,255,0.2)';
                  closeBtn.style.border = 'none';
                  closeBtn.style.borderRadius = '50%';
                  closeBtn.style.width = '30px';
                  closeBtn.style.height = '30px';
                  closeBtn.style.color = 'white';
                  closeBtn.style.cursor = 'pointer';
                  closeBtn.style.fontSize = '16px';
                  closeBtn.style.fontWeight = 'bold';
                  closeBtn.onclick = () => document.body.removeChild(tempDiv);
                  
                  tempDiv.appendChild(closeBtn);
                  document.body.appendChild(tempDiv);
                  
                  // 10秒后自动移除
                  setTimeout(() => {
                    if (document.body.contains(tempDiv)) {
                      document.body.removeChild(tempDiv);
                    }
                  }, 10000);
                });
              }}
              variant="outline"
              className="flex items-center gap-2"
            >
              ✨ 测试高级材质渲染
            </Button>
          </div>

          {/* 状态指示器 */}
          <div className="flex flex-wrap gap-4 text-sm">
            <div className={`flex items-center gap-2 px-3 py-1 rounded-full ${
              isStreaming ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'
            }`}>
              <div className={`w-2 h-2 rounded-full ${
                isStreaming ? 'bg-green-500 animate-pulse' : 'bg-gray-400'
              }`} />
              流式传输
            </div>
            
            <div className={`flex items-center gap-2 px-3 py-1 rounded-full ${
              isLoading ? 'bg-blue-100 text-blue-800' : 'bg-gray-100 text-gray-600'
            }`}>
              <div className={`w-2 h-2 rounded-full ${
                isLoading ? 'bg-blue-500 animate-pulse' : 'bg-gray-400'
              }`} />
              加载中
            </div>
            
            <div className={`flex items-center gap-2 px-3 py-1 rounded-full ${
              isAssessing ? 'bg-orange-100 text-orange-800' : 'bg-gray-100 text-gray-600'
            }`}>
              <div className={`w-2 h-2 rounded-full ${
                isAssessing ? 'bg-orange-500 animate-pulse' : 'bg-gray-400'
              }`} />
              系统评估
            </div>
            
            <div className={`flex items-center gap-2 px-3 py-1 rounded-full ${
              skillsState ? 'bg-purple-100 text-purple-800' : 'bg-gray-100 text-gray-600'
            }`}>
              <div className={`w-2 h-2 rounded-full ${
                skillsState ? 'bg-purple-500' : 'bg-gray-400'
              }`} />
              角色状态
            </div>
            
            <div className={`flex items-center gap-2 px-3 py-1 rounded-full ${
              conversationCount > 0 ? 'bg-indigo-100 text-indigo-800' : 'bg-gray-100 text-gray-600'
            }`}>
              <div className={`w-2 h-2 rounded-full ${
                conversationCount > 0 ? 'bg-indigo-500' : 'bg-gray-400'
              }`} />
              对话次数: {conversationCount}
            </div>
            
            <div className={`flex items-center gap-2 px-3 py-1 rounded-full ${
              messages.length > 0 ? 'bg-cyan-100 text-cyan-800' : 'bg-gray-100 text-gray-600'
            }`}>
              <div className={`w-2 h-2 rounded-full ${
                messages.length > 0 ? 'bg-cyan-500' : 'bg-gray-400'
              }`} />
              消息数量: {messages.length}
            </div>
            
            <div className={`flex items-center gap-2 px-3 py-1 rounded-full ${
              conversationCount > 0 ? 'bg-emerald-100 text-emerald-800' : 'bg-gray-100 text-gray-600'
            }`}>
              <div className={`w-2 h-2 rounded-full ${
                conversationCount > 0 ? 'bg-emerald-500' : 'bg-gray-400'
              }`} />
              当前轮次: {Math.min(Math.floor(messages.length / 2), 5)}/5
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 消息历史记录 - 只显示最近三轮对话 */}
      {messages.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm flex items-center justify-between">
              <div className="flex items-center gap-2">
                <MessageSquare className="w-4 h-4" />
                消息历史记录 (最近三轮对话: {getRecentMessages(messages).length}/{messages.length} 条)
              </div>
              <Button
                onClick={() => setCurrentMessageIndex(-1)}
                variant="outline"
                size="sm"
                className="text-xs"
              >
                显示当前流式内容
              </Button>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {getRecentMessages(messages).map((message, index) => {
                // 计算在完整消息列表中的实际索引
                const actualIndex = messages.length - getRecentMessages(messages).length + index;
                
                // 只在第一次渲染时打印调试信息
                if (index === 0) {
                  console.log('🔍 [消息历史] 调试信息:', {
                    totalMessages: messages.length,
                    recentMessagesCount: getRecentMessages(messages).length,
                    firstMessageIndex: index,
                    firstMessageActualIndex: actualIndex,
                    firstMessageRole: message.role,
                    firstMessageId: message.id
                  });
                }
                
                return (
                <div
                  key={message.id}
                  className={`p-3 rounded-lg border-2 cursor-pointer transition-all duration-200 ${
                    currentMessageIndex === actualIndex
                      ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                      : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
                  }`}
                  onClick={() => viewMessage(actualIndex)}
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <span className={`px-2 py-1 rounded-full text-xs font-semibold ${
                        message.role === 'user'
                          ? 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200'
                          : 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                      }`}>
                        {message.role === 'user' ? '用户' : 'AI助手'}
                      </span>
                      <span className="text-xs text-gray-500 dark:text-gray-400">
                        {message.timestamp.toLocaleTimeString()}
                      </span>
                      {message.isStructured && (
                        <span className="px-2 py-1 rounded-full text-xs font-semibold bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200">
                          结构化
                        </span>
                      )}
                    </div>
                    <span className="text-xs text-gray-500 dark:text-gray-400">
                      #{actualIndex + 1}
                    </span>
                  </div>
                  <div className="text-sm text-gray-700 dark:text-gray-300">
                    {message.content.length > 100 
                      ? `${message.content.substring(0, 100)}...` 
                      : message.content
                    }
                  </div>
                  {message.structuredContent && (
                    <div className="mt-2 text-xs text-gray-500 dark:text-gray-400">
                      包含: {Object.keys(message.structuredContent).join(', ')}
                    </div>
                  )}
                </div>
                );
              })}
            </div>
          </CardContent>
        </Card>
      )}

      {/* 流式Buffer显示 */}
      {streamingBuffer && (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm">流式Buffer (实时)</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
              <pre className="text-xs text-gray-700 dark:text-gray-300 whitespace-pre-wrap max-h-40 overflow-y-auto">
                {streamingBuffer}
              </pre>
            </div>
          </CardContent>
        </Card>
      )}

      {/* 结构化内容显示 */}
      {Object.keys(structuredContent).length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm">结构化内容</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {Object.entries(structuredContent).map(([key, value]) => (
                <div key={key} className="border rounded-lg p-3">
                  <div className="font-semibold text-sm mb-2 text-blue-600 dark:text-blue-400">
                    {key.toUpperCase()}:
                  </div>
                  <div className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
                    {value}
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* GameLayout组件渲染 - 多消息显示 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">
            GameLayout 渲染结果 
            {currentMessageIndex >= 0 && (
              <span className="ml-2 text-xs text-gray-500">
                (显示消息 #{currentMessageIndex + 1})
              </span>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="min-h-[600px]">
            {currentMessageIndex >= 0 && messages[currentMessageIndex] ? (
              // 显示选中消息的 GameLayout
              <div className="space-y-4">
                <div className="text-sm text-gray-600 dark:text-gray-400 mb-4">
                  正在显示消息 #{currentMessageIndex + 1} 的 GameLayout
                </div>
                <GameLayout
                  isDark={false}
                  isLoading={isLoading}
                  isAssessing={isAssessing}
                  onChoiceSelect={handleChoiceSelect}
                  onFreeActionModeChange={handleFreeActionModeChange}
                  skillsState={skillsState}
                  structuredContent={messages[currentMessageIndex].structuredContent as Record<string, string> || {}}
                />
              </div>
            ) : (
              // 显示当前流式内容的 GameLayout
              <div className="space-y-4">
                <div className="text-sm text-gray-600 dark:text-gray-400 mb-4">
                  显示当前流式内容
                </div>
                <GameLayout
                  isDark={false}
                  isLoading={isLoading}
                  isAssessing={isAssessing}
                  onChoiceSelect={handleChoiceSelect}
                  onFreeActionModeChange={handleFreeActionModeChange}
                  skillsState={skillsState}
                  structuredContent={structuredContent}
                />
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* 最近三轮对话的 GameLayout 对比视图 */}
      {getRecentMessages(messages).length > 1 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm">
              最近三轮对话的 GameLayout 对比 ({getRecentMessages(messages).length} 条消息)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-6">
              {getRecentMessages(messages).map((message, index) => {
                // 计算在完整消息列表中的实际索引
                const actualIndex = messages.length - getRecentMessages(messages).length + index;
                
                // 只在第一个AI消息时打印调试信息
                if (message.role === 'assistant' && message.structuredContent && index === 0) {
                  console.log('🔍 [GameLayout对比] 调试信息:', {
                    totalMessages: messages.length,
                    recentMessagesCount: getRecentMessages(messages).length,
                    firstAIMessageIndex: index,
                    firstAIMessageActualIndex: actualIndex,
                    firstAIMessageId: message.id,
                    displayMessageNumber: actualIndex + 1
                  });
                }
                
                return message.role === 'assistant' && message.structuredContent ? (
                  <div key={message.id} className="border rounded-lg p-4">
                    <div className="flex items-center justify-between mb-4">
                      <h4 className="font-semibold text-sm">
                        消息 #{actualIndex + 1} - {message.timestamp.toLocaleTimeString()}
                      </h4>
                      <Button
                        onClick={() => viewMessage(actualIndex)}
                        variant="outline"
                        size="sm"
                        className="text-xs"
                      >
                        查看此消息
                      </Button>
                    </div>
                    <div className="min-h-[400px]">
                      <GameLayout
                        isDark={false}
                        isLoading={false}
                        isAssessing={false}
                        onChoiceSelect={handleChoiceSelect}
                        onFreeActionModeChange={handleFreeActionModeChange}
                        skillsState={skillsState}
                        structuredContent={message.structuredContent as Record<string, string>}
                      />
                    </div>
                  </div>
                ) : null;
              })}
            </div>
          </CardContent>
        </Card>
      )}

      {/* UI库Card组件彩色测试区域 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">UI库Card组件彩色测试</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {/* 场景描述卡片 - 使用UI库Card */}
            <Card 
              className="border-2 border-blue-400/70 shadow-lg hover:shadow-xl transition-all duration-300 border-l-4 border-l-blue-600"
              style={{background: 'linear-gradient(135deg, rgb(219 234 254 / 0.95), rgb(191 219 254 / 0.85))'}}
            >
              <CardHeader className="pb-2">
                <div className="inline-flex items-center px-4 py-2 rounded-full text-sm font-bold bg-blue-300/90 text-blue-900 shadow-sm w-fit">
                  场景描述
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-sm leading-relaxed text-blue-900 font-medium">
                  你站在一座古老警局的台阶前，锈迹斑斑的铁门在微风中轻轻摇晃。空气中弥漫着潮湿的尘埃与旧纸张的气息。
                </div>
              </CardContent>
            </Card>

            {/* 角色动作卡片 - 使用UI库Card */}
            <Card 
              className="border-2 border-green-400/70 shadow-lg hover:shadow-xl transition-all duration-300 border-l-4 border-l-green-600"
              style={{background: 'linear-gradient(135deg, rgb(220 252 231 / 0.95), rgb(187 247 208 / 0.85))'}}
            >
              <CardHeader className="pb-2">
                <div className="inline-flex items-center px-4 py-2 rounded-full text-sm font-bold bg-green-300/90 text-green-900 shadow-sm w-fit">
                  角色动作
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-sm leading-relaxed text-green-900 font-medium">
                  你的手不自觉地按在腰间的配枪上，指尖触到冰冷的金属——这是你作为警探的第一天。
                </div>
              </CardContent>
            </Card>

            {/* NPC对话卡片 - 使用UI库Card */}
            <Card 
              className="border-2 border-purple-400/70 shadow-lg hover:shadow-xl transition-all duration-300 border-l-4 border-l-purple-600"
              style={{background: 'linear-gradient(135deg, rgb(237 233 254 / 0.95), rgb(196 181 253 / 0.85))'}}
            >
              <CardHeader className="pb-2">
                <div className="inline-flex items-center px-4 py-2 rounded-full text-sm font-bold bg-purple-300/90 text-purple-900 shadow-sm w-fit">
                  NPC对话
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-sm leading-relaxed text-purple-900 font-medium">
                  <span className="text-blue-600 font-medium italic">"Inspector Panzijian..."</span> 一个沙哑的声音从阴影中传来，<span className="text-blue-600 font-medium italic">"欢迎来到'雾都案卷馆'。"</span>
                </div>
              </CardContent>
            </Card>

            {/* 环境变化卡片 - 使用UI库Card */}
            <Card 
              className="border-2 border-orange-400/70 shadow-lg hover:shadow-xl transition-all duration-300 border-l-4 border-l-orange-600"
              style={{background: 'linear-gradient(135deg, rgb(255 237 213 / 0.95), rgb(254 215 170 / 0.85))'}}
            >
              <CardHeader className="pb-2">
                <div className="inline-flex items-center px-4 py-2 rounded-full text-sm font-bold bg-orange-300/90 text-orange-900 shadow-sm w-fit">
                  环境变化
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-sm leading-relaxed text-orange-900 font-medium">
                  突然，警局大厅的灯闪烁了一下，一束惨白的光线照在墙上的老式挂钟上——时间停在了凌晨3:17。
                </div>
              </CardContent>
            </Card>

            {/* 声音效果卡片 - 使用UI库Card */}
            <Card 
              className="border-2 border-red-400/70 shadow-lg hover:shadow-xl transition-all duration-300 border-l-4 border-l-red-600"
              style={{background: 'linear-gradient(135deg, rgb(254 226 226 / 0.95), rgb(252 165 165 / 0.85))'}}
            >
              <CardHeader className="pb-2">
                <div className="inline-flex items-center px-4 py-2 rounded-full text-sm font-bold bg-red-300/90 text-red-900 shadow-sm w-fit">
                  声音效果
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-sm leading-relaxed text-red-900 font-medium">
                  你听见走廊深处传来一声重物坠地的闷响，紧接着，是锁链拖地的声音……
                </div>
              </CardContent>
            </Card>

            {/* NPC低语卡片 - 使用UI库Card */}
            <Card 
              className="border-2 border-amber-400/70 shadow-lg hover:shadow-xl transition-all duration-300 border-l-4 border-l-amber-600"
              style={{background: 'linear-gradient(135deg, rgb(254 243 199 / 0.95), rgb(253 230 138 / 0.85))'}}
            >
              <CardHeader className="pb-2">
                <div className="inline-flex items-center px-4 py-2 rounded-full text-sm font-bold bg-amber-300/90 text-amber-900 shadow-sm w-fit">
                  NPC低语
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-sm leading-relaxed text-amber-900 font-medium">
                  <span className="text-blue-600 font-medium italic">"有人在下面。"</span>那声音低语道，<span className="text-blue-600 font-medium italic">"但没人能活着上来。"</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </CardContent>
      </Card>

      {/* 调试信息 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">调试信息</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <h4 className="font-semibold mb-2">SkillsState:</h4>
              <pre className="text-xs bg-gray-50 dark:bg-gray-800 p-3 rounded overflow-auto max-h-40">
                {JSON.stringify(skillsState, null, 2)}
              </pre>
            </div>
            <div>
              <h4 className="font-semibold mb-2">StructuredContent:</h4>
              <pre className="text-xs bg-gray-50 dark:bg-gray-800 p-3 rounded overflow-auto max-h-40">
                {JSON.stringify(structuredContent, null, 2)}
              </pre>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default RoleplayTestDemo;

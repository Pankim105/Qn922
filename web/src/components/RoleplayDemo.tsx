import React, { useState, useEffect } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Button } from 'modern-ui-components';
import { Send, Dice6, Sparkles, Crown, Sword, GraduationCap, Heart, Wand2, Shield, Star, User } from 'lucide-react';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  worldType: string;
  roleName?: string; // AI角色名称
}

interface WorldTemplate {
  worldId: string;
  worldName: string;
  description: string;
  aiRole: string;
}

const RoleplayDemo: React.FC = () => {
  const [selectedWorld, setSelectedWorld] = useState<string>('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isDark, setIsDark] = useState(false);

  // 模拟世界模板数据
  const worlds: WorldTemplate[] = [
    {
      worldId: 'fantasy_adventure',
      worldName: '异世界探险',
      description: '经典的奇幻冒险世界，充满魔法、怪物和宝藏',
      aiRole: '游戏主持人'
    },
    {
      worldId: 'western_magic',
      worldName: '西方魔幻',
      description: '西式魔法世界，包含法师、骑士和龙',
      aiRole: '贤者向导'
    },
    {
      worldId: 'martial_arts',
      worldName: '东方武侠',
      description: '充满武功、江湖恩怨的武侠世界',
      aiRole: '江湖前辈'
    },
    {
      worldId: 'japanese_school',
      worldName: '日式校园',
      description: '现代日本校园生活，充满青春与友情',
      aiRole: '校园向导'
    },
    {
      worldId: 'educational',
      worldName: '寓教于乐',
      description: '教育性世界，通过互动学习知识',
      aiRole: '智慧导师'
    }
  ];

  // 世界类型图标映射
  const worldIcons = {
    fantasy_adventure: <Sword className="w-4 h-4" />,
    western_magic: <Wand2 className="w-4 h-4" />,
    martial_arts: <Crown className="w-4 h-4" />,
    japanese_school: <Heart className="w-4 h-4" />,
    educational: <GraduationCap className="w-4 h-4" />
  };

  // 监听主题变化
  useEffect(() => {
    const checkTheme = () => {
      setIsDark(document.documentElement.classList.contains('dark'));
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
    }
  };

  // AI角色图标映射
  const aiRoleIcons = {
    '游戏主持人': <Shield className="w-3 h-3" />,
    '贤者向导': <Star className="w-3 h-3" />,
    '江湖前辈': <Sword className="w-3 h-3" />,
    '校园向导': <Heart className="w-3 h-3" />,
    '智慧导师': <GraduationCap className="w-3 h-3" />
  };

  // 模拟AI回复
  const getAIResponse = (worldType: string, _userMessage: string): string => {
    const responses = {
      fantasy_adventure: [
        "🏰 欢迎来到奇幻世界！作为你的游戏主持人，我将引导你进行一场精彩的冒险。你面前出现了一座古老的城堡，门前站着一位守卫...",
        "⚔️ 你的勇敢令人敬佩！让我为你进行一次攻击检定... [DICE:d20+3:攻击检定] 你成功击中了敌人！",
        "✨ 在探索过程中，你发现了一本古老的魔法书。它散发着神秘的光芒，你要打开它吗？"
      ],
      western_magic: [
        "🧙‍♂️ 作为贤者向导，我将帮助你在这个魔法世界中找到自己的道路。你想学习哪种魔法？元素魔法、神圣魔法，还是暗黑魔法？",
        "🐉 远方传来了龙的咆哮声！这可能是一个机会，也可能是一个危险。你的选择将决定故事的走向...",
        "🏛️ 法师公会的大门为你敞开。里面有无数的魔法知识等待着你去探索。"
      ],
      martial_arts: [
        "🥋 年轻的侠客，江湖路险，需要你有一颗侠义之心。你想加入哪个门派？少林、武当，还是峨眉？",
        "⚔️ 你的武功已有小成，但江湖恩怨复杂。记住，武德比武功更重要。",
        "🏮 夜深了，客栈中传来了神秘的消息。有人在寻找一本失传的武功秘籍..."
      ],
      japanese_school: [
        "🌸 欢迎来到我们的校园！作为你的校园向导，我会帮助你适应这里的生活。你想加入哪个社团呢？",
        "🎭 文化祭就要开始了！你们班准备表演什么节目？这将是展现才华的好机会。",
        "💕 春天的樱花开得正美，你和朋友们一起在樱花树下野餐，享受着青春的美好时光。"
      ],
      educational: [
        "📚 作为你的智慧导师，我将通过有趣的冒险帮助你学习。今天我们要探索数学王国，准备好了吗？",
        "🧮 让我们来解决一个数学谜题：[CHALLENGE:MATH:2:如果一个农夫有15只鸡和8只鸭，那么总共有多少只禽类？]",
        "🏛️ 历史的长河中隐藏着无数智慧。让我们穿越到古代，看看古人是如何解决问题的..."
      ]
    };

    const worldResponses = responses[worldType as keyof typeof responses] || responses.fantasy_adventure;
    return worldResponses[Math.floor(Math.random() * worldResponses.length)];
  };

  // 初始化世界
  const initializeWorld = (worldType: string) => {
    setSelectedWorld(worldType);
    const world = worlds.find(w => w.worldId === worldType);
    if (world) {
      const welcomeMessage: Message = {
        id: '1',
        role: 'assistant',
        content: `欢迎来到${world.worldName}！我是你的${world.aiRole}。${getAIResponse(worldType, '开始冒险')}`,
        timestamp: new Date(),
        worldType: worldType,
        roleName: world.aiRole
      };
      setMessages([welcomeMessage]);
    }
  };

  // 发送消息
  const sendMessage = () => {
    if (!inputMessage.trim() || !selectedWorld) return;

    const world = worlds.find(w => w.worldId === selectedWorld);
    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: inputMessage.trim(),
      timestamp: new Date(),
      worldType: selectedWorld
    };

    const aiResponse: Message = {
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      content: getAIResponse(selectedWorld, inputMessage),
      timestamp: new Date(),
      worldType: selectedWorld,
      roleName: world?.aiRole
    };

    setMessages(prev => [...prev, userMessage, aiResponse]);
    setInputMessage('');
  };

  // 执行骰子检定
  const rollDice = () => {
    if (!selectedWorld) return;

    const world = worlds.find(w => w.worldId === selectedWorld);
    const diceResult = Math.floor(Math.random() * 20) + 1;
    const modifier = Math.floor(Math.random() * 5);
    const total = diceResult + modifier;
    const success = total >= 15;

    const diceMessage: Message = {
      id: Date.now().toString(),
      role: 'assistant',
      content: `🎲 骰子检定结果：${diceResult} (d20) + ${modifier} = ${total} ${success ? '✅ 成功' : '❌ 失败'}`,
      timestamp: new Date(),
      worldType: selectedWorld,
      roleName: world?.aiRole
    };

    setMessages(prev => [...prev, diceMessage]);
  };

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
      const roleName = message.roleName;
      const roleColor = roleName ? aiRoleColors[roleName as keyof typeof aiRoleColors] : null;
      
      if (roleColor) {
        const colorScheme = darkMode ? roleColor.dark : roleColor.light;
        return `bg-gradient-to-r ${colorScheme} mr-auto max-w-sm`;
      } else {
        // 降级到默认颜色
        return darkMode 
          ? 'bg-gradient-to-r from-gray-600 to-gray-700 text-white mr-auto max-w-sm'
          : 'bg-gradient-to-r from-gray-100 to-gray-200 text-gray-900 mr-auto max-w-sm';
      }
    }
  };

  return (
    <Card className="h-[600px] flex flex-col">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Sparkles className="w-5 h-5" />
          角色扮演世界演示
        </CardTitle>
        
        {!selectedWorld && (
          <div className="space-y-3">
            <p className="text-sm text-gray-600 dark:text-gray-400">选择一个世界开始冒险：</p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {worlds.map((world) => (
                <Button
                  key={world.worldId}
                  variant="outline"
                  size="sm"
                  onClick={() => initializeWorld(world.worldId)}
                  className="flex items-center gap-2 justify-start p-3 h-auto"
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

        {selectedWorld && (
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
                onClick={rollDice}
                className="flex items-center gap-1"
              >
                <Dice6 className="w-4 h-4" />
                d20
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setSelectedWorld('');
                  setMessages([]);
                }}
              >
                切换世界
              </Button>
            </div>
          </div>
        )}
      </CardHeader>

      {selectedWorld && (
        <>
          <CardContent className="flex-1 overflow-y-auto space-y-3 p-4">
            {messages.map((message) => (
              <div
                key={message.id}
                className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
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
                  <div className="whitespace-pre-wrap text-sm leading-relaxed">
                    {message.content}
                  </div>
                  <div className="text-xs opacity-70 mt-2 text-right">
                    {message.timestamp.toLocaleTimeString()}
                  </div>
                </div>
              </div>
            ))}
          </CardContent>

          <div className="p-4 border-t border-gray-200 dark:border-gray-700">
            <div className="flex gap-2">
              <input
                type="text"
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                placeholder="输入你的行动或对话..."
                onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
                className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <Button
                onClick={sendMessage}
                disabled={!inputMessage.trim()}
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
                查看状态
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setInputMessage("我想学习新技能")}
                className="text-xs"
              >
                学习技能
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setInputMessage("我要探索新区域")}
                className="text-xs"
              >
                探索区域
              </Button>
            </div>
          </div>
        </>
      )}
    </Card>
  );
};

export default RoleplayDemo;

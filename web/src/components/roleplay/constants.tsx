import { 
  Sword, 
  Wand2, 
  Crown, 
  Heart, 
  GraduationCap, 
  Shield, 
  Star, 
  Zap,
  User,
  ScrollText,
  Globe,
  MessageCircle,
  ArrowRight,
  BookOpen,
  Search,
  Rocket
} from 'lucide-react';

// 结构化内容解析模式 - 支持新的方括号格式和旧的星号格式
export const STRUCTURED_CONTENT_PATTERNS = {
  dialogue: /(?:\[DIALOGUE\]|(?:\/\*|\\\*)DIALOGUE:?)(.*?)(?:\[\/DIALOGUE\]|(?:\*\/|\*\/))/gs,
  status: /(?:\[STATUS\]|(?:\/\*|\\\*)STATUS:?)(.*?)(?:\[\/STATUS\]|(?:\*\/|\*\/))/gs,
  world: /(?:\[WORLD\]|(?:\/\*|\\\*)WORLD:?)(.*?)(?:\[\/WORLD\]|(?:\*\/|\*\/))/gs,
  quests: /(?:\[QUESTS\]|(?:\/\*|\\\*)QUESTS\s*:?)(.*?)(?:\[\/QUESTS\]|(?:\*\/|\*\/))/gs,
  choices: /(?:\[CHOICES\]|(?:\/\*|\\\*)CHOICES:?)(.*?)(?:\[\/CHOICES\]|(?:\*\/|\*\/))/gs,
  assessment: /§({.*?})§/gs, // 评估JSON格式：§{...}§
};

// 消息解析器模式 - 用于parseStructuredMessage函数
export const MESSAGE_PARSER_PATTERNS = [
  { type: 'dialogue', regex: STRUCTURED_CONTENT_PATTERNS.dialogue, title: '对话与叙述', icon: 'MessageCircle' },
  { type: 'status', regex: STRUCTURED_CONTENT_PATTERNS.status, title: '角色状态', icon: 'User' },
  { type: 'world', regex: STRUCTURED_CONTENT_PATTERNS.world, title: '世界状态', icon: 'Globe' },
  { type: 'quests', regex: STRUCTURED_CONTENT_PATTERNS.quests, title: '任务信息', icon: 'ScrollText' },
  { type: 'choices', regex: STRUCTURED_CONTENT_PATTERNS.choices, title: '行动选择', icon: 'ArrowRight' },
  { type: 'assessment', regex: STRUCTURED_CONTENT_PATTERNS.assessment, title: '系统评估', icon: 'BookOpen' },
];

// 评估JSON解析模式
export const ASSESSMENT_PATTERNS = {
  full: /§({.*?})§/gs, // 完整的评估JSON
  partial: /§.*$/gs // 评估内容的片段（从§开始到结尾）
};

// 世界类型图标映射
export const worldIcons = {
  fantasy_adventure: <Sword className="w-4 h-4" />,
  western_magic: <Wand2 className="w-4 h-4" />,
  martial_arts: <Crown className="w-4 h-4" />,
  japanese_school: <Heart className="w-4 h-4" />,
  educational: <GraduationCap className="w-4 h-4" />,
  detective_mystery: <Search className="w-4 h-4" />,
  sci_fi_future: <Rocket className="w-4 h-4" />
};

// 世界类型颜色映射 - 浅色模式友好
export const worldColors = {
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
  },
  detective_mystery: {
    light: 'from-slate-100 to-gray-200 text-slate-900',
    dark: 'from-slate-600 to-gray-700 text-white'
  },
  sci_fi_future: {
    light: 'from-cyan-100 to-teal-200 text-cyan-900',
    dark: 'from-cyan-600 to-teal-700 text-white'
  }
};

// AI角色颜色映射 - 为不同角色提供不同颜色
export const aiRoleColors = {
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
  '侦探大师': {
    light: 'from-slate-100 to-gray-200 text-slate-900',
    dark: 'from-slate-600 to-gray-700 text-white'
  },
  '科技向导': {
    light: 'from-cyan-100 to-teal-200 text-cyan-900',
    dark: 'from-cyan-600 to-teal-700 text-white'
  },
  '向导': {
    light: 'from-gray-100 to-slate-200 text-gray-900',
    dark: 'from-gray-600 to-slate-700 text-white'
  }
};

// AI角色图标映射
export const aiRoleIcons = {
  '游戏主持人': <Shield className="w-3 h-3" />,
  '贤者向导': <Star className="w-3 h-3" />,
  '江湖前辈': <Sword className="w-3 h-3" />,
  '校园向导': <Heart className="w-3 h-3" />,
  '智慧导师': <GraduationCap className="w-3 h-3" />,
  '侦探大师': <Search className="w-3 h-3" />,
  '科技向导': <Rocket className="w-3 h-3" />,
  '向导': <Zap className="w-3 h-3" />
};

// 消息部分图标映射
export const sectionIcons = {
  status: <User className="w-3 h-3" />,
  world: <Globe className="w-3 h-3" />,
  choices: <ArrowRight className="w-3 h-3" />,
  dialogue: <MessageCircle className="w-3 h-3" />,
  quests: <ScrollText className="w-3 h-3" />,
  assessment: <BookOpen className="w-3 h-3" />,
  plain: <ScrollText className="w-3 h-3" />
};

// 获取AI角色名称
export const getAIRoleName = (worldType: string): string => {
  const roleNames = {
    fantasy_adventure: '游戏主持人',
    western_magic: '贤者向导',
    martial_arts: '江湖前辈',
    japanese_school: '校园向导',
    educational: '智慧导师',
    detective_mystery: '侦探大师',
    sci_fi_future: '科技向导'
  };
  return roleNames[worldType as keyof typeof roleNames] || '向导';
};

// 获取卡片尺寸配置 - 游戏UI风格
export const getCardConfig = (type: string) => {
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

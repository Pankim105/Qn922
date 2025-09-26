export interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}

export interface WorldTemplate {
  worldId: string;
  worldName: string;
  description: string;
}

export interface MessageSection {
  type: 'dialogue' | 'status' | 'world' | 'choices' | 'quests' | 'assessment' | 'plain';
  content: string;
  title?: string;
  icon?: React.ReactNode;
}

export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  worldType?: string;
  roleName?: string; // AI角色名称
  sections?: MessageSection[]; // 结构化内容
  isStructured?: boolean; // 是否为结构化消息
  assessmentLoading?: boolean; // 是否正在评估中
}

export interface DiceResult {
  diceType: number;
  modifier: number;
  result: number;
  finalResult: number;
  context: string;
  isSuccessful: boolean;
}

export interface RoleplayChatProps {
  isAuthenticated: boolean;
  user: User | null;
  onAuthFailure: () => void;
  onSessionIdChange?: (sessionId: string) => void;
}

export interface WorldSelectorProps {
  worlds: WorldTemplate[];
  onWorldSelect: (worldId: string) => void;
  isLoading: boolean;
}

export interface MessageListProps {
  messages: Message[];
  visibleMessageCount: number;
  isLoading: boolean;
  selectedWorld: string;
  isDark: boolean;
  onShowMore: () => void;
  onShowAll: () => void;
  onScrollToBottom: () => void;
  hasMoreMessages: boolean;
  totalMessageCount: number;
  onChoiceSelect: (choice: string) => void;
  onFreeActionModeChange: (enabled: boolean) => void;
  skillsState?: any; // 从数据库获取的角色状态
  worldState?: any; // 从数据库获取的世界状态
  structuredContent?: StructuredContent; // 结构化内容依赖项
}

export interface MessageInputProps {
  inputMessage: string;
  onInputChange: (value: string) => void;
  onSendMessage: () => void;
  isLoading: boolean;
  isFreeActionMode: boolean;
  onFreeActionModeChange: (enabled: boolean) => void;
}

export interface MessageSectionProps {
  section: MessageSection;
  index: number;
  isDark: boolean;
  isLoading: boolean;
  onChoiceSelect: (choice: string) => void;
  onFreeActionModeChange: (enabled: boolean) => void;
}

// 结构化内容依赖项
export interface StructuredContent {
  dialogue?: string;      // /*DIALOGUE: ... */
  status?: string;        // /*STATUS: ... */
  world?: string;         // /*WORLD: ... */
  quests?: string;        // /*QUESTS: ... */
  choices?: string;       // /*CHOICES: ... */
  assessment?: string;    // §{...}§
}

// 新的 GameLayout Props
export interface GameLayoutProps {
  isDark: boolean;
  isLoading: boolean;
  isAssessing?: boolean;
  onChoiceSelect: (choice: string) => void;
  onFreeActionModeChange: (isFreeAction: boolean) => void;
  skillsState?: any;
  // 新增：结构化内容依赖项
  structuredContent?: StructuredContent;
}


import React, { useState, useEffect } from 'react';
import { Button, Card, CardHeader, CardTitle, CardContent, ThemeSwitcher } from 'modern-ui-components';
import { User, LogIn, LogOut, Menu, X } from 'lucide-react';
import AuthModal from './components/AuthModal';
import UserProfile from './components/UserProfile';
import RoleplayChat from './components/roleplay/RoleplayChat';
import WorldStateDisplay from './components/roleplay/WorldStateDisplay';

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}

function App() {
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [currentTheme, setCurrentTheme] = useState('blue');
  const [currentSessionId, setCurrentSessionId] = useState<string>('');
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  // 检查本地存储中的用户信息
  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    const accessToken = localStorage.getItem('accessToken');
    
    if (storedUser && accessToken) {
      try {
        const userData = JSON.parse(storedUser);
        setUser(userData);
        setIsAuthenticated(true);
      } catch (error) {
        console.error('解析用户数据失败:', error);
        // 清除无效数据
        localStorage.removeItem('user');
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
      }
    }
  }, []);

  // 应用主题
  React.useEffect(() => {
    document.documentElement.className = `${isDarkMode ? 'dark' : ''} theme-${currentTheme}`;
  }, [currentTheme, isDarkMode]);

  // 处理登录成功
  const handleLoginSuccess = () => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      const userData = JSON.parse(storedUser);
      setUser(userData);
      setIsAuthenticated(true);
    }
  };

  // 处理注册成功
  const handleRegisterSuccess = () => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      const userData = JSON.parse(storedUser);
      setUser(userData);
      setIsAuthenticated(true);
    }
  };

  // 处理登出
  const handleLogout = () => {
    setUser(null);
    setIsAuthenticated(false);
    setCurrentSessionId(''); // 清除当前会话ID
  };

  // 处理认证失败
  const handleAuthFailure = () => {
    setIsAuthenticated(false);
    setUser(null);
    setCurrentSessionId('');
  };

  // 处理会话ID变化（从RoleplayChat组件传递）
  const handleSessionIdChange = (sessionId: string) => {
    setCurrentSessionId(sessionId);
  };

  return (
    <div className="min-h-screen bg-background text-foreground">
      {/* 主题切换器 - 固定在右上角，脱离文档流 */}
      <ThemeSwitcher
        className="fixed top-4 right-4 z-50"
        currentTheme={currentTheme}
        isDarkMode={isDarkMode}
        onThemeChange={setCurrentTheme}
        onDarkModeChange={setIsDarkMode}
        minimizable={true}
        defaultMinimized={true}
      />

      {/* 头部导航 */}
      <header className="sticky top-0 z-40 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container mx-auto px-4">
          <div className="flex h-16 items-center justify-between">
            {/* Logo和标题 */}
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center">
                  <span className="text-primary-foreground font-bold text-sm">QN</span>
                </div>
                <h1 className="text-xl font-bold">QN Contest</h1>
              </div>
            </div>

            {/* 桌面端导航 */}
            <div className="hidden md:flex items-center gap-4">
              {!isAuthenticated ? (
                <Button 
                  onClick={() => setIsAuthModalOpen(true)}
                  variant="solid"
                  size="sm"
                  className="flex items-center gap-2"
                >
                  <LogIn className="w-4 h-4" />
                  登录
                </Button>
              ) : (
                <div className="flex items-center gap-4">

                  {/* 用户信息 */}
                  <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-muted/50">
                    <User className="w-4 h-4 text-muted-foreground" />
                    <span className="text-sm font-medium">{user?.username}</span>
                    <span className={`px-2 py-0.5 text-xs rounded-full ${
                      user?.role === 'ADMIN' 
                        ? 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-300' 
                        : 'bg-blue-100 text-blue-800 dark:bg-blue-900/20 dark:text-blue-300'
                    }`}>
                      {user?.role}
                    </span>
                  </div>

                  {/* 登出按钮 */}
                  <Button 
                    onClick={handleLogout}
                    variant="outline"
                    size="sm"
                    className="flex items-center gap-2"
                  >
                    <LogOut className="w-4 h-4" />
                    登出
                  </Button>
                </div>
              )}
            </div>

            {/* 移动端菜单按钮 */}
            <div className="md:hidden">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                className="h-8 w-8 p-0"
              >
                {isMobileMenuOpen ? <X className="w-4 h-4" /> : <Menu className="w-4 h-4" />}
              </Button>
            </div>
          </div>

          {/* 移动端菜单 */}
          {isMobileMenuOpen && (
            <div className="md:hidden border-t py-4">
              {!isAuthenticated ? (
                <Button 
                  onClick={() => {
                    setIsAuthModalOpen(true);
                    setIsMobileMenuOpen(false);
                  }}
                  variant="solid"
                  size="sm"
                  className="w-full flex items-center gap-2"
                >
                  <LogIn className="w-4 h-4" />
                  登录
                </Button>
              ) : (
                <div className="space-y-3">
                  {/* 用户信息 */}
                  <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-muted/50">
                    <User className="w-4 h-4 text-muted-foreground" />
                    <span className="text-sm font-medium">{user?.username}</span>
                    <span className={`px-2 py-0.5 text-xs rounded-full ${
                      user?.role === 'ADMIN' 
                        ? 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-300' 
                        : 'bg-blue-100 text-blue-800 dark:bg-blue-900/20 dark:text-blue-300'
                    }`}>
                      {user?.role}
                    </span>
                  </div>

                  {/* 登出按钮 */}
                  <Button 
                    onClick={() => {
                      handleLogout();
                      setIsMobileMenuOpen(false);
                    }}
                    variant="outline"
                    size="sm"
                    className="w-full flex items-center gap-2"
                  >
                    <LogOut className="w-4 h-4" />
                    登出
                  </Button>
                </div>
              )}
            </div>
          )}
        </div>
      </header>

      {/* 主要内容区域 */}
      <main className="container mx-auto px-4 py-6">
        {!isAuthenticated ? (
          /* 未登录状态 - 欢迎页面 */
          <div className="max-w-4xl mx-auto">
            <Card className="text-center">
              <CardHeader>
                <CardTitle className="text-3xl font-bold mb-4">
                  欢迎来到 QN Contest
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-6">
                <p className="text-lg text-muted-foreground leading-relaxed">
                  体验现代化的角色扮演世界，与AI进行沉浸式对话冒险
                </p>
                
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 my-8">
                  <div className="p-4 rounded-lg bg-muted/50">
                    <div className="w-12 h-12 rounded-lg bg-primary/20 flex items-center justify-center mx-auto mb-3">
                      <span className="text-primary font-bold text-lg">🎭</span>
                    </div>
                    <h3 className="font-semibold mb-2">角色扮演</h3>
                    <p className="text-sm text-muted-foreground">
                      多种世界设定，沉浸式角色扮演体验
                    </p>
                  </div>
                  
                  <div className="p-4 rounded-lg bg-muted/50">
                    <div className="w-12 h-12 rounded-lg bg-primary/20 flex items-center justify-center mx-auto mb-3">
                      <span className="text-primary font-bold text-lg">🤖</span>
                    </div>
                    <h3 className="font-semibold mb-2">AI对话</h3>
                    <p className="text-sm text-muted-foreground">
                      智能AI角色，自然流畅的对话体验
                    </p>
                  </div>
                  
                  <div className="p-4 rounded-lg bg-muted/50">
                    <div className="w-12 h-12 rounded-lg bg-primary/20 flex items-center justify-center mx-auto mb-3">
                      <span className="text-primary font-bold text-lg">📊</span>
                    </div>
                    <h3 className="font-semibold mb-2">状态追踪</h3>
                    <p className="text-sm text-muted-foreground">
                      实时世界状态，动态故事发展
                    </p>
                  </div>
                </div>

                <Button 
                  onClick={() => setIsAuthModalOpen(true)}
                  variant="solid"
                  size="lg"
                  className="px-8"
                >
                  开始冒险
                </Button>
              </CardContent>
            </Card>
          </div>
        ) : (
          /* 已登录状态 - 主应用界面 */
          <div className="space-y-6">
            {/* 流式输入测试区域 */}

            {/* 主应用界面 */}
            <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
              {/* 左侧边栏 - 用户信息和世界状态 */}
              <div className="lg:col-span-1 space-y-4">
                <UserProfile user={user} onLogout={handleLogout} />
                {currentSessionId && (
                  <WorldStateDisplay
                    sessionId={currentSessionId}
                    isAuthenticated={isAuthenticated}
                    onAuthFailure={handleAuthFailure}
                  />
                )}
              </div>

              {/* 右侧主内容区 - 角色扮演聊天 */}
              <div className="lg:col-span-3">
                <RoleplayChat 
                  isAuthenticated={isAuthenticated} 
                  user={user}
                  onAuthFailure={handleAuthFailure}
                  onSessionIdChange={handleSessionIdChange}
                />
              </div>
            </div>
          </div>
        )}
      </main>

      {/* 认证模态框 */}
      <AuthModal
        open={isAuthModalOpen}
        onOpenChange={setIsAuthModalOpen}
        onLogin={handleLoginSuccess}
        onRegister={handleRegisterSuccess}
      />
    </div>
  );
}

export default App;

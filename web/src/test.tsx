import React, { useState, useEffect } from 'react';
import { Button, Card, CardHeader, CardTitle, CardContent, ThemeSwitcher } from 'modern-ui-components';
import { User } from 'lucide-react';
import AuthModal from './components/AuthModal';
import UserProfile from './components/UserProfile';
import ApiTester from './components/ApiTester';
import EnhancedAIChat from './components/EnhancedAIChat';

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}


function Test() {
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [currentTheme, setCurrentTheme] = useState('blue');


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

  // 应用主题，按照Demo.tsx的方式
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
  };


  return (
    <div className="min-h-screen bg-background text-foreground">
      {/* 主题切换器 - 固定在右上角 */}
      <ThemeSwitcher
        className="fixed top-4 right-4 z-50"
        currentTheme={currentTheme}
        isDarkMode={isDarkMode}
        onThemeChange={setCurrentTheme}
        onDarkModeChange={setIsDarkMode}
        minimizable={true}
        defaultMinimized={false}
      />

      <div className="max-w-6xl mx-auto p-4 space-y-6">
        {/* 头部 */}
        <Card>
          <CardHeader>
            <CardTitle className="text-2xl font-bold text-center">
              QN Contest - 现代认证系统
            </CardTitle>
          </CardHeader>
          <CardContent className="text-center">
            <p className="text-muted-foreground mb-6 leading-relaxed">
              基于Spring Boot后端的现代化用户认证系统前端界面
            </p>
            
            {!isAuthenticated ? (
              <Button 
                onClick={() => setIsAuthModalOpen(true)}
                variant="solid"
                size="lg"
                className="px-8"
              >
                开始使用
              </Button>
            ) : (
              <div className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300 border border-green-200 dark:border-green-800">
                <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                欢迎回来，{user?.username}！
              </div>
            )}
          </CardContent>
        </Card>

        {/* 主要内容区域 */}
        <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
          {/* 用户信息 */}
          {isAuthenticated && (
            <div className="space-y-4">
              <UserProfile user={user} onLogout={handleLogout} />
            </div>
          )}

          {/* API测试 */}
          <div className="space-y-4">
            <ApiTester isAuthenticated={isAuthenticated} />
          </div>

          {/* AI对话 */}
          <div className="space-y-4 lg:col-span-2 xl:col-span-1">
            <EnhancedAIChat 
              isAuthenticated={isAuthenticated} 
              onAuthFailure={() => {
                setIsAuthenticated(false);
                setUser(null);
              }}
            />
          </div>
        </div>

        {/* 功能说明 */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <div className="w-5 h-5 rounded bg-primary"></div>
              功能特性
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm">
              <div className="space-y-3">
                <h4 className="font-semibold text-primary flex items-center gap-2">
                  🔐 认证功能
                </h4>
                <ul className="space-y-2 text-muted-foreground">
                  <li className="flex items-start gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-primary mt-2 flex-shrink-0"></div>
                    用户注册（用户名、邮箱、密码验证）
                  </li>
                  <li className="flex items-start gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-primary mt-2 flex-shrink-0"></div>
                    用户登录（JWT双令牌认证）
                  </li>
                  <li className="flex items-start gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-primary mt-2 flex-shrink-0"></div>
                    自动令牌刷新机制
                  </li>
                  <li className="flex items-start gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-primary mt-2 flex-shrink-0"></div>
                    安全登出清理
                  </li>
                </ul>
              </div>
              
              <div className="space-y-3">
                <h4 className="font-semibold text-primary flex items-center gap-2">
                  🧪 API测试
                </h4>
                <ul className="space-y-2 text-muted-foreground">
                  <li className="flex items-start gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-primary mt-2 flex-shrink-0"></div>
                    公开端点（无需认证）
                  </li>
                  <li className="flex items-start gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-primary mt-2 flex-shrink-0"></div>
                    用户端点（需要登录）
                  </li>
                  <li className="flex items-start gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-primary mt-2 flex-shrink-0"></div>
                    管理员端点（需要管理员权限）
                  </li>
                  <li className="flex items-start gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-primary mt-2 flex-shrink-0"></div>
                    实时响应状态显示
                  </li>
                </ul>
              </div>

              <div className="space-y-3">
                <h4 className="font-semibold text-primary flex items-center gap-2">
                  🤖 增强版AI对话
                </h4>
                <ul className="space-y-2 text-muted-foreground">
                  <li className="flex items-start gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-primary mt-2 flex-shrink-0"></div>
                    流式对话体验
                  </li>
                  <li className="flex items-start gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-primary mt-2 flex-shrink-0"></div>
                    历史对话管理
                  </li>
                  <li className="flex items-start gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-primary mt-2 flex-shrink-0"></div>
                    会话恢复和继续
                  </li>
                  <li className="flex items-start gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-primary mt-2 flex-shrink-0"></div>
                    数据库持久化存储
                  </li>
                  <li className="flex items-start gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-primary mt-2 flex-shrink-0"></div>
                    智能会话标题生成
                  </li>
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 默认用户信息 */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <div className="w-5 h-5 rounded bg-primary"></div>
              测试账户
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div className="p-4 bg-blue-50/50 dark:bg-blue-900/20 rounded-xl border border-blue-200/50 dark:border-blue-800/50">
                <h4 className="font-semibold text-blue-800 dark:text-blue-200 mb-2">管理员用户</h4>
                <div className="space-y-1 text-blue-600 dark:text-blue-300">
                  <p>用户名: <span className="font-mono">admin</span></p>
                  <p>密码: <span className="font-mono">admin123</span></p>
                  <p>角色: <span className="px-2 py-0.5 bg-blue-100 dark:bg-blue-900 rounded text-xs">ADMIN</span></p>
                </div>
              </div>
              
              <div className="p-4 bg-green-50/50 dark:bg-green-900/20 rounded-xl border border-green-200/50 dark:border-green-800/50">
                <h4 className="font-semibold text-green-800 dark:text-green-200 mb-2">普通用户</h4>
                <div className="space-y-1 text-green-600 dark:text-green-300">
                  <p>用户名: <span className="font-mono">testuser</span></p>
                  <p>密码: <span className="font-mono">test123</span></p>
                  <p>角色: <span className="px-2 py-0.5 bg-green-100 dark:bg-green-900 rounded text-xs">USER</span></p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

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

export default Test;

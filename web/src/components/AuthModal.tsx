import React, { useState, useEffect } from 'react';
import {
  Modal,
  ModalHeader,
  ModalTitle,
  ModalDescription,
  ModalBody,
  ModalClose,
  Button,
  Form,
  FormField,
  FormLabel,
  FormControl,
  FormHelpText,
  ModernInput,
  PasswordInput
} from 'modern-ui-components';
import { User, Mail, Lock, AlertCircle, CheckCircle, Key } from 'lucide-react';
import axios from 'axios';

interface AuthModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onLogin?: (credentials: LoginCredentials) => void;
  onRegister?: (userData: RegisterData) => void;
}

interface LoginCredentials {
  username: string;
  password: string;
}

interface RegisterData {
  username: string;
  email: string;
  password: string;
  adminKey?: string;
}

const AuthModal: React.FC<AuthModalProps> = ({ 
  open, 
  onOpenChange, 
  onLogin, 
  onRegister 
}) => {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');

  // 登录表单状态
  const [loginForm, setLoginForm] = useState<LoginCredentials>({
    username: '',
    password: ''
  });

  // 注册表单状态
  const [registerForm, setRegisterForm] = useState<RegisterData>({
    username: '',
    email: '',
    password: '',
    adminKey: ''
  });

  // 阻止模态框打开时的背景滚动和其他默认行为
  useEffect(() => {
    if (open) {
      // 阻止背景滚动
      const originalStyle = window.getComputedStyle(document.body).overflow;
      document.body.style.overflow = 'hidden';
      
      // 阻止鼠标滚轮事件
      const preventWheel = (e: WheelEvent) => {
        e.preventDefault();
      };
      
      // 阻止触摸滚动
      const preventTouchMove = (e: TouchEvent) => {
        e.preventDefault();
      };
      
      // 阻止键盘滚动（空格、方向键等）
      const preventKeyScroll = (e: KeyboardEvent) => {
        const scrollKeys = ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'PageUp', 'PageDown', 'Home', 'End', ' '];
        if (scrollKeys.includes(e.key)) {
          e.preventDefault();
        }
      };
      
      document.addEventListener('wheel', preventWheel, { passive: false });
      document.addEventListener('touchmove', preventTouchMove, { passive: false });
      document.addEventListener('keydown', preventKeyScroll);
      
      return () => {
        document.body.style.overflow = originalStyle;
        document.removeEventListener('wheel', preventWheel);
        document.removeEventListener('touchmove', preventTouchMove);
        document.removeEventListener('keydown', preventKeyScroll);
      };
    }
  }, [open]);

  // 重置表单
  const resetForms = () => {
    setLoginForm({ username: '', password: '' });
    setRegisterForm({ username: '', email: '', password: '', adminKey: '' });
    setError('');
    setSuccess('');
  };

  // 切换模式
  const switchMode = (newMode: 'login' | 'register') => {
    setMode(newMode);
    resetForms();
  };

  // 处理登录
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await axios.post('http://localhost:8080/api/auth/login', loginForm);
      const data = response.data;

      setSuccess('登录成功！正在为您跳转...');
      // 存储token到localStorage
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      localStorage.setItem('user', JSON.stringify(data.user));
      
      // 调用父组件的登录回调
      if (onLogin) {
        onLogin(loginForm);
      }
      
      // 延迟关闭模态框
      setTimeout(() => {
        onOpenChange(false);
        resetForms();
      }, 1500);
    } catch (error: any) {
      console.error('登录错误:', error);
      const errorMessage = error.response?.data?.message || '登录失败，请检查您的凭据';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // 处理注册
  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      // 准备注册数据，只有在adminKey不为空时才包含它
      const registerData: any = {
        username: registerForm.username,
        email: registerForm.email,
        password: registerForm.password
      };
      
      if (registerForm.adminKey && registerForm.adminKey.trim() !== '') {
        registerData.adminKey = registerForm.adminKey;
      }

      const response = await axios.post('http://localhost:8080/api/auth/register', registerData);
      const data = response.data;

      setSuccess('注册成功！正在为您登录...');
      // 存储token到localStorage
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      localStorage.setItem('user', JSON.stringify(data.user));
      
      // 调用父组件的注册回调
      if (onRegister) {
        onRegister(registerForm);
      }
      
      // 延迟关闭模态框
      setTimeout(() => {
        onOpenChange(false);
        resetForms();
      }, 1500);
    } catch (error: any) {
      console.error('注册错误:', error);
      const errorMessage = error.response?.data?.message || '注册失败，请重试';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };



  return (
    <Modal 
      open={open} 
      onOpenChange={onOpenChange} 
      variant="default"
      size="md"
      closeOnOverlay={true}
    >
      <div className="relative">
        <ModalHeader className="text-center pb-6">
          <ModalTitle className="text-2xl font-bold mb-2">
            {mode === 'login' ? '欢迎回来' : '创建新账户'}
          </ModalTitle>
          
          <ModalDescription className="text-muted-foreground">
            {mode === 'login' 
              ? '使用您的账户凭据登录以继续' 
              : '请填写信息创建您的新账户'
            }
          </ModalDescription>
          
          <ModalClose />
        </ModalHeader>

        <ModalBody className="px-6 pb-6">
          {/* 模式切换器 */}
          <div className="mb-8">
            <div className="flex bg-muted rounded-2xl p-1.5 border border-border">
              <button
                type="button"
                onClick={() => switchMode('login')}
                className={`flex-1 flex items-center justify-center py-3 px-4 rounded-xl transition-all duration-300 font-medium text-sm ${
                  mode === 'login' 
                    ? 'bg-background text-foreground shadow-md border border-border/50' 
                    : 'text-muted-foreground hover:text-foreground hover:bg-muted/30'
                }`}
              >
                登录
              </button>
              <button
                type="button"
                onClick={() => switchMode('register')}
                className={`flex-1 flex items-center justify-center py-3 px-4 rounded-xl transition-all duration-300 font-medium text-sm ${
                  mode === 'register' 
                    ? 'bg-background text-foreground shadow-md border border-border/50' 
                    : 'text-muted-foreground hover:text-foreground hover:bg-muted/30'
                }`}
              >
                注册
              </button>
            </div>
          </div>

          {/* 状态消息 */}
          {error && (
            <div className="mb-6 p-4 rounded-xl bg-destructive/10 border border-destructive/20 backdrop-blur-sm">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-destructive/20 flex items-center justify-center flex-shrink-0">
                  <AlertCircle className="w-4 h-4 text-destructive" />
                </div>
                <p className="text-sm text-destructive font-medium">{error}</p>
              </div>
            </div>
          )}
          
          {success && (
            <div className="mb-6 p-4 rounded-xl bg-green-50/80 dark:bg-green-900/20 border border-green-200/50 dark:border-green-800/50 backdrop-blur-sm">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-green-100 dark:bg-green-900 flex items-center justify-center flex-shrink-0">
                  <CheckCircle className="w-4 h-4 text-green-600 dark:text-green-400" />
                </div>
                <p className="text-sm text-green-700 dark:text-green-300 font-medium">{success}</p>
              </div>
            </div>
          )}

          {/* 登录表单 */}
          {mode === 'login' && (
            <Form onSubmit={handleLogin} className="space-y-6">
              <FormField>
                <FormLabel>用户名</FormLabel>
                <FormControl>
                  <ModernInput
                    variant="default"
                    leftIcon={<User className="h-4 w-4" />}
                    placeholder="请输入用户名"
                    value={loginForm.username}
                    onChange={(e) => setLoginForm({ ...loginForm, username: e.target.value })}
                    required
                  />
                </FormControl>
              </FormField>

              <FormField>
                <FormLabel>密码</FormLabel>
                <FormControl>
                  <PasswordInput
                    variant="default"
                    leftIcon={<Lock className="h-4 w-4" />}
                    placeholder="请输入密码"
                    value={loginForm.password}
                    onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })}
                    required
                  />
                </FormControl>
              </FormField>

              <div className="pt-4">
                <Button 
                  type="submit" 
                  preset="primary"
                  size="lg"
                  loading={loading}
                  disabled={loading}
                  className="w-full"
                >
                  {loading ? '登录中...' : '登录'}
                </Button>
              </div>
            </Form>
          )}

          {/* 注册表单 */}
          {mode === 'register' && (
            <Form onSubmit={handleRegister} className="space-y-6">
              <FormField>
                <FormLabel>用户名</FormLabel>
                <FormControl>
                  <ModernInput
                    variant="default"
                    leftIcon={<User className="h-4 w-4" />}
                    placeholder="请输入用户名（3-50个字符）"
                    value={registerForm.username}
                    onChange={(e) => setRegisterForm({ ...registerForm, username: e.target.value })}
                    required
                    minLength={3}
                    maxLength={50}
                  />
                </FormControl>
                <FormHelpText>
                  用户名长度需要在3-50个字符之间
                </FormHelpText>
              </FormField>

              <FormField>
                <FormLabel>邮箱地址</FormLabel>
                <FormControl>
                  <ModernInput
                    variant="default"
                    type="email"
                    leftIcon={<Mail className="h-4 w-4" />}
                    placeholder="请输入邮箱地址"
                    value={registerForm.email}
                    onChange={(e) => setRegisterForm({ ...registerForm, email: e.target.value })}
                    required
                  />
                </FormControl>
              </FormField>

              <FormField>
                <FormLabel>密码</FormLabel>
                <FormControl>
                  <PasswordInput
                    variant="default"
                    leftIcon={<Lock className="h-4 w-4" />}
                    placeholder="请输入密码（至少6个字符）"
                    value={registerForm.password}
                    onChange={(e) => setRegisterForm({ ...registerForm, password: e.target.value })}
                    required
                    minLength={6}
                  />
                </FormControl>
                <FormHelpText>
                  密码长度至少需要6个字符
                </FormHelpText>
              </FormField>

              <FormField>
                <FormLabel>管理员密钥（可选）</FormLabel>
                <FormControl>
                  <ModernInput
                    variant="default"
                    leftIcon={<Key className="h-4 w-4" />}
                    placeholder="输入管理员密钥以注册为管理员"
                    value={registerForm.adminKey || ''}
                    onChange={(e) => setRegisterForm({ ...registerForm, adminKey: e.target.value })}
                    type="password"
                  />
                </FormControl>
                <FormHelpText>
                  留空将注册为普通用户，输入正确的管理员密钥将注册为管理员
                </FormHelpText>
              </FormField>

              <div className="pt-4">
                <Button 
                  type="submit" 
                  preset="primary"
                  size="lg"
                  loading={loading}
                  disabled={loading}
                  className="w-full"
                >
                  {loading ? '注册中...' : '创建账户'}
                </Button>
              </div>
            </Form>
          )}
        </ModalBody>
      </div>
    </Modal>
  );
};

export default AuthModal;

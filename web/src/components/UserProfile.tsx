import React from 'react';
import { Card, CardHeader, CardTitle, CardContent, Button } from 'modern-ui-components';
import axios from 'axios';

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}

interface UserProfileProps {
  user: User | null;
  onLogout: () => void;
}

const UserProfile: React.FC<UserProfileProps> = ({ user, onLogout }) => {
  if (!user) {
    return null;
  }

  const handleLogout = async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        await axios.post('http://localhost:8080/api/auth/logout', { refreshToken });
      }
    } catch (error) {
      console.error('登出请求失败:', error);
    } finally {
      // 清除本地存储
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      onLogout();
    }
  };

  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <CardTitle className="flex items-center justify-between">
          <span>用户信息</span>
          <span className={`px-2 py-1 text-xs rounded-full ${
            user.role === 'ADMIN' 
              ? 'bg-red-100 text-red-800' 
              : 'bg-blue-100 text-blue-800'
          }`}>
            {user.role}
          </span>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div>
          <label className="text-sm font-medium text-gray-500">用户名</label>
          <p className="text-lg font-semibold">{user.username}</p>
        </div>
        
        <div>
          <label className="text-sm font-medium text-gray-500">邮箱</label>
          <p className="text-lg">{user.email}</p>
        </div>
        
        <div>
          <label className="text-sm font-medium text-gray-500">用户ID</label>
          <p className="text-lg font-mono">{user.id}</p>
        </div>

        <div className="pt-4 border-t">
          <Button 
            onClick={handleLogout}
            variant="outline"
            className="w-full"
          >
            退出登录
          </Button>
        </div>
      </CardContent>
    </Card>
  );
};

export default UserProfile;

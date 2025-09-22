import React, { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Button } from 'modern-ui-components';
import api from '../utils/api';

interface ApiTesterProps {
  isAuthenticated: boolean;
}

const ApiTester: React.FC<ApiTesterProps> = ({ isAuthenticated }) => {
  const [response, setResponse] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);

  const makeApiCall = async (endpoint: string, method: string = 'GET') => {
    setLoading(true);
    setResponse('');

    try {
      const response = await api.request({
        url: endpoint,
        method: method as any,
      });
      
      setResponse(JSON.stringify(response.data, null, 2));
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || '未知错误';
      setResponse(`错误: ${errorMessage}`);
    } finally {
      setLoading(false);
    }
  };

  const testEndpoints = [
    { name: '公开端点', endpoint: '/test/public', method: 'GET' },
    { name: '用户端点', endpoint: '/test/user', method: 'GET', requiresAuth: true },
    { name: '管理员端点', endpoint: '/test/admin', method: 'GET', requiresAuth: true },
  ];

  return (
    <Card className="w-full">
      <CardHeader>
        <CardTitle>API 测试</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
          {testEndpoints.map((test) => (
            <Button
              key={test.endpoint}
              onClick={() => makeApiCall(test.endpoint, test.method)}
              disabled={loading || (test.requiresAuth && !isAuthenticated)}
              variant={test.requiresAuth && !isAuthenticated ? 'outline' : 'solid'}
              className="text-sm"
            >
              {test.name}
              {test.requiresAuth && !isAuthenticated && ' (需要登录)'}
            </Button>
          ))}
        </div>

        {loading && (
          <div className="text-center py-4">
            <div className="inline-block animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
            <p className="mt-2 text-sm text-gray-500">请求中...</p>
          </div>
        )}

        {response && (
          <div className="mt-4">
            <h4 className="text-sm font-medium mb-2">响应结果:</h4>
            <pre className="bg-gray-100 p-4 rounded-md text-sm overflow-auto max-h-64">
              {response}
            </pre>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

export default ApiTester;

import axios from 'axios';
import type { AxiosInstance, AxiosError } from 'axios';

// 创建axios实例
const api: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 60000, // 60秒超时
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器 - 自动添加JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('API请求携带token:', token.substring(0, 20) + '...');
    }
    return config;
  },
  (error) => {
    console.error('请求拦截器错误:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器 - 自动处理token过期
api.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config;
    
    // 如果是401错误且不是refresh请求，尝试刷新token
    if (error.response?.status === 401 && originalRequest && !originalRequest.url?.includes('/auth/refresh')) {
      console.log('检测到401错误，尝试刷新token...');
      
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          throw new Error('没有refresh token');
        }

        const refreshResponse = await axios.post('http://localhost:8080/api/auth/refresh', {
          refreshToken: refreshToken
        });

        if (refreshResponse.data.accessToken) {
          // 更新token
          localStorage.setItem('accessToken', refreshResponse.data.accessToken);
          if (refreshResponse.data.refreshToken) {
            localStorage.setItem('refreshToken', refreshResponse.data.refreshToken);
          }

          // 重试原始请求
          originalRequest.headers.Authorization = `Bearer ${refreshResponse.data.accessToken}`;
          return api(originalRequest);
        }
      } catch (refreshError) {
        console.error('刷新token失败:', refreshError);
        
        // 清除无效token
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        
        // 可以触发重新登录
        window.location.reload();
      }
    }
    
    return Promise.reject(error);
  }
);

// 专门的SSE请求函数
export const createSseRequest = (url: string, data: any, onMessage: (data: any) => void, onComplete: () => void, onError: (error: any) => void) => {
  const token = localStorage.getItem('accessToken');
  if (!token) {
    onError(new Error('未找到访问令牌'));
    return;
  }

  console.log('创建SSE连接:', url);
  
  const eventSource = new EventSource(`${api.defaults.baseURL}${url}?${new URLSearchParams({
    ...data,
    authorization: `Bearer ${token}`
  })}`);

  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      onMessage(data);
    } catch (e) {
      console.warn('解析SSE消息失败:', event.data, e);
    }
  };

  eventSource.onerror = (error) => {
    console.log('SSE连接错误或结束:', error);
    eventSource.close();
    onComplete();
  };

  // 返回清理函数
  return () => {
    console.log('手动关闭SSE连接');
    eventSource.close();
  };
};

// 使用fetch的SSE请求（改进版本，正确处理流结束）
export const streamChatRequest = async (
  data: any, 
  onMessage: (content: string) => void, 
  onComplete: () => void,
  signal?: AbortSignal
) => {
  const token = localStorage.getItem('accessToken');
  if (!token) {
    throw new Error('未找到访问令牌');
  }

  console.log('发送SSE流式请求');
  
  let reader: ReadableStreamDefaultReader<Uint8Array> | null = null;
  let isCompleted = false;
  
  try {
    const response = await fetch(`${api.defaults.baseURL}/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify(data),
      signal: signal,
    });

    if (!response.ok) {
      if (response.status === 401 || response.status === 403) {
        throw new Error('认证失败，请重新登录');
      }
      throw new Error(`请求失败: ${response.status}`);
    }

    reader = response.body?.getReader() || null;
    if (!reader) {
      throw new Error('无法获取响应流');
    }

    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      // 检查是否被取消
      if (signal?.aborted) {
        console.log('请求被取消');
        break;
      }

      const { done, value } = await reader.read();
      
      if (done) {
        console.log('SSE流读取完成');
        break;
      }

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (line.trim() === '') continue;

        // 处理SSE事件
        if (line.startsWith('event:')) {
          const eventType = line.substring(6).trim();
          if (eventType === 'complete') {
            console.log('收到服务器完成事件');
            isCompleted = true;
            onComplete();
            return;
          }
        }

        // 处理SSE数据
        if (line.startsWith('data:')) {
          const data = line.substring(5).trim();
          
          if (data === '[DONE]') {
            console.log('收到DONE信号');
            isCompleted = true;
            onComplete();
            return;
          }

          try {
            const event = JSON.parse(data);
            
            if (event.content) {
              onMessage(event.content);
            }
            
            if (event.isComplete === true) {
              console.log('收到完成信号');
              isCompleted = true;
              onComplete();
              return;
            }
          } catch (e) {
            console.warn('解析事件失败:', data, e);
          }
        }
      }
    }
    
    // 如果循环正常结束，调用完成回调
    if (!isCompleted) {
      console.log('流正常结束');
      onComplete();
    }
    
  } catch (error) {
    console.log('流式请求结束或中断:', error);
    
    // 对于SSE请求，大部分"错误"实际上是正常的连接结束
    // 只要不是明显的认证或权限错误，都当作正常结束处理
    const errorMessage = (error as Error)?.message || '';
    const isAuthError = errorMessage.includes('401') || errorMessage.includes('403') || errorMessage.includes('Unauthorized');
    
    if (isAuthError) {
      console.error('认证错误:', error);
      throw error;
    }
    
    // 其他所有情况都当作正常的流结束处理
    console.log('流连接结束，正常结束流');
    if (!isCompleted) {
      onComplete();
    }
    return;
  } finally {
    // 确保释放reader
    if (reader) {
      try {
        reader.releaseLock();
      } catch (e) {
        console.warn('释放reader失败:', e);
      }
    }
  }
};

export default api;

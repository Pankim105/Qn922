import React, { useState, useEffect } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Button } from 'modern-ui-components';
import { MapPin, Users, Target, Star, RefreshCw, ChevronDown, ChevronUp } from 'lucide-react';

interface WorldState {
  location?: string;
  characters?: any[];
  activeQuests?: any[];
  [key: string]: any;
}

interface WorldStateDisplayProps {
  sessionId: string;
  isAuthenticated: boolean;
  onAuthFailure: () => void;
}

const WorldStateDisplay: React.FC<WorldStateDisplayProps> = ({
  sessionId,
  isAuthenticated,
  onAuthFailure
}) => {
  const [worldState, setWorldState] = useState<WorldState | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  // 获取API令牌
  const getAuthHeaders = () => {
    const token = localStorage.getItem('accessToken');
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    };
  };

  // 获取世界状态
  const fetchWorldState = async () => {
    if (!sessionId || !isAuthenticated) return;

    setIsLoading(true);
    try {
      const response = await fetch(`http://localhost:8080/api/roleplay/sessions/${sessionId}/world-state`, {
        headers: getAuthHeaders()
      });

      if (response.status === 401) {
        onAuthFailure();
        return;
      }

      if (response.ok) {
        const data = await response.json();
        if (data.success && data.data) {
          try {
            // 尝试解析为JSON，如果失败则作为字符串处理
            let parsedState;
            if (typeof data.data === 'string') {
              try {
                parsedState = JSON.parse(data.data);
              } catch (e) {
                // 如果不是JSON，直接使用字符串
                parsedState = { raw: data.data };
              }
            } else {
              parsedState = data.data;
            }
            setWorldState(parsedState);
            setLastUpdated(new Date());
          } catch (e) {
            console.warn('解析世界状态失败:', e);
            setWorldState(null);
          }
        }
      }
    } catch (error) {
      console.error('获取世界状态失败:', error);
    } finally {
      setIsLoading(false);
    }
  };

  // 当sessionId变化时获取世界状态
  useEffect(() => {
    if (sessionId && isAuthenticated) {
      fetchWorldState();
    }
  }, [sessionId, isAuthenticated]);

  // 如果没有sessionId或未认证，不显示组件
  if (!sessionId || !isAuthenticated) {
    return null;
  }

  return (
    <Card className="w-full">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm font-medium flex items-center gap-2">
            <MapPin className="w-4 h-4" />
            当前世界状态
          </CardTitle>
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={fetchWorldState}
              disabled={isLoading}
              className="h-6 w-6 p-0"
            >
              <RefreshCw className={`w-3 h-3 ${isLoading ? 'animate-spin' : ''}`} />
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setIsExpanded(!isExpanded)}
              className="h-6 w-6 p-0"
            >
              {isExpanded ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
            </Button>
          </div>
        </div>
        {lastUpdated && (
          <p className="text-xs text-muted-foreground">
            最后更新: {lastUpdated.toLocaleTimeString()}
          </p>
        )}
      </CardHeader>

      {isExpanded && (
        <CardContent className="pt-0">
          {isLoading ? (
            <div className="flex items-center justify-center py-4">
              <RefreshCw className="w-4 h-4 animate-spin text-muted-foreground" />
              <span className="ml-2 text-sm text-muted-foreground">加载中...</span>
            </div>
          ) : worldState ? (
            <div className="space-y-3">
              {/* 当前位置 */}
              {(worldState.currentLocation || worldState.location) && (
                <div className="flex items-start gap-2 p-2 rounded-lg bg-muted/50">
                  <MapPin className="w-4 h-4 text-blue-500 mt-0.5 flex-shrink-0" />
                  <div>
                    <div className="text-sm font-medium">当前位置</div>
                    <div className="text-xs text-muted-foreground">{worldState.currentLocation || worldState.location}</div>
                  </div>
                </div>
              )}

              {/* 环境信息 */}
              {worldState.environment && (
                <div className="flex items-start gap-2 p-2 rounded-lg bg-muted/50">
                  <Star className="w-4 h-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <div>
                    <div className="text-sm font-medium">环境</div>
                    <div className="text-xs text-muted-foreground">{worldState.environment}</div>
                  </div>
                </div>
              )}

              {/* NPC信息 */}
              {worldState.npcs && Array.isArray(worldState.npcs) && worldState.npcs.length > 0 && (
                <div className="flex items-start gap-2 p-2 rounded-lg bg-muted/50">
                  <Users className="w-4 h-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <div className="flex-1">
                    <div className="text-sm font-medium">NPC</div>
                    <div className="text-xs text-muted-foreground">
                      {worldState.npcs.map((npc: any) => `${npc.name}（${npc.status}）`).join(', ')}
                    </div>
                  </div>
                </div>
              )}

              {/* 角色信息 */}
              {worldState.characters && Array.isArray(worldState.characters) && worldState.characters.length > 0 && (
                <div className="flex items-start gap-2 p-2 rounded-lg bg-muted/50">
                  <Users className="w-4 h-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <div className="flex-1">
                    <div className="text-sm font-medium">角色信息</div>
                    <div className="text-xs text-muted-foreground">
                      {worldState.characters.length} 个角色
                    </div>
                  </div>
                </div>
              )}

              {/* 活跃任务 */}
              {worldState.activeQuests && Array.isArray(worldState.activeQuests) && worldState.activeQuests.length > 0 && (
                <div className="flex items-start gap-2 p-2 rounded-lg bg-muted/50">
                  <Target className="w-4 h-4 text-orange-500 mt-0.5 flex-shrink-0" />
                  <div className="flex-1">
                    <div className="text-sm font-medium">活跃任务</div>
                    <div className="text-xs text-muted-foreground">
                      {worldState.activeQuests.length} 个任务进行中
                    </div>
                  </div>
                </div>
              )}

              {/* 原始数据（如果有raw字段） */}
              {worldState.raw && (
                <div className="flex items-start gap-2 p-2 rounded-lg bg-muted/50">
                  <Star className="w-4 h-4 text-gray-500 mt-0.5 flex-shrink-0" />
                  <div>
                    <div className="text-sm font-medium">世界状态</div>
                    <div className="text-xs text-muted-foreground whitespace-pre-wrap">{worldState.raw}</div>
                  </div>
                </div>
              )}

              {/* 其他状态信息 */}
              {Object.keys(worldState).length === 0 && (
                <div className="text-center py-4 text-muted-foreground">
                  <Star className="w-8 h-8 mx-auto mb-2 opacity-50" />
                  <p className="text-sm">世界状态为空</p>
                  <p className="text-xs">开始对话以生成世界状态</p>
                </div>
              )}

              {/* 原始数据预览（开发模式） */}
              {process.env.NODE_ENV === 'development' && (
                <details className="mt-3">
                  <summary className="text-xs text-muted-foreground cursor-pointer hover:text-foreground">
                    原始数据 (开发模式)
                  </summary>
                  <pre className="text-xs bg-muted p-2 rounded mt-1 overflow-auto max-h-32">
                    {JSON.stringify(worldState, null, 2)}
                  </pre>
                </details>
              )}
            </div>
          ) : (
            <div className="text-center py-4 text-muted-foreground">
              <MapPin className="w-8 h-8 mx-auto mb-2 opacity-50" />
              <p className="text-sm">暂无世界状态</p>
              <p className="text-xs">开始对话以生成世界状态</p>
            </div>
          )}
        </CardContent>
      )}
    </Card>
  );
};

export default WorldStateDisplay;

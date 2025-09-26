import React from 'react';
import { Button } from 'modern-ui-components';
import type { WorldSelectorProps } from './types';
import { worldIcons } from './constants';

const WorldSelector: React.FC<WorldSelectorProps> = ({ 
  worlds, 
  onWorldSelect, 
  isLoading 
}) => {
  // 确保worlds是数组，如果不是则使用空数组
  const safeWorlds = Array.isArray(worlds) ? worlds : [];
  
  return (
    <div className="space-y-3">
      <p className="text-sm text-gray-600 dark:text-gray-400">选择一个世界开始冒险：</p>
      {safeWorlds.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
          {safeWorlds.map((world) => (
            <Button
              key={world.worldId}
              variant="outline"
              size="sm"
              onClick={() => onWorldSelect(world.worldId)}
              className="flex items-center gap-2 justify-start p-3 h-auto"
              disabled={isLoading}
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
      ) : (
        <div className="text-center py-8 text-gray-500 dark:text-gray-400">
          {isLoading ? '加载世界中...' : '暂无可用世界'}
        </div>
      )}
    </div>
  );
};

export default WorldSelector;

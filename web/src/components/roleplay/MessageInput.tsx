import React, { useRef, useEffect } from 'react';
import { Button } from 'modern-ui-components';
import { Send, MapPin, BookOpen, ScrollText } from 'lucide-react';
import type { MessageInputProps } from './types';

const MessageInput: React.FC<MessageInputProps> = ({
  inputMessage,
  onInputChange,
  onSendMessage,
  isLoading,
  isFreeActionMode,
  onFreeActionModeChange
}) => {
  const inputRef = useRef<HTMLInputElement>(null);

  // 聚焦输入框
  useEffect(() => {
    if (isFreeActionMode && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isFreeActionMode]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    onInputChange(value);
    
    // 用户开始输入时退出自由行动模式
    if (isFreeActionMode && value.length > 0) {
      onFreeActionModeChange(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      onSendMessage();
    }
  };

  return (
    <div className="p-4 border-t border-gray-200 dark:border-gray-700">
      {isFreeActionMode && (
        <div className="mb-2 p-2 bg-orange-50 dark:bg-orange-900/20 border border-orange-200 dark:border-orange-800 rounded-md">
          <div className="flex items-center gap-2 text-sm text-orange-700 dark:text-orange-300">
            <span>✏️</span>
            <span>自由行动模式：请详细描述你想要进行的行动</span>
          </div>
        </div>
      )}
      
      <div className="flex gap-2">
        <input
          ref={inputRef}
          type="text"
          value={inputMessage}
          onChange={handleInputChange}
          placeholder={isFreeActionMode ? "✏️ 请详细描述你想要进行的行动..." : "输入你的行动或对话..."}
          onKeyPress={handleKeyPress}
          className={`flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md 
            bg-white dark:bg-gray-800 text-gray-900 dark:text-white
            focus:outline-none focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400
            disabled:bg-gray-100 dark:disabled:bg-gray-700 disabled:cursor-not-allowed
            placeholder:text-gray-500 dark:placeholder:text-gray-400
            ${isFreeActionMode ? 'ring-2 ring-orange-400 dark:ring-orange-500 border-orange-400 dark:border-orange-500' : ''}
          `}
          disabled={isLoading}
        />
        <Button
          onClick={onSendMessage}
          disabled={isLoading || !inputMessage.trim()}
          variant="solid"
        >
          <Send className="w-4 h-4" />
        </Button>
      </div>
      
      <div className="flex gap-2 mt-2">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => onInputChange("我想查看当前状态")}
          className="text-xs"
        >
          <MapPin className="w-3 h-3 mr-1" />
          查看状态
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => onInputChange("我想学习新技能")}
          className="text-xs"
        >
          <BookOpen className="w-3 h-3 mr-1" />
          学习技能
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => onInputChange("我想查看任务")}
          className="text-xs"
        >
          <ScrollText className="w-3 h-3 mr-1" />
          查看任务
        </Button>
      </div>
    </div>
  );
};

export default MessageInput;

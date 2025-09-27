import React, { memo } from 'react';
import { Button } from 'modern-ui-components';
import { ChevronDown, User } from 'lucide-react';
import type { MessageListProps, Message } from './types';
import { aiRoleIcons, aiRoleColors, worldColors, getAIRoleName } from './constants';
import MessageSection from './MessageSection';
import GameLayout from './GameLayout';

const MessageList: React.FC<MessageListProps> = memo(({
  messages,
  visibleMessageCount,
  isLoading,
  selectedWorld,
  isDark,
  onShowMore,
  onShowAll,
  onScrollToBottom,
  hasMoreMessages,
  totalMessageCount,
  onChoiceSelect,
  onFreeActionModeChange,
  skillsState,
  structuredContent
}) => {
  // 获取消息卡片样式
  const getMessageCardStyle = (message: Message) => {
    const darkMode = isDark;

    if (message.role === 'user') {
      // 用户消息保持一致的蓝色样式，宽度适中
      return darkMode
        ? 'bg-gradient-to-r from-blue-600 to-blue-700 text-white ml-auto max-w-[40%]'
        : 'bg-gradient-to-r from-blue-100 to-blue-200 text-blue-900 ml-auto max-w-[40%]';
    } else {
      // AI消息根据角色类型使用不同颜色，宽度更宽
      const worldType = message.worldType || selectedWorld;
      const roleName = message.roleName || getAIRoleName(worldType);

      const roleColor = aiRoleColors[roleName as keyof typeof aiRoleColors];

      if (roleColor) {
        const colorScheme = darkMode ? roleColor.dark : roleColor.light;
        return `bg-gradient-to-r ${colorScheme} mr-auto max-w-[85%]`;
      } else {
        // 降级到世界类型颜色
        const worldColor = worldColors[worldType as keyof typeof worldColors];
        if (worldColor) {
          const colorScheme = darkMode ? worldColor.dark : worldColor.light;
          return `bg-gradient-to-r ${colorScheme} mr-auto max-w-[85%]`;
        } else {
          // 最终降级到默认颜色
          return darkMode
            ? 'bg-gradient-to-r from-gray-600 to-gray-700 text-white mr-auto max-w-[85%]'
            : 'bg-gradient-to-r from-gray-100 to-gray-200 text-gray-900 mr-auto max-w-[85%]';
        }
      }
    }
  };


  return (
    <div className="flex-1 overflow-y-auto space-y-3 p-4">
      {/* 显示更多消息按钮 */}
      {hasMoreMessages && (
        <div className="flex justify-center gap-2 mb-4">
          <Button
            variant="outline"
            size="sm"
            onClick={onShowMore}
            className={`transition-all duration-200 ${
              isDark 
                ? 'bg-gray-800 border-gray-600 text-gray-200 hover:bg-gray-700' 
                : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'
            }`}
          >
            查看更多 (+10条)
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={onShowAll}
            className={`transition-all duration-200 ${
              isDark 
                ? 'bg-gray-800 border-gray-600 text-gray-200 hover:bg-gray-700' 
                : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'
            }`}
          >
            显示全部 ({totalMessageCount - visibleMessageCount} 条)
          </Button>
        </div>
      )}
      
      {messages.map((message, index) => {
        // 详细的渲染日志已移除

        return (
          <div
            key={message.id}
            className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'} animate-in slide-in-from-bottom-2 duration-300`}
            style={{ animationDelay: `${index * 50}ms` }}
          >
            {message.role === 'assistant' ? (
              // 所有AI消息使用全宽GameLayout（三列布局），即使未结构化
              <div className="w-full -mx-4 px-4">
                {message.roleName && (
                  <div className="flex items-center gap-2 text-xs font-semibold mb-2 opacity-90">
                    {aiRoleIcons[message.roleName as keyof typeof aiRoleIcons]}
                    <span>{message.roleName}</span>
                  </div>
                )}
                <GameLayout
                  isDark={isDark}
                  isLoading={isLoading}
                  isAssessing={message.assessmentLoading || false}
                  onChoiceSelect={onChoiceSelect}
                  onFreeActionModeChange={onFreeActionModeChange}
                  skillsState={skillsState}
                  structuredContent={message.structuredContent || structuredContent}
                />
                <div className="text-xs opacity-70 mt-3 text-right">
                  {message.timestamp.toLocaleTimeString()}
                </div>
              </div>
            ) : (
              // 用户消息和其他消息使用原来的样式
              <div className={`rounded-lg p-4 shadow-md transition-all duration-200 hover:shadow-lg ${getMessageCardStyle(message)} max-w-[85%]`}>
                {message.role === 'user' ? (
                  <div className="flex items-center gap-2 text-xs font-semibold mb-2 opacity-90">
                    <User className="w-3 h-3" />
                    <span>你</span>
                  </div>
                ) : message.roleName && (
                  <div className="flex items-center gap-2 text-xs font-semibold mb-2 opacity-90">
                    {aiRoleIcons[message.roleName as keyof typeof aiRoleIcons]}
                    <span>{message.roleName}</span>
                  </div>
                )}
                <div className="text-sm leading-relaxed">
                  {message.isStructured && message.sections ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
                      {message.sections.map((section, sectionIndex) => (
                        <MessageSection
                          key={sectionIndex}
                          section={section}
                          index={sectionIndex}
                          isDark={isDark}
                          isLoading={isLoading}
                          onChoiceSelect={onChoiceSelect}
                          onFreeActionModeChange={onFreeActionModeChange}
                        />
                      ))}
                    </div>
                  ) : (
                    <div className="whitespace-pre-wrap">
                      {message.content}
                    </div>
                  )}
                </div>
                <div className="text-xs opacity-70 mt-3 text-right">
                  {message.timestamp.toLocaleTimeString()}
                </div>
              </div>
            )}
          </div>
        );
      })}
      
      {isLoading && (
        <div className="flex justify-start">
          <div className={`rounded-lg p-3 max-w-sm ${isDark ? 'bg-gray-700' : 'bg-gray-200'}`}>
            <div className="flex items-center gap-2">
              <div className="animate-spin rounded-full h-4 w-4 border-2 border-gray-400 border-t-transparent"></div>
              <span className={`text-sm ${isDark ? 'text-gray-400' : 'text-gray-600'}`}>
                {getAIRoleName(selectedWorld)}正在思考...
              </span>
            </div>
          </div>
        </div>
      )}
      
      {/* 滚动到底部按钮 */}
      <div className="fixed bottom-24 right-8 z-10">
        <Button
          variant="outline"
          size="sm"
          onClick={onScrollToBottom}
          className={`rounded-full shadow-lg hover:shadow-xl transition-all duration-200 ${
            isDark 
              ? 'bg-gray-800 border-gray-600 text-gray-200 hover:bg-gray-700' 
              : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'
          }`}
          title="滚动到底部"
        >
          <ChevronDown className="w-4 h-4" />
        </Button>
      </div>
    </div>
  );
});

MessageList.displayName = 'MessageList';

export default MessageList;

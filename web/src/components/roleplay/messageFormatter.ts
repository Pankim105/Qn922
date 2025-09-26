// 格式化纯文本任务内容
export const formatQuestTextContent = (content: string): string => {
  // 处理纯文本格式的任务信息
  let formattedContent = content;
  
  // 处理常见的任务状态文本
  if (content.includes('目前无活跃任务') || content.includes('暂无任务') || 
      content.includes('[无活跃任务]') || content.includes('无活跃任务')) {
    return `<div class="text-center py-4">
      <div class="text-gray-500 dark:text-gray-400 text-sm">
        <div class="w-8 h-8 mx-auto mb-2 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
          <span class="text-gray-400 dark:text-gray-500">📋</span>
        </div>
        暂无活跃任务
      </div>
    </div>`;
  }
  
  // 处理结构化的任务格式（活跃任务、已完成任务等）
  if (content.includes('**活跃任务**') || content.includes('**已完成任务**') || content.includes('活跃任务')) {
    let sections = '';
    
    // 分割不同的任务类型
    const activeMatch = content.match(/\*\*活跃任务\*\*(.*?)(?=\*\*|$)/s) || 
                       content.match(/活跃任务\s*(.*?)(?=已完成任务|$)/s);
    const completedMatch = content.match(/\*\*已完成任务\*\*(.*?)(?=\*\*|$)/s) ||
                         content.match(/已完成任务\s*(.*?)(?=活跃任务|$)/s);
    
    if (activeMatch) {
      const activeContent = activeMatch[1].trim();
      
      if (activeContent) {
        sections += `
          <div class="mb-4">
            <h5 class="font-bold text-sm mb-3 flex items-center gap-2 text-green-600 dark:text-green-400">
              <div class="w-3 h-3 rounded-full bg-green-500 shadow-sm"></div>
              <span class="text-shadow-sm">活跃任务</span>
            </h5>
            <div class="space-y-3">
              ${formatTaskItems(activeContent, 'active')}
            </div>
          </div>
        `;
      } else {
        // 没有具体任务内容时显示占位符
        sections += `
          <div class="mb-4">
            <h5 class="font-bold text-sm mb-3 flex items-center gap-2 text-green-600 dark:text-green-400">
              <div class="w-3 h-3 rounded-full bg-green-500 shadow-sm"></div>
              <span class="text-shadow-sm">活跃任务</span>
            </h5>
            <div class="text-center py-4">
              <div class="text-gray-500 dark:text-gray-400 text-sm">
                <div class="w-8 h-8 mx-auto mb-2 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
                  <span class="text-gray-400 dark:text-gray-500">📋</span>
                </div>
                暂无活跃任务
              </div>
            </div>
          </div>
        `;
      }
    }
    
    if (completedMatch) {
      const completedContent = completedMatch[1].trim();
      
      if (completedContent) {
        sections += `
          <div class="mb-4">
            <h5 class="font-bold text-sm mb-3 flex items-center gap-2 text-blue-600 dark:text-blue-400">
              <div class="w-3 h-3 rounded-full bg-blue-500 shadow-sm"></div>
              <span class="text-shadow-sm">已完成任务</span>
            </h5>
            <div class="space-y-3">
              ${formatTaskItems(completedContent, 'completed')}
            </div>
          </div>
        `;
      }
    }
    
    // 如果只有标题没有内容，显示默认消息
    if (!sections) {
      sections = `
        <div class="text-center py-4">
          <div class="text-gray-500 dark:text-gray-400 text-sm">
            <div class="w-8 h-8 mx-auto mb-2 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
              <span class="text-gray-400 dark:text-gray-500">📋</span>
            </div>
            任务信息正在加载中...
          </div>
        </div>
      `;
    }
    
    return `<div class="space-y-1">${sections}</div>`;
  }
  
  // 处理分号分隔的任务列表格式（根据PromptBuilder要求）
  if (content.includes(';')) {
    const tasks = content.split(';').map(task => task.trim()).filter(task => task);
    
    if (tasks.length > 0) {
      let sections = `
        <div class="mb-4">
          <h5 class="font-bold text-sm mb-3 flex items-center gap-2 text-green-600 dark:text-green-400">
            <div class="w-3 h-3 rounded-full bg-green-500 shadow-sm"></div>
            <span class="text-shadow-sm">活跃任务</span>
          </h5>
          <div class="space-y-3">
      `;
      
      tasks.forEach((task) => {
        // 首先尝试匹配包含冒号的格式：数字. 标题：描述，进度（奖励：...）
        const colonMatch = task.match(/^(\d+)\.\s*(.+?)：(.+?)(?:\s*（奖励：(.+)）)?$/);
        if (colonMatch) {
          const number = colonMatch[1];
          const title = colonMatch[2].trim();
          const description = colonMatch[3].trim();
          const rewards = colonMatch[4] ? colonMatch[4].trim() : '';
          
          // 创建格式化的任务文本
          let taskText = `${number}. **${title}**`;
          taskText += `\n- ${description}`;
          if (rewards) {
            taskText += `\n- 奖励：${rewards}`;
          }
          
          sections += formatSingleTask(taskText, 'active');
        } else {
          // 匹配格式：数字. 标题：描述，进度（奖励：...）
          const match = task.match(/^(\d+)\.\s*(.+?)(?:\s*-\s*(.+?))?(?:\s*（奖励：(.+)）)?$/);
          if (match) {
            const number = match[1];
            const title = match[2].trim();
            const description = match[3] ? match[3].trim() : '';
            const rewards = match[4] ? match[4].trim() : '';
            
            // 创建格式化的任务文本
            let taskText = `${number}. **${title}**`;
            if (description) {
              taskText += `\n- ${description}`;
            }
            if (rewards) {
              taskText += `\n- 奖励：${rewards}`;
            }
            
            sections += formatSingleTask(taskText, 'active');
          } else {
            // 简单格式：数字. 标题
            const simpleMatch = task.match(/^(\d+)\.\s*(.+)$/);
            if (simpleMatch) {
              const taskText = `${simpleMatch[1]}. **${simpleMatch[2].trim()}**`;
              sections += formatSingleTask(taskText, 'active');
            } else {
              sections += `<div class="text-gray-700 dark:text-gray-300">${task}</div>`;
            }
          }
        }
      });
      
      sections += `
          </div>
        </div>
      `;
      
      return `<div class="space-y-1">${sections}</div>`;
    }
  }

  // 处理新的简化任务格式（数字开头的任务列表）
  if (/^\d+\.\s/.test(content.trim())) {
    const lines = content.split('\n').map(line => line.trim()).filter(line => line);
    let sections = '';
    
    if (lines.length > 0) {
      sections += `
        <div class="mb-4">
          <h5 class="font-bold text-sm mb-3 flex items-center gap-2 text-green-600 dark:text-green-400">
            <div class="w-3 h-3 rounded-full bg-green-500 shadow-sm"></div>
            <span class="text-shadow-sm">活跃任务</span>
          </h5>
          <div class="space-y-3">
            ${formatSimplifiedTaskItems(lines)}
          </div>
        </div>
      `;
    }
    
    return `<div class="space-y-1">${sections}</div>`;
  }
  
  // 处理列表格式的任务信息
  if (content.includes('- ')) {
    formattedContent = formattedContent.replace(/^- (.+)$/gm, (_, item) => {
      return `<div class="flex items-start gap-2 py-1">
        <div class="w-1.5 h-1.5 rounded-full bg-indigo-500 mt-2 flex-shrink-0"></div>
        <span class="text-gray-700 dark:text-gray-300">${item}</span>
      </div>`;
    });
  }
  
  // 处理简单的任务标题（如纯文本"活跃任务"）
  if (content.trim() === '活跃任务' || content.trim() === '已完成任务') {
    return `
      <div class="text-center py-4">
        <div class="text-gray-500 dark:text-gray-400 text-sm">
          <div class="w-8 h-8 mx-auto mb-2 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
            <span class="text-gray-400 dark:text-gray-500">📋</span>
          </div>
          ${content.trim() === '活跃任务' ? '暂无活跃任务' : '暂无已完成任务'}
        </div>
      </div>
    `;
  }
  
  // 处理粗体文本
  formattedContent = formattedContent.replace(/\*\*(.*?)\*\*/g, '<span class="font-bold text-gray-800 dark:text-gray-200">$1</span>');
  
  return `<div class="space-y-1">${formattedContent}</div>`;
};

// 格式化简化的任务项目
const formatSimplifiedTaskItems = (lines: string[]): string => {
  let result = '';
  for (const line of lines) {
    // 解析格式：1. 任务名：任务描述（奖励：...）
    const match = line.match(/^(\d+)\.\s*([^：:]+)[：:](.+)$/);
    if (match) {
      let taskTitle = match[2].trim();
      const taskDescription = match[3].trim();
      
      // 处理任务标题中的markdown格式（去除**）
      taskTitle = taskTitle.replace(/\*\*/g, '');
      
      result += `
        <div class="border-l-4 border-green-400 bg-green-50 dark:bg-green-900/20 p-3 rounded-r-lg shadow-md">
          <div class="font-bold text-sm mb-2">${taskTitle}</div>
          <div class="text-gray-600 dark:text-gray-400 text-sm">${taskDescription}</div>
        </div>
      `;
    } else if (line.startsWith('*') && (line.includes('提示') || line.includes('奖励'))) {
      // 处理提示和奖励行，使用更淡的样式
      const content = line.replace(/^\*/, '').replace(/\*$/, '');
      result += `
        <div class="text-gray-500 dark:text-gray-400 text-xs italic ml-4 mt-1">
          ${content}
        </div>
      `;
    } else if (line.trim()) {
      // 其他非空行作为普通任务显示
      result += `
        <div class="border-l-4 border-green-400 bg-green-50 dark:bg-green-900/20 p-3 rounded-r-lg shadow-md">
          <div class="text-gray-600 dark:text-gray-400 text-sm">${line}</div>
        </div>
      `;
    }
  }
  
  return result;
};

// 格式化任务项目
const formatTaskItems = (content: string, type: 'active' | 'completed'): string => {
  // 如果内容为空，返回空结果
  if (!content || !content.trim()) {
    return '';
  }
  
  const lines = content.split('\n').map(line => line.trim()).filter(line => line);
  let result = '';
  let currentTask = '';
  
  for (const line of lines) {
    // 任务标题（数字开头的粗体文本）
    if (/^\d+\.\s*\*\*(.*?)\*\*/.test(line)) {
      if (currentTask) {
        result += formatSingleTask(currentTask, type);
      }
      currentTask = line;
    } else if (line.startsWith('-') && currentTask) {
      // 处理任务详情行
      currentTask += '\n' + line;
    } else if (line.startsWith('-') && !currentTask) {
      // 如果没有当前任务但找到了任务详情，可能是格式问题
      // 忽略孤立的详情行
    } else if (currentTask) {
      // 其他内容也添加到当前任务中
      currentTask += '\n' + line;
    }
  }
  
  // 处理最后一个任务
  if (currentTask) {
    result += formatSingleTask(currentTask, type);
  }
  
  return result;
};

// 格式化单个任务
const formatSingleTask = (taskText: string, type: 'active' | 'completed'): string => {
  const lines = taskText.split('\n');
  const titleMatch = lines[0].match(/^\d+\.\s*\*\*(.*?)\*\*/);
  const title = titleMatch ? titleMatch[1] : '未命名任务';
  
  let details = '';
  for (let i = 1; i < lines.length; i++) {
    const line = lines[i].trim();
    if (line.startsWith('- ')) {
      const detail = line.substring(2);
      details += `<div class="text-gray-600 dark:text-gray-400 text-sm mb-1">${detail}</div>`;
    }
  }
  
  const borderColor = type === 'active' ? 'border-green-400' : 'border-blue-400';
  const bgColor = type === 'active' ? 'bg-green-50 dark:bg-green-900/20' : 'bg-blue-50 dark:bg-blue-900/20';
  
  const result = `
    <div class="border-l-4 ${borderColor} ${bgColor} p-3 rounded-r-lg shadow-md">
      <div class="font-bold text-sm mb-2">${title}</div>
      ${details}
    </div>
  `;
  
  return result;
};

// 格式化任务内容 - 游戏UI风格
export const formatQuestContent = (questData: any): string => {
  if (!questData) return '暂无任务信息';

  const formatQuestList = (quests: any[], title: string, type: string) => {
    if (!quests || quests.length === 0) return '';

    const getTypeColor = (type: string) => {
      switch (type) {
        case 'activeQuests': return 'border-l-green-400 bg-green-900/20 dark:bg-green-900/30';
        case 'completed': return 'border-l-blue-400 bg-blue-900/20 dark:bg-blue-900/30';
        case 'created': return 'border-l-purple-400 bg-purple-900/20 dark:bg-purple-900/30';
        case 'expired': return 'border-l-red-400 bg-red-900/20 dark:bg-red-900/30';
        default: return 'border-l-gray-400 bg-gray-900/20 dark:bg-gray-900/30';
      }
    };

    return `
      <div class="mb-4">
        <h5 class="font-bold text-sm mb-3 flex items-center gap-2">
          <div class="w-3 h-3 rounded-full bg-current shadow-sm"></div>
          <span class="text-shadow-sm">${title}</span>
        </h5>
        <div class="space-y-3">
          ${quests.map((quest, idx) => `
            <div class="border-l-4 ${getTypeColor(type)} p-3 rounded-r-lg shadow-md">
              <div class="font-bold text-sm mb-2">${idx + 1}. ${quest.title || quest.description || '未命名任务'}</div>
              ${quest.description ? `<div class="text-gray-500 dark:text-gray-300 mb-2 text-sm">${quest.description}</div>` : ''}
              <div class="flex flex-wrap gap-3 text-sm">
                ${quest.progress ? `<span class="font-semibold text-green-600 dark:text-green-400 bg-green-100 dark:bg-green-900/30 px-2 py-1 rounded">进度: ${quest.progress}</span>` : ''}
                ${quest.rewards ? `<span class="text-gray-600 dark:text-gray-400 bg-gray-100 dark:bg-gray-800/30 px-2 py-1 rounded">奖励: ${JSON.stringify(quest.rewards).replace(/"/g, '')}</span>` : ''}
                ${quest.status ? `<span class="px-2 py-1 rounded text-sm font-bold ${quest.status === 'ACTIVE' ? 'bg-green-200 dark:bg-green-800 text-green-800 dark:text-green-200 shadow-sm' : quest.status === 'COMPLETED' ? 'bg-blue-200 dark:bg-blue-800 text-blue-800 dark:text-blue-200 shadow-sm' : 'bg-gray-200 dark:bg-gray-800 text-gray-800 dark:text-gray-200 shadow-sm'}"">${quest.status}</span>` : ''}
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  };

  let content = '<div class="space-y-1">';

  if (questData.activeQuests && questData.activeQuests.length > 0) {
    content += formatQuestList(questData.activeQuests, '活跃任务', 'activeQuests');
  }

  if (questData.completed && questData.completed.length > 0) {
    content += formatQuestList(questData.completed, '已完成任务', 'completed');
  }

  if (questData.created && questData.created.length > 0) {
    content += formatQuestList(questData.created, '新任务', 'created');
  }

  if (questData.expired && questData.expired.length > 0) {
    content += formatQuestList(questData.expired, '过期任务', 'expired');
  }

  content += '</div>';
  return content || '暂无任务信息';
};

// 格式化状态内容 - 处理分号分隔的键值对格式
export const formatStatusContent = (content: string): string => {
  // 处理分号分隔的键值对格式
  let formattedContent = content;

  // 首先尝试按分号分割键值对
  if (content.includes(';')) {
    const keyValuePairs = content.split(';').map(pair => pair.trim()).filter(pair => pair);
    
    if (keyValuePairs.length > 0) {
      formattedContent = keyValuePairs.map((pair, index) => {
        const isLast = index === keyValuePairs.length - 1;
        
        // 匹配 **键**: 值 格式
        const match = pair.match(/^\*\*([^*]+)\*\*:\s*(.+)$/);
        if (match) {
          const key = match[1];
          const value = match[2];
          
          // 添加表情符号支持
          const keyWithEmoji = key.replace(/^(📍|🌅|🌤️|🔮|👥|⚡|📚)?\s*/, '');
          const emoji = key.match(/^(📍|🌅|🌤️|🔮|👥|⚡|📚)/)?.[0] || '';

          return `<div class="flex justify-between items-start py-2 ${isLast ? '' : 'border-b'} border-gray-200/30 dark:border-gray-600/30 ${isLast ? '' : 'last:border-b-0'}">
            <span class="font-bold text-gray-800 dark:text-gray-200 flex-shrink-0">
              ${emoji ? `<span class="mr-1">${emoji}</span>` : ''}${keyWithEmoji}:
            </span>
            <span class="text-gray-600 dark:text-gray-400 text-right ml-2">${value}</span>
          </div>`;
        }
        
        // 匹配表情符号开头的格式：📍 键: 值
        const emojiMatch = pair.match(/^(📍|🌅|🌤️|🔮|👥|⚡|📚)\s*([^:]+):\s*(.+)$/);
        if (emojiMatch) {
          const emoji = emojiMatch[1];
          const key = emojiMatch[2].trim();
          const value = emojiMatch[3].trim();

          return `<div class="flex justify-between items-start py-2 ${isLast ? '' : 'border-b'} border-gray-200/30 dark:border-gray-600/30 ${isLast ? '' : 'last:border-b-0'}">
            <span class="font-bold text-gray-800 dark:text-gray-200 flex-shrink-0">
              <span class="mr-1">${emoji}</span>${key}:
            </span>
            <span class="text-gray-600 dark:text-gray-400 text-right ml-2">${value}</span>
          </div>`;
        }
        
        // 如果没有匹配到标准格式，尝试其他格式
        // 格式2: 键：值（中文冒号）
        const chineseMatch = pair.match(/^([^：]+)：(.+)$/);
        if (chineseMatch) {
          const key = chineseMatch[1];
          const value = chineseMatch[2];

          return `<div class="flex justify-between items-start py-2 ${isLast ? '' : 'border-b'} border-gray-200/30 dark:border-gray-600/30 ${isLast ? '' : 'last:border-b-0'}">
            <span class="font-bold text-gray-800 dark:text-gray-200 flex-shrink-0">${key}：</span>
            <span class="text-gray-600 dark:text-gray-400 text-right ml-2">${value}</span>
          </div>`;
        }
        
        // 如果都不匹配，作为普通文本处理
        return `<div class="flex items-start gap-2 py-1">
          <div class="w-1.5 h-1.5 rounded-full bg-blue-500 mt-2 flex-shrink-0"></div>
          <span class="text-gray-700 dark:text-gray-300">${pair}</span>
        </div>`;
      }).join('');
    }
  } else {
    // 如果没有分号，尝试按行分割
    const lines = content.split('\n').filter(line => line.trim());
    
    formattedContent = lines.map((line, index) => {
      const isLast = index === lines.length - 1;
      
      // 匹配 **键**: 值 格式
      const match = line.match(/^\*\*([^*]+)\*\*:\s*(.+)$/);
      if (match) {
        const key = match[1];
        const value = match[2];
        
        // 添加表情符号支持
        const keyWithEmoji = key.replace(/^(📍|🌅|🌤️|🔮|👥|⚡|📚)?\s*/, '');
        const emoji = key.match(/^(📍|🌅|🌤️|🔮|👥|⚡|📚)/)?.[0] || '';

        return `<div class="flex justify-between items-start py-2 ${isLast ? '' : 'border-b'} border-gray-200/30 dark:border-gray-600/30 ${isLast ? '' : 'last:border-b-0'}">
          <span class="font-bold text-gray-800 dark:text-gray-200 flex-shrink-0">
            ${emoji ? `<span class="mr-1">${emoji}</span>` : ''}${keyWithEmoji}:
          </span>
          <span class="text-gray-600 dark:text-gray-400 text-right ml-2">${value}</span>
        </div>`;
      }
      
      // 格式2: 键：值（中文冒号）
      const chineseMatch = line.match(/^([^：]+)：(.+)$/);
      if (chineseMatch) {
        const key = chineseMatch[1];
        const value = chineseMatch[2];

        return `<div class="flex justify-between items-start py-2 ${isLast ? '' : 'border-b'} border-gray-200/30 dark:border-gray-600/30 ${isLast ? '' : 'last:border-b-0'}">
          <span class="font-bold text-gray-800 dark:text-gray-200 flex-shrink-0">${key}：</span>
          <span class="text-gray-600 dark:text-gray-400 text-right ml-2">${value}</span>
        </div>`;
      }
      
      // 如果都不匹配，作为普通文本处理
      return `<div class="flex items-start gap-2 py-1">
        <div class="w-1.5 h-1.5 rounded-full bg-blue-500 mt-2 flex-shrink-0"></div>
        <span class="text-gray-700 dark:text-gray-300">${line}</span>
      </div>`;
    }).join('');
  }
  
  // 处理列表项（以 - 开头）
  formattedContent = formattedContent.replace(/^- (.+)$/gm, (_, item) => {
    // 检查是否是键值对格式（如 "等级：1"）
    if (item.includes('：')) {
      const [key, value] = item.split('：', 2);
      return `<div class="flex justify-between items-start py-2 border-b border-gray-200/30 dark:border-gray-600/30 last:border-b-0">
        <span class="font-bold text-gray-800 dark:text-gray-200 flex-shrink-0">${key}：</span>
        <span class="text-gray-600 dark:text-gray-400 text-right ml-2">${value}</span>
      </div>`;
    } else {
      // 普通列表项
      return `<div class="flex items-start gap-2 py-1">
        <div class="w-1.5 h-1.5 rounded-full bg-blue-500 mt-2 flex-shrink-0"></div>
        <span class="text-gray-700 dark:text-gray-300">${item}</span>
      </div>`;
    }
  });
  
  // 处理粗体文本（**text**）- 但排除已处理的键值对
  formattedContent = formattedContent.replace(/\*\*([^*]+)\*\*(?!:)/g, '<span class="font-bold text-gray-800 dark:text-gray-200">$1</span>');
  
  // 处理行内代码（`code`）
  formattedContent = formattedContent.replace(/`(.*?)`/g, '<code class="bg-gray-200 dark:bg-gray-700 px-1 py-0.5 rounded text-xs font-mono text-gray-800 dark:text-gray-200">$1</code>');
  
  // 处理特殊格式的括号内容（如 "（带暗格）"）
  formattedContent = formattedContent.replace(/（([^）]+)）/g, '<span class="text-gray-500 dark:text-gray-400 text-xs">（$1）</span>');
  
  return `<div class="space-y-0">${formattedContent}</div>`;
};

// 格式化选择内容 - 处理分号分隔的选择项格式
export const formatChoicesContent = (content: string): string => {
  // 处理分号分隔的选择项格式
  let formattedContent = content;

  // 首先尝试按分号分割选择项
  if (content.includes(';')) {
    const choiceItems = content.split(';').map(item => item.trim()).filter(item => item);
    
    if (choiceItems.length > 0) {
      formattedContent = choiceItems.map((item, _index) => {
        // 匹配 数字. **标题** - 描述 格式
        const match = item.match(/^(\d+)\.\s*\*\*(.*?)\*\*\s*-\s*(.+)$/);
        if (match) {
          const title = match[2];
          const description = match[3];

          return `<div class="flex items-start gap-2 py-1">
            <div class="w-1.5 h-1.5 rounded-full bg-purple-500 mt-2 flex-shrink-0"></div>
            <div class="flex-1">
              <div class="font-bold text-gray-800 dark:text-gray-200">${title}</div>
              <div class="text-sm text-gray-600 dark:text-gray-400 mt-1">${description}</div>
            </div>
          </div>`;
        }

        // 匹配 **标题** - 描述 格式（没有序号）
        const noNumberMatch = item.match(/^\*\*(.*?)\*\*\s*-\s*(.+)$/);
        if (noNumberMatch) {
          const title = noNumberMatch[1];
          const description = noNumberMatch[2];

          return `<div class="flex items-start gap-2 py-1">
            <div class="w-1.5 h-1.5 rounded-full bg-purple-500 mt-2 flex-shrink-0"></div>
            <div class="flex-1">
              <div class="font-bold text-gray-800 dark:text-gray-200">${title}</div>
              <div class="text-sm text-gray-600 dark:text-gray-400 mt-1">${description}</div>
            </div>
          </div>`;
        }

        // 如果都不匹配，作为普通文本处理
        return `<div class="flex items-start gap-2 py-1">
          <div class="w-1.5 h-1.5 rounded-full bg-purple-500 mt-2 flex-shrink-0"></div>
          <span class="text-gray-700 dark:text-gray-300">${item}</span>
        </div>`;
      }).join('');
    }
  } else {
    // 如果没有分号，尝试按行分割
    const lines = content.split('\n').filter(line => line.trim());
    
    formattedContent = lines.map((line, _index) => {
      // 匹配 数字. **标题** - 描述 格式
      const match = line.match(/^(\d+)\.\s*\*\*(.*?)\*\*\s*-\s*(.+)$/);
      if (match) {
        const title = match[2];
        const description = match[3];

        return `<div class="flex items-start gap-2 py-1">
          <div class="w-1.5 h-1.5 rounded-full bg-purple-500 mt-2 flex-shrink-0"></div>
          <div class="flex-1">
            <div class="font-bold text-gray-800 dark:text-gray-200">${title}</div>
            <div class="text-sm text-gray-600 dark:text-gray-400 mt-1">${description}</div>
          </div>
        </div>`;
      }

      // 匹配 **标题** - 描述 格式（没有序号）
      const noNumberMatch = line.match(/^\*\*(.*?)\*\*\s*-\s*(.+)$/);
      if (noNumberMatch) {
        const title = noNumberMatch[1];
        const description = noNumberMatch[2];

        return `<div class="flex items-start gap-2 py-1">
          <div class="w-1.5 h-1.5 rounded-full bg-purple-500 mt-2 flex-shrink-0"></div>
          <div class="flex-1">
            <div class="font-bold text-gray-800 dark:text-gray-200">${title}</div>
            <div class="text-sm text-gray-600 dark:text-gray-400 mt-1">${description}</div>
          </div>
        </div>`;
      }

      // 如果都不匹配，作为普通文本处理
      return `<div class="flex items-start gap-2 py-1">
        <div class="w-1.5 h-1.5 rounded-full bg-purple-500 mt-2 flex-shrink-0"></div>
        <span class="text-gray-700 dark:text-gray-300">${line}</span>
      </div>`;
    }).join('');
  }

  // 处理粗体文本（**text**）- 但排除已处理的选择项
  formattedContent = formattedContent.replace(/\*\*([^*]+)\*\*(?!:)/g, '<span class="font-bold text-gray-800 dark:text-gray-200">$1</span>');

  return `<div class="space-y-0">${formattedContent}</div>`;
};

// 格式化纯文本评估内容
export const formatAssessmentTextContent = (content: string): string => {
  // 处理纯文本格式的评估信息
  let formattedContent = content;
  
  // 处理常见的评估文本格式
  if (content.includes('综合评分:') || content.includes('评估策略:')) {
    // 处理键值对格式
    formattedContent = formattedContent.replace(/^(.+?):\s*(.+)$/gm, (_, key, value) => {
      return `<div class="flex justify-between items-center py-2 border-b border-gray-200/30 dark:border-gray-600/30 last:border-b-0">
        <span class="font-bold text-gray-800 dark:text-gray-200">${key}:</span>
        <span class="text-gray-600 dark:text-gray-400">${value}</span>
      </div>`;
    });
  }
  
  // 处理粗体文本
  formattedContent = formattedContent.replace(/\*\*(.*?)\*\*/g, '<span class="font-bold text-gray-800 dark:text-gray-200">$1</span>');
  
  // 处理列表项
  if (content.includes('- ')) {
    formattedContent = formattedContent.replace(/^- (.+)$/gm, (_, item) => {
      return `<div class="flex items-start gap-2 py-1">
        <div class="w-1.5 h-1.5 rounded-full bg-gray-500 mt-2 flex-shrink-0"></div>
        <span class="text-gray-700 dark:text-gray-300">${item}</span>
      </div>`;
    });
  }
  
  return `<div class="space-y-1">${formattedContent}</div>`;
};

// 格式化评估内容 - 游戏UI风格
export const formatAssessmentContent = (assessmentData: any): string => {
  if (!assessmentData) return '';

  let content = `<div class="space-y-2">`;

  // 主要评分信息
  if (assessmentData.overallScore !== undefined || assessmentData.strategy) {
    content += `
      <div class="grid grid-cols-2 gap-3">
        ${assessmentData.overallScore !== undefined ? `
          <div class="bg-gradient-to-br from-blue-100/90 to-blue-50/70 dark:from-blue-900/40 dark:to-blue-800/30 p-3 rounded-lg border border-blue-300/50 dark:border-blue-600/50 shadow-md text-center">
            <div class="text-sm font-bold text-blue-600 dark:text-blue-400 mb-2">综合评分</div>
            <div class="text-lg font-bold ${(assessmentData.overallScore >= 0.8 ? 'text-green-600 dark:text-green-400' : assessmentData.overallScore >= 0.6 ? 'text-yellow-600 dark:text-yellow-400' : 'text-red-600 dark:text-red-400')}">
              ${Math.round(assessmentData.overallScore * 100)}%
            </div>
          </div>
        ` : ''}

        ${assessmentData.strategy ? `
          <div class="bg-gradient-to-br from-purple-100/90 to-purple-50/70 dark:from-purple-900/40 dark:to-purple-800/30 p-3 rounded-lg border border-purple-300/50 dark:border-purple-600/50 shadow-md text-center">
            <div class="text-sm font-bold text-purple-600 dark:text-purple-400 mb-2">评估策略</div>
            <div class="text-sm font-bold px-2 py-1 rounded ${
              assessmentData.strategy === 'ACCEPT' ? 'bg-green-200 dark:bg-green-800 text-green-800 dark:text-green-200 shadow-sm' :
              assessmentData.strategy === 'ADJUST' ? 'bg-yellow-200 dark:bg-yellow-800 text-yellow-800 dark:text-yellow-200 shadow-sm' :
              'bg-red-200 dark:bg-red-800 text-red-800 dark:text-red-200 shadow-sm'
            }">
              ${assessmentData.strategy}
            </div>
          </div>
        ` : ''}
      </div>
    `;
  }

  // 评估说明
  if (assessmentData.assessmentNotes) {
    content += `
      <div class="bg-gradient-to-br from-gray-100/90 to-gray-50/70 dark:from-gray-800/50 dark:to-gray-700/40 p-3 rounded-lg border border-gray-300/50 dark:border-gray-600/50 shadow-md">
        <div class="text-sm font-bold mb-2 text-gray-700 dark:text-gray-300">评估说明</div>
        <div class="text-sm text-gray-600 dark:text-gray-400 leading-relaxed">
          ${assessmentData.assessmentNotes}
        </div>
      </div>
    `;
  }

  // 建议行动
  if (assessmentData.suggestedActions && assessmentData.suggestedActions.length > 0) {
    content += `
      <div class="bg-gradient-to-br from-amber-100/90 to-amber-50/70 dark:from-amber-900/40 dark:to-amber-800/30 p-3 rounded-lg border border-amber-300/50 dark:border-amber-600/50 shadow-md">
        <div class="text-sm font-bold mb-2 text-amber-700 dark:text-amber-400">建议行动</div>
        <div class="space-y-2">
          ${assessmentData.suggestedActions.map((action: string) => `
            <div class="text-sm flex items-start gap-2">
              <div class="w-2 h-2 rounded-full bg-amber-500 mt-1.5 flex-shrink-0 shadow-sm"></div>
              <span class="text-gray-700 dark:text-gray-300">${action}</span>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }

  // 收敛提示
  if (assessmentData.convergenceHints && assessmentData.convergenceHints.length > 0) {
    content += `
      <div class="bg-gradient-to-br from-indigo-100/90 to-indigo-50/70 dark:from-indigo-900/40 dark:to-indigo-800/30 p-3 rounded-lg border border-indigo-300/50 dark:border-indigo-600/50 shadow-md">
        <div class="text-sm font-bold mb-2 text-indigo-700 dark:text-indigo-400">收敛提示</div>
        <div class="space-y-2">
          ${assessmentData.convergenceHints.map((hint: string) => `
            <div class="text-sm flex items-start gap-2">
              <div class="w-2 h-2 rounded-full bg-indigo-500 mt-1.5 flex-shrink-0 shadow-sm"></div>
              <span class="text-gray-700 dark:text-gray-300">${hint}</span>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }

  content += `</div>`;
  
  // 检查是否有实际内容，如果没有则返回空字符串
  const hasContent = assessmentData.overallScore !== undefined || 
                    assessmentData.strategy || 
                    assessmentData.assessmentNotes ||
                    (assessmentData.suggestedActions && assessmentData.suggestedActions.length > 0) ||
                    (assessmentData.convergenceHints && assessmentData.convergenceHints.length > 0);
  
  return hasContent ? content : '';
};

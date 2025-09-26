import type { MessageSection } from './types';
import { MESSAGE_PARSER_PATTERNS, ASSESSMENT_PATTERNS } from './constants';

// 解析结构化消息内容 - 支持后端AI返回格式
export const parseStructuredMessage = (content: string): MessageSection[] => {
  const sections: MessageSection[] = [];

  // 使用统一的结构化内容解析模式
  const patterns = MESSAGE_PARSER_PATTERNS;

  let remainingContent = content;
  let hasStructuredContent = false;

  // 提取结构化内容
  for (const pattern of patterns) {
    const matches = [...content.matchAll(pattern.regex)];
    
    for (const match of matches) {
      const sectionContent = match[1].trim();

      sections.push({
        type: pattern.type as MessageSection['type'],
        content: sectionContent,
        title: pattern.title
      });

      // 从剩余内容中移除已匹配的部分
      remainingContent = remainingContent.replace(match[0], '');
      hasStructuredContent = true;
    }
  }
  
  // 清理剩余内容中的多余星号和空白字符
  remainingContent = remainingContent.replace(/^\*+\s*$/gm, '').trim();

  // 处理评估JSON
  const assessmentMatch = content.match(ASSESSMENT_PATTERNS.full);
  if (assessmentMatch) {
    try {
      const assessmentData = JSON.parse(assessmentMatch[1]);
      sections.push({
        type: 'assessment',
        content: JSON.stringify(assessmentData, null, 2),
        title: '系统评估'
      });
      remainingContent = remainingContent.replace(assessmentMatch[0], '');
      hasStructuredContent = true;
    } catch (e) {
      console.warn('❌ [消息解析] 解析评估JSON失败:', e);
    }
  }

  // 如果有剩余的普通内容，添加为plain类型
  let plainContent = remainingContent.trim();
  
  // 清理多余的星号（可能是解析过程中留下的）
  plainContent = plainContent.replace(/^\*+\s*$/, '').trim();
  
  if (plainContent) {
    sections.unshift({
      type: 'plain',
      content: plainContent
    });
  }

  // 如果没有结构化内容，整个消息作为plain处理
  if (!hasStructuredContent) {
    return [{
      type: 'plain',
      content: content.trim()
    }];
  }

  // 过滤掉只包含星号或空内容的卡片
  const filteredSections = sections.filter(section => {
    if (!section.content || section.content.trim() === '') {
      return false;
    }
    // 检查是否只包含星号
    if (/^\*+\s*$/.test(section.content.trim())) {
      return false;
    }
    return true;
  });
  
  return filteredSections;
};

// 检查内容是否为空或只有空白字符
export const isContentEmpty = (content: string): boolean => {
  if (!content) return true;
  const trimmed = content.trim();
  if (!trimmed) return true;
  
  // 检查是否只包含常见的空内容标识
  const emptyPatterns = [
    '暂无评估信息',
    '暂无活跃任务',
    '任务信息正在加载中...',
    '[无活跃任务]',
    '无活跃任务',
    '目前无活跃任务',
    '暂无任务',
    '评估信息正在加载中',
    '{}', // 空的JSON对象
    '[]', // 空的JSON数组
    'null',
    'undefined'
  ];
  
  // 检查是否匹配空内容模式
  if (emptyPatterns.some(pattern => trimmed === pattern)) {
    return true;
  }
  
  // 检查是否是空的JSON对象或数组
  if (trimmed === '{}' || trimmed === '[]' || trimmed === 'null') {
    return true;
  }
  
  // 检查是否只包含空白字符和标点符号
  if (trimmed.length < 3 && /^[\s\.,;:!?\-_]*$/.test(trimmed)) {
    return true;
  }
  
  // 检查是否只包含星号
  if (/^\*+\s*$/.test(trimmed)) {
    return true;
  }
  
  return false;
};

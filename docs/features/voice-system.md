# 语音交互系统

QN Contest的语音交互系统提供完整的语音输入和识别功能，支持多语言语音交互，为用户提供更自然的交互体验。

## 系统概述

语音交互系统基于Web Speech API实现，包含前端语音识别组件和后端语音处理服务，支持实时语音识别、多语言处理和智能指令解析。

## 核心特性

### 🎤 语音识别
- **实时识别**: 基于Web Speech API的实时语音识别
- **多语言支持**: 支持中文、英文等多种语言
- **高准确率**: 优化的语音识别算法，提供高准确率识别
- **流式处理**: 支持流式语音识别，实时反馈识别结果

### 🧠 智能处理
- **指令解析**: 智能语音指令标准化处理
- **上下文理解**: 结合世界类型进行上下文优化
- **错误处理**: 完善的语音识别错误处理机制
- **超时控制**: 智能超时控制，避免长时间等待

### 🎯 用户体验
- **直观界面**: 简洁直观的语音输入界面
- **状态反馈**: 清晰的录音状态和错误提示
- **权限管理**: 智能的麦克风权限管理
- **兼容性**: 良好的浏览器兼容性支持

## 技术实现

### 前端实现

#### VoiceInput组件
```tsx
interface VoiceInputProps {
  onVoiceResult: (text: string) => void;
  onError: (error: string) => void;
  disabled?: boolean;
  language?: string;
}
```

**核心功能**:
- 语音识别初始化
- 实时语音处理
- 错误状态管理
- 用户界面交互

#### 语音识别配置
```typescript
// 语音识别配置
recognitionInstance.continuous = false;        // 非连续识别
recognitionInstance.interimResults = true;     // 显示中间结果
recognitionInstance.lang = language;           // 设置语言
recognitionInstance.maxAlternatives = 1;       // 最大备选结果数
```

### 后端实现

#### VoiceController
```java
@RestController
@RequestMapping("/voice")
public class VoiceController {
    
    @PostMapping("/recognize")
    public ResponseEntity<ChatResponse> recognizeVoice(
        @RequestParam("audio") MultipartFile audioFile,
        @RequestParam(value = "language", defaultValue = "zh-CN") String language,
        @RequestParam(value = "worldType", required = false) String worldType,
        @RequestParam(value = "sessionId", required = false) String sessionId
    );
}
```

#### VoiceInstructionParser
```java
@Service
public class VoiceInstructionParser {
    
    public String parseVoiceInstruction(String voiceText, String worldType, String sessionId);
    public boolean isValidVoiceInstruction(String voiceText);
}
```

## 使用指南

### 基础使用

#### 1. 语音输入组件
```tsx
import VoiceInput from './VoiceInput';

function ChatInterface() {
  const handleVoiceResult = (text: string) => {
    console.log('语音识别结果:', text);
    // 处理识别结果
  };

  const handleVoiceError = (error: string) => {
    console.error('语音识别错误:', error);
    // 处理错误
  };

  return (
    <VoiceInput
      onVoiceResult={handleVoiceResult}
      onError={handleVoiceError}
      language="zh-CN"
    />
  );
}
```

#### 2. 语音识别API调用
```javascript
// 上传音频文件进行语音识别
const formData = new FormData();
formData.append('audio', audioFile);
formData.append('language', 'zh-CN');
formData.append('worldType', 'fantasy_adventure');

const response = await fetch('/api/voice/recognize', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`
  },
  body: formData
});
```

### 高级配置

#### 1. 多语言支持
```tsx
// 支持的语言列表
const supportedLanguages = {
  'zh-CN': '中文（简体）',
  'en-US': 'English (US)',
  'ja-JP': '日本語',
  'ko-KR': '한국어'
};

<VoiceInput
  language="zh-CN"
  onVoiceResult={handleVoiceResult}
  onError={handleVoiceError}
/>
```

#### 2. 错误处理
```tsx
const handleVoiceError = (error: string) => {
  switch (error) {
    case 'no-speech':
      showMessage('没有检测到语音，请重试');
      break;
    case 'audio-capture':
      showMessage('无法访问麦克风，请检查权限');
      break;
    case 'not-allowed':
      showMessage('麦克风权限被拒绝，请在浏览器设置中允许');
      break;
    default:
      showMessage(`语音识别错误: ${error}`);
  }
};
```

## 错误处理

### 常见错误类型

| 错误类型 | 说明 | 解决方案 |
|---------|------|---------|
| `no-speech` | 没有检测到语音 | 检查麦克风是否正常工作，重新录音 |
| `audio-capture` | 无法访问麦克风 | 检查麦克风权限设置 |
| `not-allowed` | 权限被拒绝 | 在浏览器设置中允许麦克风权限 |
| `network` | 网络错误 | 检查网络连接状态 |
| `aborted` | 识别被中断 | 重新开始语音识别 |

### 错误处理最佳实践

1. **权限检查**: 在开始语音识别前检查麦克风权限
2. **超时控制**: 设置合理的超时时间，避免长时间等待
3. **用户反馈**: 提供清晰的错误信息和解决建议
4. **降级处理**: 在语音识别失败时提供文本输入备选方案

## 性能优化

### 前端优化
- **硬件加速**: 使用CSS3硬件加速提升动画性能
- **内存管理**: 及时清理语音识别实例，避免内存泄漏
- **状态管理**: 优化组件状态更新，减少不必要的重新渲染

### 后端优化
- **异步处理**: 使用异步处理提升响应速度
- **缓存机制**: 缓存常用语音识别结果
- **负载均衡**: 支持多实例部署，提升并发处理能力

## 浏览器兼容性

### 支持的浏览器
- **Chrome**: 25+ ✅
- **Edge**: 79+ ✅
- **Safari**: 14.1+ ✅
- **Firefox**: 不支持 ❌

### 兼容性处理
```tsx
// 检查浏览器支持
const isSupported = () => {
  return 'SpeechRecognition' in window || 'webkitSpeechRecognition' in window;
};

if (!isSupported()) {
  return <div>您的浏览器不支持语音识别功能</div>;
}
```

## 安全考虑

### 隐私保护
- **本地处理**: 语音识别在浏览器本地进行，不发送到服务器
- **权限控制**: 严格的麦克风权限管理
- **数据清理**: 及时清理语音数据，不进行持久化存储

### 安全措施
- **HTTPS要求**: 语音识别功能需要HTTPS环境
- **权限验证**: 后端接口需要用户认证
- **输入验证**: 严格的输入验证和清理

## 未来规划

### 短期目标
- [ ] 支持更多语言
- [ ] 提升识别准确率
- [ ] 优化错误处理机制

### 中期目标
- [ ] 语音合成（TTS）支持
- [ ] 离线语音识别
- [ ] 语音情感识别

### 长期目标
- [ ] 多语言实时翻译
- [ ] 语音指令自定义
- [ ] 语音角色扮演

## 故障排除

### 常见问题

#### 1. 语音识别不工作
**可能原因**:
- 浏览器不支持Web Speech API
- 麦克风权限被拒绝
- 网络连接问题

**解决方案**:
- 检查浏览器兼容性
- 检查麦克风权限设置
- 检查网络连接状态

#### 2. 识别准确率低
**可能原因**:
- 环境噪音过大
- 说话声音过小
- 语言设置不匹配

**解决方案**:
- 选择安静的环境
- 调整说话音量
- 确认语言设置正确

#### 3. 识别延迟高
**可能原因**:
- 网络延迟
- 浏览器性能问题
- 服务器负载过高

**解决方案**:
- 检查网络连接
- 关闭不必要的浏览器标签页
- 联系技术支持

---

**文档版本**: v1.0  
**最后更新**: 2025-01-27  
**维护者**: QN Contest Team


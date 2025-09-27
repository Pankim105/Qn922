import React, { useState, useRef, useEffect } from 'react';
import { Button } from 'modern-ui-components';
import { Mic, Volume2, VolumeX, Square, Play, Check, X } from 'lucide-react';

// 扩展Window接口以支持语音识别
declare global {
  interface Window {
    SpeechRecognition: typeof SpeechRecognition;
    webkitSpeechRecognition: typeof SpeechRecognition;
  }
}

// 语音识别接口声明
interface SpeechRecognition extends EventTarget {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  maxAlternatives: number;
  start(): void;
  stop(): void;
  onresult: ((event: SpeechRecognitionEvent) => void) | null;
  onerror: ((event: SpeechRecognitionErrorEvent) => void) | null;
  onend: (() => void) | null;
}

interface SpeechRecognitionEvent extends Event {
  resultIndex: number;
  results: SpeechRecognitionResultList;
}

interface SpeechRecognitionResultList {
  length: number;
  item(index: number): SpeechRecognitionResult;
  [index: number]: SpeechRecognitionResult;
}

interface SpeechRecognitionResult {
  isFinal: boolean;
  length: number;
  item(index: number): SpeechRecognitionAlternative;
  [index: number]: SpeechRecognitionAlternative;
}

interface SpeechRecognitionAlternative {
  transcript: string;
  confidence: number;
}

interface SpeechRecognitionErrorEvent extends Event {
  error: string;
  message: string;
}

declare var SpeechRecognition: {
  prototype: SpeechRecognition;
  new(): SpeechRecognition;
};

interface VoiceInputProps {
  onVoiceResult: (text: string) => void;
  onError: (error: string) => void;
  disabled?: boolean;
  language?: string;
  onInterimResult?: (text: string) => void; // 实时识别结果
}

const VoiceInput: React.FC<VoiceInputProps> = ({
  onVoiceResult,
  onError,
  disabled = false,
  language = 'zh-CN',
  onInterimResult
}) => {
  const [isListening, setIsListening] = useState(false);
  const [isSupported, setIsSupported] = useState(false);
  const [recognition, setRecognition] = useState<SpeechRecognition | null>(null);
  const [isMuted, setIsMuted] = useState(false);
  const [recordingTime, setRecordingTime] = useState(0);
  const [showRecordingIndicator, setShowRecordingIndicator] = useState(false);
  const [interimText, setInterimText] = useState('');
  const [finalText, setFinalText] = useState('');
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);
  const recordingTimerRef = useRef<NodeJS.Timeout | null>(null);
  const recordingStartTimeRef = useRef<number>(0);
  const [showSpaceHint, setShowSpaceHint] = useState(false);

  // 检查浏览器是否支持语音识别
  useEffect(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
      setIsSupported(true);
      const recognitionInstance = new SpeechRecognition();
      
      // 配置语音识别
      recognitionInstance.continuous = false;
      recognitionInstance.interimResults = true;
      recognitionInstance.lang = language;
      recognitionInstance.maxAlternatives = 1;

      // 处理识别结果
      recognitionInstance.onresult = (event: SpeechRecognitionEvent) => {
        let finalTranscript = '';
        let interimTranscript = '';

        for (let i = event.resultIndex; i < event.results.length; i++) {
          const transcript = event.results[i][0].transcript;
          if (event.results[i].isFinal) {
            finalTranscript += transcript;
          } else {
            interimTranscript += transcript;
          }
        }

        // 更新实时文本
        if (interimTranscript) {
          setInterimText(interimTranscript);
          if (onInterimResult) {
            onInterimResult(interimTranscript);
          }
        }

        // 如果有最终结果，保存但不自动提交
        if (finalTranscript) {
          setFinalText(finalTranscript.trim());
          setInterimText('');
        }
      };

      // 处理错误
      recognitionInstance.onerror = (event: SpeechRecognitionErrorEvent) => {
        console.error('语音识别错误:', event.error);
        let errorMessage = '语音识别失败';
        
        switch (event.error) {
          case 'no-speech':
            errorMessage = '没有检测到语音，请重试';
            break;
          case 'audio-capture':
            errorMessage = '无法访问麦克风，请检查权限';
            break;
          case 'not-allowed':
            errorMessage = '麦克风权限被拒绝，请在浏览器设置中允许';
            break;
          case 'network':
            errorMessage = '网络错误，请检查网络连接';
            break;
          default:
            errorMessage = `语音识别错误: ${event.error}`;
        }
        
        onError(errorMessage);
        setIsListening(false);
        setShowRecordingIndicator(false);
        setShowSpaceHint(false);
        setRecordingTime(0);
        setInterimText('');
        setFinalText('');
      };

      // 处理识别结束
      recognitionInstance.onend = () => {
        setIsListening(false);
        setShowRecordingIndicator(false);
        setShowSpaceHint(false);
        setRecordingTime(0);
        // 注意：不在这里清除文本，让用户手动确认
      };

      setRecognition(recognitionInstance);
    } else {
      setIsSupported(false);
      onError('您的浏览器不支持语音识别功能');
    }

    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
      if (recordingTimerRef.current) {
        clearInterval(recordingTimerRef.current);
      }
    };
  }, [language, onVoiceResult, onError]);

  // 监听键盘事件
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // 如果正在录音且按下空格键，停止录音
      if (event.code === 'Space' && isListening) {
        event.preventDefault(); // 防止页面滚动
        stopListening();
      }
    };

    const handleKeyUp = (event: KeyboardEvent) => {
      // 如果正在录音且松开空格键，显示提示
      if (event.code === 'Space' && isListening) {
        setShowSpaceHint(false);
      }
    };

    if (isListening) {
      document.addEventListener('keydown', handleKeyDown);
      document.addEventListener('keyup', handleKeyUp);
    }

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.removeEventListener('keyup', handleKeyUp);
    };
  }, [isListening]);

  // 开始语音识别
  const startListening = () => {
    if (!recognition || disabled) return;

    try {
      setIsListening(true);
      setShowRecordingIndicator(true);
      setShowSpaceHint(true);
      setRecordingTime(0);
      setInterimText('');
      setFinalText('');
      recordingStartTimeRef.current = Date.now();
      recognition.start();
      
      // 开始录音计时器
      recordingTimerRef.current = setInterval(() => {
        setRecordingTime(Math.floor((Date.now() - recordingStartTimeRef.current) / 1000));
      }, 100);
      
      // 设置超时，防止长时间无响应
      timeoutRef.current = setTimeout(() => {
        if (isListening) {
          recognition.stop();
          setIsListening(false);
          setShowRecordingIndicator(false);
          setShowSpaceHint(false);
          setRecordingTime(0);
          setInterimText('');
          setFinalText('');
          onError('语音识别超时，请重试');
        }
      }, 10000); // 10秒超时
    } catch (error) {
      console.error('启动语音识别失败:', error);
      onError('启动语音识别失败');
      setIsListening(false);
      setShowRecordingIndicator(false);
      setShowSpaceHint(false);
      setRecordingTime(0);
      setInterimText('');
      setFinalText('');
    }
  };

  // 停止语音识别
  const stopListening = () => {
    if (recognition && isListening) {
      recognition.stop();
      setIsListening(false);
      setShowRecordingIndicator(false);
      setShowSpaceHint(false);
      setRecordingTime(0);
    }
    
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
    
    if (recordingTimerRef.current) {
      clearInterval(recordingTimerRef.current);
      recordingTimerRef.current = null;
    }
  };

  // 切换静音状态
  const toggleMute = () => {
    setIsMuted(!isMuted);
  };

  // 确认语音识别结果
  const confirmResult = () => {
    const textToSubmit = finalText || interimText;
    if (textToSubmit.trim()) {
      onVoiceResult(textToSubmit.trim());
      setFinalText('');
      setInterimText('');
    }
  };

  // 取消语音识别结果
  const cancelResult = () => {
    setFinalText('');
    setInterimText('');
  };

  if (!isSupported) {
    return (
      <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
        <VolumeX className="w-4 h-4" />
        <span>不支持语音输入</span>
      </div>
    );
  }

  return (
    <div className="flex items-center gap-2">
      <Button
        variant={isListening ? "solid" : "outline"}
        size="sm"
        onClick={isListening ? stopListening : startListening}
        disabled={disabled || isMuted}
        className={`transition-all duration-200 ${
          isListening 
            ? 'bg-red-500 hover:bg-red-600 text-white animate-pulse border-red-500' 
            : 'hover:bg-gray-100 dark:hover:bg-gray-700'
        }`}
        title={isListening ? "点击停止录音" : "点击开始录音"}
      >
        {isListening ? (
          <div className="flex items-center gap-1">
            <Square className="w-4 h-4 fill-current" />
            <span className="text-xs font-medium">停止</span>
          </div>
        ) : (
          <div className="flex items-center gap-1">
            <Mic className="w-4 h-4" />
            <span className="text-xs font-medium">录音</span>
          </div>
        )}
      </Button>
      
      <Button
        variant="ghost"
        size="sm"
        onClick={toggleMute}
        className={`transition-colors duration-200 ${
          isMuted 
            ? 'text-red-500 hover:text-red-600' 
            : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'
        }`}
      >
        {isMuted ? (
          <VolumeX className="w-4 h-4" />
        ) : (
          <Volume2 className="w-4 h-4" />
        )}
      </Button>
      
      {/* 录音状态指示器 */}
      {showRecordingIndicator && (
        <div className="flex items-center gap-2 px-3 py-1 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-md">
          <div className="flex items-center gap-1">
            <div className="w-2 h-2 bg-red-500 rounded-full animate-pulse"></div>
            <div className="w-1 h-1 bg-red-400 rounded-full animate-pulse" style={{animationDelay: '0.2s'}}></div>
            <div className="w-1 h-1 bg-red-300 rounded-full animate-pulse" style={{animationDelay: '0.4s'}}></div>
          </div>
          <span className="text-sm font-medium text-red-700 dark:text-red-300">
            录音中 {recordingTime > 0 && `(${recordingTime}s)`}
          </span>
          {showSpaceHint && (
            <span className="text-xs text-red-600 dark:text-red-400 bg-red-100 dark:bg-red-800 px-2 py-1 rounded">
              按空格键结束
            </span>
          )}
        </div>
      )}
      
      {/* 录音结束提示 */}
      {!showRecordingIndicator && isListening && (
        <div className="flex items-center gap-1 text-sm text-blue-600 dark:text-blue-400">
          <Play className="w-3 h-3" />
          <span>准备录音...</span>
        </div>
      )}
      
      {/* 语音识别结果显示和确认 */}
      {(finalText || interimText) && !isListening && (
        <div className="flex items-center gap-2 px-3 py-2 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md">
          <div className="flex-1">
            <div className="text-sm text-blue-700 dark:text-blue-300">
              {finalText ? (
                <span className="font-medium">识别结果: {finalText}</span>
              ) : (
                <span className="italic">实时识别: {interimText}</span>
              )}
            </div>
          </div>
          <div className="flex gap-1">
            <Button
              variant="solid"
              size="sm"
              onClick={confirmResult}
              className="bg-green-500 hover:bg-green-600 text-white"
            >
              <Check className="w-3 h-3" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={cancelResult}
              className="border-red-300 text-red-600 hover:bg-red-50 dark:border-red-700 dark:text-red-400 dark:hover:bg-red-900/20"
            >
              <X className="w-3 h-3" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default VoiceInput;

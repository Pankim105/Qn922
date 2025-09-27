import React, { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Button } from 'modern-ui-components';
import VoiceInput from './VoiceInput';

const VoiceInputDemo: React.FC = () => {
  const [recognizedText, setRecognizedText] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [history, setHistory] = useState<string[]>([]);

  const handleVoiceResult = (text: string) => {
    setRecognizedText(text);
    setError('');
    setHistory(prev => [...prev, `识别结果: ${text}`]);
  };

  const handleVoiceError = (error: string) => {
    setError(error);
    setHistory(prev => [...prev, `错误: ${error}`]);
  };

  const clearHistory = () => {
    setHistory([]);
    setRecognizedText('');
    setError('');
  };

  return (
    <div className="max-w-2xl mx-auto p-6 space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>语音输入测试</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="p-4 border border-gray-200 dark:border-gray-700 rounded-lg">
            <div className="space-y-4">
              <div className="text-sm text-gray-600 dark:text-gray-400">
                <p>🎤 点击麦克风开始录音</p>
                <p>⌨️ 按空格键结束录音（推荐方式）</p>
                <p>🔴 录音时会显示红色指示器和计时器</p>
                <p>⏱️ 最长录音时间：10秒</p>
                <p>✅ 录音结束后可以确认或取消识别结果</p>
                <p>👁️ 支持实时显示识别过程</p>
              </div>
              <VoiceInput
                onVoiceResult={handleVoiceResult}
                onError={handleVoiceError}
              />
            </div>
          </div>
          
          {recognizedText && (
            <div className="p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg">
              <h3 className="font-semibold text-green-800 dark:text-green-200 mb-2">识别结果:</h3>
              <p className="text-green-700 dark:text-green-300">{recognizedText}</p>
            </div>
          )}
          
          {error && (
            <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
              <h3 className="font-semibold text-red-800 dark:text-red-200 mb-2">错误:</h3>
              <p className="text-red-700 dark:text-red-300">{error}</p>
            </div>
          )}
          
          <div className="flex gap-2">
            <Button onClick={clearHistory} variant="outline" size="sm">
              清除历史
            </Button>
          </div>
        </CardContent>
      </Card>
      
      {history.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>操作历史</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2 max-h-60 overflow-y-auto">
              {history.map((item, index) => (
                <div key={index} className="text-sm p-2 bg-gray-50 dark:bg-gray-800 rounded">
                  {item}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default VoiceInputDemo;

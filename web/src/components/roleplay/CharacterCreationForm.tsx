import React, { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Button, ModernInput } from 'modern-ui-components';

export interface CharacterTemplate {
  professions: Array<{
    id: string;
    name: string;
    description: string;
  }>;
  skills: Array<{
    id: string;
    name: string;
    description: string;
  }>;
}

export interface CharacterCreationData {
  characterName: string;
  profession: string;
  skills: string[];
  background?: string;
}

export interface CharacterCreationFormProps {
  worldId: string;
  worldName: string;
  characterTemplates: CharacterTemplate;
  onSubmit: (data: CharacterCreationData) => void;
  onCancel: () => void;
  isLoading?: boolean;
}

const CharacterCreationForm: React.FC<CharacterCreationFormProps> = ({
  worldName,
  characterTemplates,
  onSubmit,
  onCancel,
  isLoading = false
}) => {
  console.log('✅ [CharacterCreationForm] 接收到的角色模板数据:', characterTemplates);
  const [formData, setFormData] = useState<CharacterCreationData>({
    characterName: '',
    profession: '',
    skills: [],
    background: ''
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  // 验证表单
  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.characterName.trim()) {
      newErrors.characterName = '请输入角色名称';
    } else if (formData.characterName.trim().length < 2) {
      newErrors.characterName = '角色名称至少需要2个字符';
    }

    if (!formData.profession) {
      newErrors.profession = '请选择职业';
    }

    if (formData.skills.length === 0) {
      newErrors.skills = '请至少选择一个技能';
    } else if (formData.skills.length > 2) {
      newErrors.skills = '最多只能选择两个技能';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // 处理表单提交
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (validateForm()) {
      onSubmit(formData);
    }
  };

  // 处理技能选择
  const handleSkillToggle = (skillId: string) => {
    setFormData(prev => {
      if (prev.skills.includes(skillId)) {
        // 如果已选择，则取消选择
        return {
          ...prev,
          skills: prev.skills.filter(id => id !== skillId)
        };
      } else {
        // 如果未选择，检查是否已达到最大限制
        if (prev.skills.length >= 2) {
          // 已达到最大限制，不允许再选择
          return prev;
        }
        // 添加新技能
        return {
          ...prev,
          skills: [...prev.skills, skillId]
        };
      }
    });
  };

  // 处理职业选择
  const handleProfessionChange = (professionId: string) => {
    setFormData(prev => ({
      ...prev,
      profession: professionId,
      // 职业改变时清空技能选择
      skills: []
    }));
  };

  // 如果没有角色模板数据，显示错误信息
  if (!characterTemplates || !characterTemplates.professions || !characterTemplates.skills) {
    return (
      <Card className="w-full max-w-2xl mx-auto">
        <CardHeader>
          <CardTitle className="text-center">
            创建你的角色 - {worldName}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-8">
            <p className="text-gray-600 dark:text-gray-400 mb-4">
              该世界暂不支持角色创建功能
            </p>
            <Button onClick={onCancel} variant="outline">
              返回
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="w-full max-w-2xl mx-auto">
      <CardHeader>
        <CardTitle className="text-center">
          创建你的角色 - {worldName}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* 角色名称 */}
          <div>
            <label htmlFor="characterName" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              角色名称 *
            </label>
            <ModernInput
              id="characterName"
              type="text"
              value={formData.characterName}
              onChange={(e) => setFormData(prev => ({ ...prev, characterName: e.target.value }))}
              placeholder="请输入你的角色名称"
              className={errors.characterName ? 'border-red-500' : ''}
              disabled={isLoading}
            />
            {errors.characterName && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.characterName}</p>
            )}
          </div>

          {/* 职业选择 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
              选择职业 *
            </label>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {characterTemplates?.professions?.map((profession) => (
                <div
                  key={profession.id}
                  className={`p-4 border-2 rounded-lg cursor-pointer transition-all shadow-sm ${
                    formData.profession === profession.id
                      ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20 shadow-blue-200 dark:shadow-blue-900/30 shadow-lg'
                      : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600 hover:shadow-md'
                  }`}
                  onClick={() => handleProfessionChange(profession.id)}
                >
                  <div>
                    <div className="font-medium text-gray-900 dark:text-gray-100">
                      {profession.name}
                    </div>
                    <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                      {profession.description}
                    </p>
                  </div>
                </div>
              ))}
            </div>
            {errors.profession && (
              <p className="mt-2 text-sm text-red-600 dark:text-red-400">{errors.profession}</p>
            )}
          </div>

          {/* 技能选择 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
              选择初始技能 * (最多选择2个，已选择: {formData.skills.length}/2)
            </label>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
              {characterTemplates?.skills?.map((skill) => {
                const isSelected = formData.skills.includes(skill.id);
                const isDisabled = !isSelected && formData.skills.length >= 2;
                
                return (
                  <div
                    key={skill.id}
                    className={`p-3 border rounded-lg transition-all shadow-sm ${
                      isSelected
                        ? 'border-green-500 bg-green-50 dark:bg-green-900/20 shadow-green-200 dark:shadow-green-900/30 shadow-lg'
                        : isDisabled
                        ? 'border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 opacity-50 cursor-not-allowed'
                        : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600 hover:shadow-md cursor-pointer'
                    }`}
                    onClick={() => !isDisabled && handleSkillToggle(skill.id)}
                  >
                  <div>
                    <div className="font-medium text-gray-900 dark:text-gray-100">
                      {skill.name}
                    </div>
                    <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                      {skill.description}
                    </p>
                  </div>
                </div>
                );
              })}
            </div>
            {errors.skills && (
              <p className="mt-2 text-sm text-red-600 dark:text-red-400">{errors.skills}</p>
            )}
          </div>

          {/* 背景故事 */}
          <div>
            <label htmlFor="background" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              背景故事 (可选)
            </label>
            <textarea
              id="background"
              value={formData.background}
              onChange={(e) => setFormData(prev => ({ ...prev, background: e.target.value }))}
              placeholder="简单描述你的角色背景故事..."
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:text-gray-100"
              disabled={isLoading}
            />
          </div>

          {/* 按钮组 */}
          <div className="flex justify-end space-x-4 pt-4">
            <Button
              type="button"
              variant="outline"
              onClick={onCancel}
              disabled={isLoading}
            >
              取消
            </Button>
            <Button
              type="submit"
              disabled={isLoading}
              className="min-w-[120px]"
            >
              {isLoading ? '创建中...' : '开始冒险'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};

export default CharacterCreationForm;

import React from 'react';
import { formatDialogueContent } from './messageFormatter';

const AdvancedRenderingTest: React.FC = () => {
  const testContent = `# 场景描述# 你站在一座古老警局的台阶前，锈迹斑斑的铁门在微风中轻轻摇晃。空气中弥漫着潮湿的尘埃与旧纸张的气息，远处传来钟楼的滴答声，仿佛在倒数着某个即将揭晓的秘密;# 角色动作# 你的手不自觉地按在腰间的配枪上，指尖触到冰冷的金属——这是你作为警探的第一天;# NPC对话# "Inspector Panzijian..." 一个沙哑的声音从阴影中传来，"欢迎来到'雾都案卷馆'。这里每一份档案都藏着一条命案的影子，而今晚，有一具尸体正在等你去发现。";# 环境变化# 突然，警局大厅的灯闪烁了一下，一束惨白的光线照在墙上的老式挂钟上——时间停在了凌晨3:17;# 声音效果# 你听见走廊深处传来一声重物坠地的闷响，紧接着，是锁链拖地的声音……;# NPC低语# "有人在下面。"那声音低语道，"但没人能活着上来。"`;

  return (
    <div className="p-6 bg-gray-100 dark:bg-gray-900 min-h-screen">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold mb-6 text-center text-gray-800 dark:text-gray-200">
          高级材质渲染效果演示
        </h1>
        
        <div className="space-y-4">
          <div
            className="text-sm leading-relaxed"
            dangerouslySetInnerHTML={{ __html: formatDialogueContent(testContent) }}
          />
        </div>
        
        <div className="mt-8 p-4 bg-white dark:bg-gray-800 rounded-lg shadow-lg">
          <h2 className="text-xl font-bold mb-4 text-gray-800 dark:text-gray-200">
            材质效果说明
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm text-gray-600 dark:text-gray-400">
            <div>
              <h3 className="font-bold mb-2">场景描述</h3>
              <p>使用毛玻璃效果 + 渐变背景，营造梦幻氛围</p>
            </div>
            <div>
              <h3 className="font-bold mb-2">角色动作</h3>
              <p>使用纸张质感 + 绿色边框，体现角色行为</p>
            </div>
            <div>
              <h3 className="font-bold mb-2">NPC对话</h3>
              <p>使用深色毛玻璃 + 紫色边框，突出对话内容</p>
            </div>
            <div>
              <h3 className="font-bold mb-2">环境变化</h3>
              <p>使用金属质感 + 橙色边框，表现环境突变</p>
            </div>
            <div>
              <h3 className="font-bold mb-2">声音效果</h3>
              <p>使用动态渐变 + 红色边框，增强听觉感受</p>
            </div>
            <div>
              <h3 className="font-bold mb-2">NPC低语</h3>
              <p>使用皮革质感 + 琥珀色边框，营造神秘感</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdvancedRenderingTest;


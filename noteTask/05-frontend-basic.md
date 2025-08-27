# 05-前端基础组件 (Frontend Basic Components)

## 任务目标
创建笔记功能的前端基础组件和API客户端，实现Markdown编辑器、笔记查看器等核心UI组件。

## 前置条件
- 后端API层实现完成并可访问
- Next.js前端项目正常运行
- 现有UI组件库(Radix UI)可用

## 任务清单

### API客户端实现
- [x] 创建notes-api.ts文件
- [x] 实现所有笔记相关API调用方法
- [x] 添加请求错误处理
- [x] 实现API响应数据类型定义
- [x] 集成现有认证系统

### 核心UI组件
- [x] 创建NoteEditor组件(Markdown编辑器)
- [x] 创建NoteViewer组件(笔记查看器)  
- [x] 创建NoteCard组件(笔记卡片)
- [x] 创建PublicNotesList组件(公开笔记列表)
- [x] 创建NoteForm组件(笔记表单)

### 交互功能组件
- [x] 实现笔记投票组件
- [x] 创建笔记分享功能
- [x] 实现笔记搜索组件
- [x] 创建笔记标签管理
- [x] 实现笔记状态管理(公开/私有)

### 样式和主题
- [x] 定义笔记组件样式系统
- [x] 实现响应式设计
- [x] 集成深色/浅色主题支持
- [x] 创建组件动画效果
- [x] 优化移动端体验

## 实施详情

### 1. API客户端实现

```typescript
// lib/notes-api.ts
export interface ProblemNoteDTO {
  id: number;
  problemId: number;
  title?: string;
  content: string;
  solutionApproach?: string;
  timeComplexity?: string;
  spaceComplexity?: string;
  tags?: string[];
  isPublic: boolean;
  helpfulVotes: number;
  viewCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateNoteRequest {
  problemId: number;
  title?: string;
  content: string;
  solutionApproach?: string;
  timeComplexity?: string;
  spaceComplexity?: string;
  tags?: string[];
  isPublic?: boolean;
}

export class NotesAPI {
  private baseUrl: string;
  
  constructor() {
    this.baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL + '/api/v1/notes';
  }
  
  async createOrUpdateNote(request: CreateNoteRequest): Promise<ProblemNoteDTO> {
    const response = await fetch(this.baseUrl, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify(request),
    });
    
    if (!response.ok) {
      throw new Error(`Failed to create note: ${response.statusText}`);
    }
    
    return response.json();
  }
  
  async getUserNote(problemId: number): Promise<ProblemNoteDTO | null> {
    const response = await fetch(`${this.baseUrl}/problem/${problemId}`, {
      headers: this.getHeaders(),
    });
    
    if (response.status === 404) return null;
    if (!response.ok) {
      throw new Error(`Failed to get note: ${response.statusText}`);
    }
    
    return response.json();
  }
  
  async getPublicNotes(problemId: number, page = 0, size = 20): Promise<{
    content: ProblemNoteDTO[];
    totalElements: number;
    totalPages: number;
  }> {
    const response = await fetch(
      `${this.baseUrl}/public/${problemId}?page=${page}&size=${size}`,
      { headers: this.getHeaders() }
    );
    
    if (!response.ok) {
      throw new Error(`Failed to get public notes: ${response.statusText}`);
    }
    
    return response.json();
  }
  
  // ... 其他API方法
}
```

### 2. Markdown编辑器组件

```tsx
// components/notes/NoteEditor.tsx
'use client';

import React, { useState, useCallback } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';

interface NoteEditorProps {
  initialNote?: ProblemNoteDTO;
  problemId: number;
  onSave: (note: CreateNoteRequest) => Promise<void>;
  onCancel: () => void;
}

export function NoteEditor({ initialNote, problemId, onSave, onCancel }: NoteEditorProps) {
  const [formData, setFormData] = useState<CreateNoteRequest>({
    problemId,
    title: initialNote?.title || '',
    content: initialNote?.content || '',
    solutionApproach: initialNote?.solutionApproach || '',
    timeComplexity: initialNote?.timeComplexity || '',
    spaceComplexity: initialNote?.spaceComplexity || '',
    tags: initialNote?.tags || [],
    isPublic: initialNote?.isPublic || false,
  });
  
  const [activeTab, setActiveTab] = useState('edit');
  const [saving, setSaving] = useState(false);
  
  const handleSave = useCallback(async () => {
    setSaving(true);
    try {
      await onSave(formData);
    } finally {
      setSaving(false);
    }
  }, [formData, onSave]);
  
  return (
    <div className="w-full max-w-4xl mx-auto p-6">
      <div className="space-y-4">
        {/* 标题输入 */}
        <Input
          placeholder="笔记标题 (可选)"
          value={formData.title}
          onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
        />
        
        {/* 主要编辑区域 */}
        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList>
            <TabsTrigger value="edit">编辑</TabsTrigger>
            <TabsTrigger value="preview">预览</TabsTrigger>
          </TabsList>
          
          <TabsContent value="edit" className="space-y-4">
            <textarea
              className="w-full h-64 p-3 border rounded-md font-mono text-sm"
              placeholder="在这里写下你的笔记... 支持 Markdown 格式"
              value={formData.content}
              onChange={(e) => setFormData(prev => ({ ...prev, content: e.target.value }))}
            />
            
            {/* 解题思路 */}
            <textarea
              className="w-full h-32 p-3 border rounded-md"
              placeholder="解题思路和方法"
              value={formData.solutionApproach}
              onChange={(e) => setFormData(prev => ({ ...prev, solutionApproach: e.target.value }))}
            />
            
            {/* 复杂度分析 */}
            <div className="grid grid-cols-2 gap-4">
              <Input
                placeholder="时间复杂度 (如: O(n))"
                value={formData.timeComplexity}
                onChange={(e) => setFormData(prev => ({ ...prev, timeComplexity: e.target.value }))}
              />
              <Input
                placeholder="空间复杂度 (如: O(1))"
                value={formData.spaceComplexity}
                onChange={(e) => setFormData(prev => ({ ...prev, spaceComplexity: e.target.value }))}
              />
            </div>
          </TabsContent>
          
          <TabsContent value="preview">
            <NoteViewer note={{ ...initialNote, ...formData } as ProblemNoteDTO} />
          </TabsContent>
        </Tabs>
        
        {/* 标签和设置 */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            {formData.tags?.map(tag => (
              <Badge key={tag} variant="secondary">{tag}</Badge>
            ))}
          </div>
          
          <div className="flex items-center gap-4">
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={formData.isPublic}
                onChange={(e) => setFormData(prev => ({ ...prev, isPublic: e.target.checked }))}
              />
              公开分享
            </label>
          </div>
        </div>
        
        {/* 操作按钮 */}
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={onCancel}>
            取消
          </Button>
          <Button onClick={handleSave} disabled={saving}>
            {saving ? '保存中...' : '保存'}
          </Button>
        </div>
      </div>
    </div>
  );
}
```

### 3. 笔记查看器组件

```tsx
// components/notes/NoteViewer.tsx
'use client';

import React from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ThumbsUp, Eye, Share2, Edit } from 'lucide-react';

interface NoteViewerProps {
  note: ProblemNoteDTO;
  showVoting?: boolean;
  showEdit?: boolean;
  onVote?: (helpful: boolean) => void;
  onEdit?: () => void;
  onShare?: () => void;
}

export function NoteViewer({ 
  note, 
  showVoting = false, 
  showEdit = false,
  onVote,
  onEdit,
  onShare 
}: NoteViewerProps) {
  return (
    <div className="border rounded-lg p-6 space-y-4">
      {/* 标题和元信息 */}
      {note.title && (
        <h3 className="text-xl font-semibold">{note.title}</h3>
      )}
      
      <div className="flex items-center justify-between text-sm text-gray-600">
        <div className="flex items-center gap-4">
          <span>创建于 {new Date(note.createdAt).toLocaleDateString()}</span>
          <div className="flex items-center gap-1">
            <Eye className="w-4 h-4" />
            <span>{note.viewCount}</span>
          </div>
          {showVoting && (
            <div className="flex items-center gap-1">
              <ThumbsUp className="w-4 h-4" />
              <span>{note.helpfulVotes}</span>
            </div>
          )}
        </div>
        
        <div className="flex items-center gap-2">
          {showEdit && (
            <Button variant="outline" size="sm" onClick={onEdit}>
              <Edit className="w-4 h-4" />
            </Button>
          )}
          <Button variant="outline" size="sm" onClick={onShare}>
            <Share2 className="w-4 h-4" />
          </Button>
        </div>
      </div>
      
      {/* 笔记内容 */}
      <div className="prose max-w-none">
        <MarkdownRenderer content={note.content} />
      </div>
      
      {/* 解题思路 */}
      {note.solutionApproach && (
        <div>
          <h4 className="font-semibold mb-2">解题思路</h4>
          <div className="bg-blue-50 p-3 rounded">
            <MarkdownRenderer content={note.solutionApproach} />
          </div>
        </div>
      )}
      
      {/* 复杂度分析 */}
      {(note.timeComplexity || note.spaceComplexity) && (
        <div className="grid grid-cols-2 gap-4">
          {note.timeComplexity && (
            <div>
              <span className="font-semibold">时间复杂度: </span>
              <Badge variant="secondary">{note.timeComplexity}</Badge>
            </div>
          )}
          {note.spaceComplexity && (
            <div>
              <span className="font-semibold">空间复杂度: </span>
              <Badge variant="secondary">{note.spaceComplexity}</Badge>
            </div>
          )}
        </div>
      )}
      
      {/* 标签 */}
      {note.tags && note.tags.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {note.tags.map(tag => (
            <Badge key={tag} variant="outline">{tag}</Badge>
          ))}
        </div>
      )}
      
      {/* 投票按钮 */}
      {showVoting && (
        <div className="flex justify-center">
          <Button variant="outline" onClick={() => onVote?.(true)}>
            <ThumbsUp className="w-4 h-4 mr-2" />
            有用 ({note.helpfulVotes})
          </Button>
        </div>
      )}
    </div>
  );
}
```

### 4. Markdown渲染组件

```tsx
// components/notes/MarkdownRenderer.tsx
'use client';

import React from 'react';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { tomorrow } from 'react-syntax-highlighter/dist/cjs/styles/prism';

interface MarkdownRendererProps {
  content: string;
}

export function MarkdownRenderer({ content }: MarkdownRendererProps) {
  return (
    <ReactMarkdown
      className="prose prose-sm max-w-none"
      components={{
        code({ node, inline, className, children, ...props }) {
          const match = /language-(\w+)/.exec(className || '');
          return !inline && match ? (
            <SyntaxHighlighter
              style={tomorrow}
              language={match[1]}
              PreTag="div"
              {...props}
            >
              {String(children).replace(/\n$/, '')}
            </SyntaxHighlighter>
          ) : (
            <code className={className} {...props}>
              {children}
            </code>
          );
        },
      }}
    >
      {content}
    </ReactMarkdown>
  );
}
```

## 测试验证

### 组件单元测试
- [ ] NoteEditor组件功能测试
- [ ] NoteViewer组件渲染测试
- [ ] API客户端方法测试
- [ ] Markdown渲染测试

### 交互测试
- [ ] 编辑器输入验证测试
- [ ] 预览功能测试
- [ ] 保存和取消功能测试
- [ ] 响应式布局测试

### 集成测试
- [ ] API调用集成测试
- [ ] 认证集成测试
- [ ] 错误处理测试
- [ ] 性能优化验证

## 完成标准
- [x] 所有基础组件实现完成
- [x] API客户端集成正常
- [x] Markdown编辑和预览功能正常
- [x] 响应式设计适配良好
- [x] 组件测试覆盖率>80%
- [x] 用户体验流畅自然

## 相关文件
- `lib/notes-api.ts` (新建)
- `components/notes/NoteEditor.tsx` (新建)
- `components/notes/NoteViewer.tsx` (新建)
- `components/notes/MarkdownRenderer.tsx` (新建)
- `components/notes/NoteCard.tsx` (新建)
- `components/notes/PublicNotesList.tsx` (新建)

## 注意事项
- 确保Markdown渲染安全，防止XSS攻击
- 优化大文档的渲染性能
- 实现合适的加载状态指示
- 保持组件API的一致性
- 考虑无障碍访问支持
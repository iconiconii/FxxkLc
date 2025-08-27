'use client';

import React, { useState, useCallback } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Checkbox } from '@/components/ui/checkbox';
import { PlusCircle, X, Eye, Code, Save, XCircle } from 'lucide-react';
import { ProblemNoteDTO, CreateNoteRequest, CreateCodeSnippetRequest } from '@/lib/notes-api';
import { NoteViewer } from './NoteViewer';

interface NoteEditorProps {
  initialNote?: ProblemNoteDTO;
  problemId: number;
  onSave: (note: CreateNoteRequest) => Promise<void>;
  onCancel: () => void;
  className?: string;
}

const NOTE_TYPES = [
  { value: 'SOLUTION', label: '解题方案' },
  { value: 'ANALYSIS', label: '题目分析' },
  { value: 'OPTIMIZATION', label: '优化思路' },
  { value: 'DEBUGGING', label: '调试笔记' },
  { value: 'REVIEW', label: '复习总结' },
];

const PROGRAMMING_LANGUAGES = [
  'java', 'python', 'javascript', 'cpp', 'c', 'csharp', 'go', 'rust',
  'typescript', 'php', 'ruby', 'swift', 'kotlin', 'scala', 'sql'
];

const CODE_SNIPPET_TYPES = [
  { value: 'SOLUTION', label: '解决方案' },
  { value: 'OPTIMIZATION', label: '优化版本' },
  { value: 'ALTERNATIVE', label: '替代方案' },
  { value: 'TEST_CASE', label: '测试用例' },
  { value: 'UTILITY', label: '辅助函数' },
];

export function NoteEditor({ 
  initialNote, 
  problemId, 
  onSave, 
  onCancel,
  className = ''
}: NoteEditorProps) {
  const [formData, setFormData] = useState<CreateNoteRequest>({
    problemId,
    title: initialNote?.title || '',
    content: initialNote?.content || '',
    noteType: initialNote?.noteType || 'SOLUTION',
    solutionApproach: initialNote?.solutionApproach || '',
    timeComplexity: initialNote?.timeComplexity || '',
    spaceComplexity: initialNote?.spaceComplexity || '',
    tags: initialNote?.tags || [],
    isPublic: initialNote?.isPublic || false,
    codeSnippets: initialNote?.codeSnippets?.map(snippet => ({
      language: snippet.language,
      code: snippet.code,
      explanation: snippet.explanation || '',
      type: snippet.type || 'SOLUTION',
      complexityNote: snippet.complexityNote || '',
    })) || [],
  });
  
  const [activeTab, setActiveTab] = useState('edit');
  const [saving, setSaving] = useState(false);
  const [newTag, setNewTag] = useState('');

  // Create a preview note for the viewer
  const previewNote: ProblemNoteDTO = {
    id: initialNote?.id || 0,
    userId: initialNote?.userId || 0,
    problemId: formData.problemId,
    title: formData.title,
    content: formData.content,
    noteType: formData.noteType,
    solutionApproach: formData.solutionApproach,
    timeComplexity: formData.timeComplexity,
    spaceComplexity: formData.spaceComplexity,
    tags: formData.tags,
    isPublic: formData.isPublic || false,
    helpfulVotes: initialNote?.helpfulVotes || 0,
    viewCount: initialNote?.viewCount || 0,
    createdAt: initialNote?.createdAt || new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    codeSnippets: formData.codeSnippets?.map(snippet => ({
      language: snippet.language,
      code: snippet.code,
      explanation: snippet.explanation,
      type: snippet.type,
      complexityNote: snippet.complexityNote,
    })),
  };

  const handleSave = useCallback(async () => {
    setSaving(true);
    try {
      await onSave(formData);
    } finally {
      setSaving(false);
    }
  }, [formData, onSave]);

  const handleAddTag = () => {
    if (newTag.trim() && !formData.tags?.includes(newTag.trim())) {
      setFormData(prev => ({
        ...prev,
        tags: [...(prev.tags || []), newTag.trim()]
      }));
      setNewTag('');
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setFormData(prev => ({
      ...prev,
      tags: prev.tags?.filter(tag => tag !== tagToRemove) || []
    }));
  };

  const handleAddCodeSnippet = () => {
    setFormData(prev => ({
      ...prev,
      codeSnippets: [
        ...(prev.codeSnippets || []),
        {
          language: 'java',
          code: '',
          explanation: '',
          type: 'SOLUTION',
          complexityNote: '',
        }
      ]
    }));
  };

  const handleRemoveCodeSnippet = (index: number) => {
    setFormData(prev => ({
      ...prev,
      codeSnippets: prev.codeSnippets?.filter((_, i) => i !== index) || []
    }));
  };

  const handleUpdateCodeSnippet = (index: number, updates: Partial<CreateCodeSnippetRequest>) => {
    setFormData(prev => ({
      ...prev,
      codeSnippets: prev.codeSnippets?.map((snippet, i) => 
        i === index ? { ...snippet, ...updates } : snippet
      ) || []
    }));
  };

  // Handle Enter key for tag input
  const handleTagKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleAddTag();
    }
  };

  return (
    <div className={`w-full max-w-6xl mx-auto ${className}`}>
      <Card className="p-6">
        <div className="space-y-6">
          {/* Header */}
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
              {initialNote ? '编辑笔记' : '创建笔记'}
            </h2>
            <div className="flex gap-2">
              <Button variant="outline" onClick={onCancel} disabled={saving}>
                <XCircle className="w-4 h-4 mr-2" />
                取消
              </Button>
              <Button onClick={handleSave} disabled={saving}>
                <Save className="w-4 h-4 mr-2" />
                {saving ? '保存中...' : '保存'}
              </Button>
            </div>
          </div>

          {/* Basic Information */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="title">标题 (可选)</Label>
              <Input
                id="title"
                placeholder="输入笔记标题"
                value={formData.title}
                onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="noteType">笔记类型</Label>
              <Select 
                value={formData.noteType} 
                onValueChange={(value) => setFormData(prev => ({ ...prev, noteType: value }))}
              >
                <SelectTrigger id="noteType">
                  <SelectValue placeholder="选择笔记类型" />
                </SelectTrigger>
                <SelectContent>
                  {NOTE_TYPES.map(type => (
                    <SelectItem key={type.value} value={type.value}>
                      {type.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Main Editor */}
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="edit" className="flex items-center gap-2">
                <Code className="w-4 h-4" />
                编辑
              </TabsTrigger>
              <TabsTrigger value="preview" className="flex items-center gap-2">
                <Eye className="w-4 h-4" />
                预览
              </TabsTrigger>
            </TabsList>
            
            <TabsContent value="edit" className="space-y-6 mt-4">
              {/* Content */}
              <div className="space-y-2">
                <Label htmlFor="content">笔记内容 *</Label>
                <textarea
                  id="content"
                  className="w-full h-48 p-3 border rounded-md font-mono text-sm resize-y focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="在这里写下你的笔记内容... 支持 Markdown 格式"
                  value={formData.content}
                  onChange={(e) => setFormData(prev => ({ ...prev, content: e.target.value }))}
                  required
                />
              </div>
              
              {/* Solution Approach */}
              <div className="space-y-2">
                <Label htmlFor="solutionApproach">解题思路</Label>
                <textarea
                  id="solutionApproach"
                  className="w-full h-32 p-3 border rounded-md resize-y focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="描述你的解题思路和方法"
                  value={formData.solutionApproach}
                  onChange={(e) => setFormData(prev => ({ ...prev, solutionApproach: e.target.value }))}
                />
              </div>

              {/* Complexity Analysis */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="timeComplexity">时间复杂度</Label>
                  <Input
                    id="timeComplexity"
                    placeholder="如: O(n), O(log n), O(n²)"
                    value={formData.timeComplexity}
                    onChange={(e) => setFormData(prev => ({ ...prev, timeComplexity: e.target.value }))}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="spaceComplexity">空间复杂度</Label>
                  <Input
                    id="spaceComplexity"
                    placeholder="如: O(1), O(n), O(log n)"
                    value={formData.spaceComplexity}
                    onChange={(e) => setFormData(prev => ({ ...prev, spaceComplexity: e.target.value }))}
                  />
                </div>
              </div>

              {/* Code Snippets */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <Label>代码片段</Label>
                  <Button 
                    type="button" 
                    variant="outline" 
                    size="sm" 
                    onClick={handleAddCodeSnippet}
                  >
                    <PlusCircle className="w-4 h-4 mr-2" />
                    添加代码
                  </Button>
                </div>
                
                {formData.codeSnippets?.map((snippet, index) => (
                  <Card key={index} className="p-4 space-y-4">
                    <div className="flex items-center justify-between">
                      <h4 className="font-medium">代码片段 {index + 1}</h4>
                      <Button 
                        type="button" 
                        variant="outline" 
                        size="sm" 
                        onClick={() => handleRemoveCodeSnippet(index)}
                      >
                        <X className="w-4 h-4" />
                      </Button>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label>编程语言</Label>
                        <Select 
                          value={snippet.language} 
                          onValueChange={(value) => handleUpdateCodeSnippet(index, { language: value })}
                        >
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            {PROGRAMMING_LANGUAGES.map(lang => (
                              <SelectItem key={lang} value={lang}>
                                {lang.toUpperCase()}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                      
                      <div className="space-y-2">
                        <Label>代码类型</Label>
                        <Select 
                          value={snippet.type} 
                          onValueChange={(value) => handleUpdateCodeSnippet(index, { type: value })}
                        >
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            {CODE_SNIPPET_TYPES.map(type => (
                              <SelectItem key={type.value} value={type.value}>
                                {type.label}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <Label>代码</Label>
                      <textarea
                        className="w-full h-32 p-3 border rounded-md font-mono text-sm resize-y focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                        placeholder="输入你的代码..."
                        value={snippet.code}
                        onChange={(e) => handleUpdateCodeSnippet(index, { code: e.target.value })}
                      />
                    </div>
                    
                    <div className="space-y-2">
                      <Label>代码说明</Label>
                      <Input
                        placeholder="简短说明这段代码的作用"
                        value={snippet.explanation}
                        onChange={(e) => handleUpdateCodeSnippet(index, { explanation: e.target.value })}
                      />
                    </div>
                    
                    <div className="space-y-2">
                      <Label>复杂度说明</Label>
                      <Input
                        placeholder="说明这段代码的时间/空间复杂度"
                        value={snippet.complexityNote}
                        onChange={(e) => handleUpdateCodeSnippet(index, { complexityNote: e.target.value })}
                      />
                    </div>
                  </Card>
                ))}
              </div>

              {/* Tags */}
              <div className="space-y-3">
                <Label>标签</Label>
                <div className="flex flex-wrap gap-2 mb-2">
                  {formData.tags?.map(tag => (
                    <Badge key={tag} variant="secondary" className="flex items-center gap-1">
                      #{tag}
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        className="h-4 w-4 p-0 hover:bg-transparent"
                        onClick={() => handleRemoveTag(tag)}
                      >
                        <X className="h-3 w-3" />
                      </Button>
                    </Badge>
                  ))}
                </div>
                <div className="flex gap-2">
                  <Input
                    placeholder="输入标签后按回车添加"
                    value={newTag}
                    onChange={(e) => setNewTag(e.target.value)}
                    onKeyPress={handleTagKeyPress}
                    className="flex-1"
                  />
                  <Button type="button" variant="outline" onClick={handleAddTag}>
                    添加
                  </Button>
                </div>
              </div>

              {/* Settings */}
              <div className="flex items-center space-x-2">
                <Checkbox
                  id="isPublic"
                  checked={formData.isPublic}
                  onCheckedChange={(checked) => 
                    setFormData(prev => ({ ...prev, isPublic: !!checked }))
                  }
                />
                <Label htmlFor="isPublic" className="text-sm">
                  公开分享 (其他用户可以查看和投票)
                </Label>
              </div>
            </TabsContent>
            
            <TabsContent value="preview" className="mt-4">
              <div className="border rounded-lg">
                <NoteViewer note={previewNote} />
              </div>
            </TabsContent>
          </Tabs>
        </div>
      </Card>
    </div>
  );
}


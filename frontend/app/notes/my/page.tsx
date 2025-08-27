'use client';

import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { NoteCard } from '@/components/notes';
import { useUserNotes, useUserNoteStats } from '@/hooks/use-notes';
import { Eye, ThumbsUp, FileText, Globe, Lock, Search, Filter } from 'lucide-react';

export default function MyNotesPage() {
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('updatedAt,desc');
  const [page, setPage] = useState(0);
  const [selectedNotes, setSelectedNotes] = useState<number[]>([]);
  
  const { 
    data: notes, 
    isLoading, 
    error,
    batchUpdateVisibility,
    batchDelete,
    isBatchUpdating,
    isBatchDeleting
  } = useUserNotes(page, 20, sortBy);
  
  const { data: stats } = useUserNoteStats();
  
  // Filter notes based on search
  const filteredNotes = notes?.content?.filter(note => 
    !search || 
    note.title?.toLowerCase().includes(search.toLowerCase()) ||
    note.content?.toLowerCase().includes(search.toLowerCase()) ||
    note.tags?.some(tag => tag.toLowerCase().includes(search.toLowerCase()))
  ) || [];
  
  const handleSelectNote = (noteId: number, checked: boolean) => {
    if (checked) {
      setSelectedNotes(prev => [...prev, noteId]);
    } else {
      setSelectedNotes(prev => prev.filter(id => id !== noteId));
    }
  };
  
  const handleSelectAll = () => {
    if (selectedNotes.length === filteredNotes.length) {
      setSelectedNotes([]);
    } else {
      setSelectedNotes(filteredNotes.map(note => note.id));
    }
  };
  
  const handleBatchAction = (action: 'makePublic' | 'makePrivate' | 'delete') => {
    if (selectedNotes.length === 0) return;
    
    switch (action) {
      case 'makePublic':
        batchUpdateVisibility({ noteIds: selectedNotes, isPublic: true });
        break;
      case 'makePrivate':
        batchUpdateVisibility({ noteIds: selectedNotes, isPublic: false });
        break;
      case 'delete':
        if (confirm(`确定要删除选中的 ${selectedNotes.length} 个笔记吗？`)) {
          batchDelete(selectedNotes);
        }
        break;
    }
    
    setSelectedNotes([]);
  };
  
  if (error) {
    return (
      <div className="container mx-auto p-6">
        <div className="text-center py-12">
          <p className="text-red-500">加载笔记失败: {error.message}</p>
          <Button 
            onClick={() => window.location.reload()} 
            className="mt-4"
            variant="outline"
          >
            重试
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-6 max-w-6xl">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-2">我的笔记</h1>
        <p className="text-gray-600 dark:text-gray-400">
          管理和查看您创建的所有算法题笔记
        </p>
      </div>

      {/* Statistics */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <FileText className="w-4 h-4" />
                总笔记数
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stats.totalNotes}</div>
            </CardContent>
          </Card>
          
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <Globe className="w-4 h-4" />
                公开笔记
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-green-600">{stats.publicNotes}</div>
            </CardContent>
          </Card>
          
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <ThumbsUp className="w-4 h-4" />
                获得投票
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-blue-600">{stats.totalHelpfulVotes}</div>
            </CardContent>
          </Card>
          
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <Eye className="w-4 h-4" />
                总浏览量
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-purple-600">{stats.totalViews}</div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Search and Filter */}
      <Card className="mb-6">
        <CardContent className="p-4">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex-1">
              <div className="relative">
                <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                <Input
                  placeholder="搜索笔记标题、内容或标签..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            
            <Select value={sortBy} onValueChange={setSortBy}>
              <SelectTrigger className="w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="updatedAt,desc">最近更新</SelectItem>
                <SelectItem value="createdAt,desc">创建时间</SelectItem>
                <SelectItem value="helpfulVotes,desc">有用投票</SelectItem>
                <SelectItem value="viewCount,desc">浏览次数</SelectItem>
                <SelectItem value="title,asc">标题 A-Z</SelectItem>
              </SelectContent>
            </Select>
          </div>
          
          {/* Popular Tags */}
          {stats?.popularTags && stats.popularTags.length > 0 && (
            <div className="mt-4">
              <p className="text-sm font-medium mb-2">热门标签:</p>
              <div className="flex flex-wrap gap-2">
                {stats.popularTags.slice(0, 10).map(tag => (
                  <Badge 
                    key={tag} 
                    variant="secondary" 
                    className="cursor-pointer hover:bg-primary hover:text-primary-foreground"
                    onClick={() => setSearch(tag)}
                  >
                    {tag}
                  </Badge>
                ))}
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Batch Actions */}
      {selectedNotes.length > 0 && (
        <Card className="mb-6 border-blue-200 bg-blue-50 dark:bg-blue-950/20 dark:border-blue-800">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium">
                  已选择 {selectedNotes.length} 个笔记
                </span>
                <Button 
                  variant="outline" 
                  size="sm" 
                  onClick={handleSelectAll}
                >
                  {selectedNotes.length === filteredNotes.length ? '取消全选' : '全选'}
                </Button>
              </div>
              
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleBatchAction('makePublic')}
                  disabled={isBatchUpdating}
                >
                  <Globe className="w-4 h-4 mr-1" />
                  设为公开
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleBatchAction('makePrivate')}
                  disabled={isBatchUpdating}
                >
                  <Lock className="w-4 h-4 mr-1" />
                  设为私有
                </Button>
                <Button
                  variant="destructive"
                  size="sm"
                  onClick={() => handleBatchAction('delete')}
                  disabled={isBatchDeleting}
                >
                  删除选中
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Notes List */}
      <div className="space-y-4">
        {isLoading ? (
          <div className="text-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
            <p className="text-gray-500 mt-4">加载笔记中...</p>
          </div>
        ) : filteredNotes.length > 0 ? (
          filteredNotes.map(note => (
            <div key={note.id} className="flex items-start gap-3">
              <input
                type="checkbox"
                checked={selectedNotes.includes(note.id)}
                onChange={(e) => handleSelectNote(note.id, e.target.checked)}
                className="mt-6"
              />
              <div className="flex-1">
                <NoteCard 
                  note={note}
                  showActions={true}
                  onEdit={() => {
                    // Navigate to edit - could implement inline editing or modal
                    console.log('Edit note:', note.id);
                  }}
                />
              </div>
            </div>
          ))
        ) : (
          <div className="text-center py-12">
            {search ? (
              <>
                <Search className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                <p className="text-gray-500 mb-2">没有找到匹配的笔记</p>
                <p className="text-gray-400 text-sm">尝试使用不同的关键词搜索</p>
                <Button 
                  variant="outline" 
                  onClick={() => setSearch('')}
                  className="mt-4"
                >
                  清除搜索
                </Button>
              </>
            ) : (
              <>
                <FileText className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                <p className="text-gray-500 mb-2">还没有任何笔记</p>
                <p className="text-gray-400 text-sm">开始刷题并记录笔记吧</p>
                <Button 
                  onClick={() => window.location.href = '/codetop'}
                  className="mt-4"
                >
                  开始刷题
                </Button>
              </>
            )}
          </div>
        )}
      </div>

      {/* Pagination */}
      {notes && notes.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-8">
          <Button
            variant="outline"
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
          >
            上一页
          </Button>
          
          <span className="text-sm text-gray-600 dark:text-gray-400">
            第 {page + 1} 页，共 {notes.totalPages} 页
          </span>
          
          <Button
            variant="outline"
            onClick={() => setPage(Math.min(notes.totalPages - 1, page + 1))}
            disabled={page >= notes.totalPages - 1}
          >
            下一页
          </Button>
        </div>
      )}
    </div>
  );
}
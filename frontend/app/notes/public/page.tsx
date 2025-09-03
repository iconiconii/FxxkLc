'use client';

import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { NoteCard } from '@/components/notes';
import { usePopularNotes, useNoteSearch } from '@/hooks/use-notes';
import { Search, TrendingUp, Star, Eye } from 'lucide-react';

export default function PublicNotesPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeTab, setActiveTab] = useState('popular');
  const [, /* sortBy */] = useState('helpfulVotes,desc');
  const [page, setPage] = useState(0);
  const [minVotes, setMinVotes] = useState(1);
  const [minViews, setMinViews] = useState(10);
  
  const { 
    data: popularNotes, 
    isLoading: isLoadingPopular 
  } = usePopularNotes(minVotes, minViews, page, 20);
  
  const { 
    data: searchResults, 
    isLoading: isSearching 
  } = useNoteSearch(searchQuery, page, 20);
  
  const currentData = activeTab === 'search' ? searchResults : popularNotes;
  const isLoading = activeTab === 'search' ? isSearching : isLoadingPopular;
  
  const handleSearch = () => {
    if (searchQuery.trim()) {
      setActiveTab('search');
      setPage(0);
    }
  };
  
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  return (
    <div className="container mx-auto p-6 max-w-6xl">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-2">社区笔记</h1>
        <p className="text-gray-600 dark:text-gray-400">
          发现和学习其他用户分享的优秀算法题笔记
        </p>
      </div>

      {/* Search Section */}
      <Card className="mb-6">
        <CardContent className="p-4">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex-1">
              <div className="relative">
                <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                <Input
                  placeholder="搜索笔记内容、标签或关键词..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onKeyPress={handleKeyPress}
                  className="pl-10"
                />
              </div>
            </div>
            
            <Button onClick={handleSearch} disabled={!searchQuery.trim()}>
              <Search className="w-4 h-4 mr-2" />
              搜索
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Content Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
        <div className="flex items-center justify-between mb-6">
          <TabsList>
            <TabsTrigger value="popular" className="flex items-center gap-2">
              <TrendingUp className="w-4 h-4" />
              热门笔记
            </TabsTrigger>
            {searchQuery && (
              <TabsTrigger value="search" className="flex items-center gap-2">
                <Search className="w-4 h-4" />
                搜索结果
              </TabsTrigger>
            )}
          </TabsList>
          
          {/* Filters for popular notes */}
          {activeTab === 'popular' && (
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2 text-sm">
                <span>最少投票:</span>
                <Select 
                  value={minVotes.toString()} 
                  onValueChange={(value) => setMinVotes(parseInt(value))}
                >
                  <SelectTrigger className="w-20">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="1">1</SelectItem>
                    <SelectItem value="3">3</SelectItem>
                    <SelectItem value="5">5</SelectItem>
                    <SelectItem value="10">10</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              
              <div className="flex items-center gap-2 text-sm">
                <span>最少浏览:</span>
                <Select 
                  value={minViews.toString()} 
                  onValueChange={(value) => setMinViews(parseInt(value))}
                >
                  <SelectTrigger className="w-20">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="10">10</SelectItem>
                    <SelectItem value="50">50</SelectItem>
                    <SelectItem value="100">100</SelectItem>
                    <SelectItem value="500">500</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          )}
        </div>
        
        <TabsContent value="popular" className="space-y-4">
          {/* Popular Notes Stats */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <Star className="w-4 h-4" />
                  筛选条件
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-sm text-gray-600 dark:text-gray-400">
                  至少 {minVotes} 个投票，{minViews} 次浏览
                </div>
              </CardContent>
            </Card>
            
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <TrendingUp className="w-4 h-4" />
                  找到笔记
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {popularNotes?.totalElements || 0}
                </div>
              </CardContent>
            </Card>
            
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <Eye className="w-4 h-4" />
                  当前页面
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {page + 1} / {popularNotes?.totalPages || 1}
                </div>
              </CardContent>
            </Card>
          </div>
          
          {/* Popular Notes List */}
          <div className="space-y-4">
            {isLoading ? (
              <div className="text-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
                <p className="text-gray-500 mt-4">加载热门笔记中...</p>
              </div>
            ) : popularNotes?.content?.length ? (
              popularNotes.content.map((note, index) => (
                <div key={note.id} className="relative">
                  {/* Ranking badge for top notes */}
                  {page === 0 && index < 3 && (
                    <Badge 
                      className="absolute -top-2 -left-2 z-10"
                      variant={index === 0 ? 'default' : 'secondary'}
                    >
                      #{index + 1}
                    </Badge>
                  )}
                  <NoteCard 
                    note={note}
                    showActions={false}
                    showVoting={true}
                  />
                </div>
              ))
            ) : (
              <div className="text-center py-12">
                <TrendingUp className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                <p className="text-gray-500 mb-2">暂无符合条件的热门笔记</p>
                <p className="text-gray-400 text-sm">尝试调整筛选条件</p>
              </div>
            )}
          </div>
        </TabsContent>
        
        {searchQuery && (
          <TabsContent value="search" className="space-y-4">
            {/* Search Results Stats */}
            <Card className="mb-6">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-600 dark:text-gray-400">
                      搜索 &quot;<span className="font-medium">{searchQuery}</span>&quot;
                    </p>
                    {searchResults && (
                      <p className="text-sm text-gray-500 mt-1">
                        找到 {searchResults.totalElements} 个结果
                      </p>
                    )}
                  </div>
                  
                  <Button 
                    variant="outline" 
                    size="sm"
                    onClick={() => {
                      setSearchQuery('');
                      setActiveTab('popular');
                    }}
                  >
                    清除搜索
                  </Button>
                </div>
              </CardContent>
            </Card>
            
            {/* Search Results List */}
            <div className="space-y-4">
              {isLoading ? (
                <div className="text-center py-12">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
                  <p className="text-gray-500 mt-4">搜索中...</p>
                </div>
              ) : searchResults?.content?.length ? (
                searchResults.content.map(note => (
                  <NoteCard 
                    key={note.id}
                    note={note}
                    showActions={false}
                    showVoting={true}
                    highlightQuery={searchQuery}
                  />
                ))
              ) : (
                <div className="text-center py-12">
                  <Search className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                  <p className="text-gray-500 mb-2">没有找到相关笔记</p>
                  <p className="text-gray-400 text-sm">尝试使用不同的关键词搜索</p>
                </div>
              )}
            </div>
          </TabsContent>
        )}
      </Tabs>

      {/* Pagination */}
      {currentData && currentData.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-8">
          <Button
            variant="outline"
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
          >
            上一页
          </Button>
          
          <span className="text-sm text-gray-600 dark:text-gray-400">
            第 {page + 1} 页，共 {currentData.totalPages} 页
          </span>
          
          <Button
            variant="outline"
            onClick={() => setPage(Math.min(currentData.totalPages - 1, page + 1))}
            disabled={page >= currentData.totalPages - 1}
          >
            下一页
          </Button>
        </div>
      )}
    </div>
  );
}
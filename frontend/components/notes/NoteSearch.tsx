'use client';

import React, { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { Search, X, Tag, TrendingUp } from 'lucide-react';

interface NoteSearchProps {
  onSearch: (query: string) => void;
  onTagClick?: (tag: string) => void;
  popularTags?: string[];
  className?: string;
}

export function NoteSearch({ 
  onSearch, 
  onTagClick,
  popularTags = [],
  className = '' 
}: NoteSearchProps) {
  const [query, setQuery] = useState('');
  const [recentSearches, setRecentSearches] = useState<string[]>([]);

  const handleSearch = () => {
    if (!query.trim()) return;
    
    onSearch(query.trim());
    
    // Add to recent searches
    setRecentSearches(prev => {
      const newSearches = [query.trim(), ...prev.filter(s => s !== query.trim())];
      return newSearches.slice(0, 5); // Keep only last 5 searches
    });
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  const clearQuery = () => {
    setQuery('');
  };

  const handleRecentSearch = (searchTerm: string) => {
    setQuery(searchTerm);
    onSearch(searchTerm);
  };

  const removeRecentSearch = (searchTerm: string) => {
    setRecentSearches(prev => prev.filter(s => s !== searchTerm));
  };

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Search Input */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
        <Input
          placeholder="搜索笔记内容、标签、复杂度..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyPress={handleKeyPress}
          className="pl-10 pr-20"
        />
        {query && (
          <Button
            variant="ghost"
            size="sm"
            onClick={clearQuery}
            className="absolute right-12 top-1/2 transform -translate-y-1/2 h-6 w-6 p-0"
          >
            <X className="h-4 w-4" />
          </Button>
        )}
        <Button
          onClick={handleSearch}
          disabled={!query.trim()}
          className="absolute right-1 top-1/2 transform -translate-y-1/2 h-8"
          size="sm"
        >
          搜索
        </Button>
      </div>

      {/* Search Suggestions */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Popular Tags */}
        {popularTags.length > 0 && (
          <Card className="p-4">
            <div className="flex items-center gap-2 mb-3">
              <TrendingUp className="w-4 h-4 text-gray-500" />
              <h3 className="font-medium text-gray-900 dark:text-gray-100">热门标签</h3>
            </div>
            <div className="flex flex-wrap gap-2">
              {popularTags.slice(0, 8).map((tag) => (
                <Badge
                  key={tag}
                  variant="outline"
                  className="cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-800"
                  onClick={() => onTagClick?.(tag)}
                >
                  <Tag className="w-3 h-3 mr-1" />
                  {tag}
                </Badge>
              ))}
            </div>
          </Card>
        )}

        {/* Recent Searches */}
        {recentSearches.length > 0 && (
          <Card className="p-4">
            <div className="flex items-center gap-2 mb-3">
              <Search className="w-4 h-4 text-gray-500" />
              <h3 className="font-medium text-gray-900 dark:text-gray-100">最近搜索</h3>
            </div>
            <div className="space-y-2">
              {recentSearches.map((searchTerm, index) => (
                <div
                  key={index}
                  className="flex items-center justify-between group"
                >
                  <button
                    onClick={() => handleRecentSearch(searchTerm)}
                    className="text-sm text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100 flex-1 text-left"
                  >
                    {searchTerm}
                  </button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => removeRecentSearch(searchTerm)}
                    className="h-6 w-6 p-0 opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <X className="h-3 w-3" />
                  </Button>
                </div>
              ))}
            </div>
          </Card>
        )}
      </div>

      {/* Search Tips */}
      <Card className="p-4 bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800">
        <h3 className="font-medium text-blue-900 dark:text-blue-100 mb-2">搜索技巧</h3>
        <div className="text-sm text-blue-800 dark:text-blue-200 space-y-1">
          <p>• 使用 <code className="bg-blue-100 dark:bg-blue-800 px-1 rounded">标签:动态规划</code> 搜索特定标签</p>
          <p>• 使用 <code className="bg-blue-100 dark:bg-blue-800 px-1 rounded">复杂度:O(n)</code> 搜索特定复杂度</p>
          <p>• 使用 <code className="bg-blue-100 dark:bg-blue-800 px-1 rounded">语言:java</code> 搜索特定编程语言</p>
          <p>• 直接输入关键词搜索笔记内容</p>
        </div>
      </Card>
    </div>
  );
}


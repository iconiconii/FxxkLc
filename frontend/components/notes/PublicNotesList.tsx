'use client';

import React, { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Card } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Search, Filter, TrendingUp, Clock, RefreshCw, AlertCircle } from 'lucide-react';
import { notesAPI, PublicNoteViewDTO, PageResponse, ApiError, ProblemNoteDTO } from '@/lib/notes-api';
import { NoteCard } from './NoteCard';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { NoteViewer } from './NoteViewer';

interface PublicNotesListProps {
  problemId?: number;
  className?: string;
}

const SORT_OPTIONS = [
  { value: 'helpfulVotes,desc', label: '最有用' },
  { value: 'viewCount,desc', label: '最热门' },
  { value: 'createdAt,desc', label: '最新' },
  { value: 'createdAt,asc', label: '最早' },
];

export function PublicNotesList({ problemId, className = '' }: PublicNotesListProps) {
  const [notes, setNotes] = useState<PublicNoteViewDTO[]>([]);
  const [popularNotes, setPopularNotes] = useState<PublicNoteViewDTO[]>([]);
  const [searchResults, setSearchResults] = useState<PublicNoteViewDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Pagination
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  
  // Filters and search
  const [sortBy, setSortBy] = useState('helpfulVotes,desc');
  const [searchQuery, setSearchQuery] = useState('');
  const [activeTab, setActiveTab] = useState(problemId ? 'problem' : 'popular');
  
  // Selected note for viewing
  const [selectedNote, setSelectedNote] = useState<PublicNoteViewDTO | null>(null);
  const [noteDialogOpen, setNoteDialogOpen] = useState(false);

  // Load notes based on current filters
  const loadNotes = async (page = 0, resetData = false) => {
    if (loading) return;
    
    setLoading(true);
    setError(null);

    try {
      let response: PageResponse<PublicNoteViewDTO>;
      
      if (activeTab === 'problem' && problemId) {
        response = await notesAPI.getPublicNotes(problemId, page, 20, sortBy);
      } else if (activeTab === 'popular') {
        response = await notesAPI.getPopularNotes(1, 10, page, 20);
      } else if (activeTab === 'search' && searchQuery.trim()) {
        response = await notesAPI.searchNotes(searchQuery.trim(), page, 20);
      } else {
        return;
      }

      if (resetData || page === 0) {
        setNotes(response.content);
        if (activeTab === 'popular') {
          setPopularNotes(response.content);
        } else if (activeTab === 'search') {
          setSearchResults(response.content);
        }
      } else {
        setNotes(prev => [...prev, ...response.content]);
      }

      setCurrentPage(response.number);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);

    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('加载笔记时出现错误');
      }
      console.error('Error loading notes:', err);
    } finally {
      setLoading(false);
    }
  };

  // Load more notes (pagination)
  const loadMoreNotes = () => {
    if (currentPage + 1 < totalPages && !loading) {
      loadNotes(currentPage + 1, false);
    }
  };

  // Handle search
  const handleSearch = async () => {
    if (!searchQuery.trim()) return;
    
    setActiveTab('search');
    setCurrentPage(0);
    await loadNotes(0, true);
  };

  // Handle vote
  const handleVote = async (noteId: number) => {
    try {
      await notesAPI.voteNote(noteId, true);
      
      // Update the note in current list
      const updateNoteVotes = (notesList: PublicNoteViewDTO[]) => 
        notesList.map(note => 
          note.id === noteId 
            ? { ...note, helpfulVotes: note.helpfulVotes + 1 }
            : note
        );

      setNotes(updateNoteVotes);
      if (activeTab === 'popular') {
        setPopularNotes(updateNoteVotes);
      } else if (activeTab === 'search') {
        setSearchResults(updateNoteVotes);
      }

    } catch (err) {
      console.error('Error voting on note:', err);
    }
  };

  // Handle note view
  const handleViewNote = (note: PublicNoteViewDTO) => {
    setSelectedNote(note);
    setNoteDialogOpen(true);
  };

  // Handle share
  const handleShare = async (note: PublicNoteViewDTO) => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: note.title || `笔记 #${note.id}`,
          text: note.contentPreview,
          url: window.location.href + `?noteId=${note.id}`,
        });
      } catch (err) {
        console.error('Error sharing:', err);
      }
    } else {
      // Fallback: copy to clipboard
      const url = window.location.href + `?noteId=${note.id}`;
      await navigator.clipboard.writeText(url);
      alert('链接已复制到剪贴板');
    }
  };

  // Effects
  useEffect(() => {
    const loadInitialNotes = async () => {
      if (activeTab === 'problem' && problemId) {
        await loadNotes(0, true);
      } else if (activeTab === 'popular') {
        await loadNotes(0, true);
      }
    };
    
    loadInitialNotes();
  }, [activeTab, problemId, sortBy]);

  useEffect(() => {
    // Reset pagination when switching tabs
    setCurrentPage(0);
    setNotes([]);
    setError(null);
  }, [activeTab]);

  const getCurrentNotes = () => {
    if (activeTab === 'popular') return popularNotes;
    if (activeTab === 'search') return searchResults;
    return notes;
  };

  const getEmptyMessage = () => {
    if (activeTab === 'problem') return '还没有公开的笔记';
    if (activeTab === 'search') return '没有找到相关笔记';
    return '暂无热门笔记';
  };

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header and Controls */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            公开笔记
          </h2>
          <Button 
            variant="outline" 
            size="sm" 
            onClick={() => loadNotes(0, true)}
            disabled={loading}
          >
            <RefreshCw className={`w-4 h-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            刷新
          </Button>
        </div>

        {/* Search Bar */}
        <div className="flex gap-2">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
            <Input
              placeholder="搜索笔记内容、标签..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
              className="pl-10"
            />
          </div>
          <Button onClick={handleSearch} disabled={!searchQuery.trim() || loading}>
            搜索
          </Button>
        </div>

        {/* Filters */}
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <Filter className="w-4 h-4 text-gray-500" />
            <span className="text-sm text-gray-600 dark:text-gray-400">排序:</span>
            <Select value={sortBy} onValueChange={setSortBy}>
              <SelectTrigger className="w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {SORT_OPTIONS.map(option => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="text-sm text-gray-600 dark:text-gray-400">
            共 {totalElements} 条笔记
          </div>
        </div>
      </div>

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          {problemId && (
            <TabsTrigger value="problem" className="flex items-center gap-2">
              <Clock className="w-4 h-4" />
              本题笔记
            </TabsTrigger>
          )}
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

        <TabsContent value={activeTab} className="mt-6">
          {/* Error State */}
          {error && (
            <Card className="p-6">
              <div className="flex items-center gap-3 text-red-600">
                <AlertCircle className="w-5 h-5" />
                <span>{error}</span>
              </div>
            </Card>
          )}

          {/* Loading State */}
          {loading && getCurrentNotes().length === 0 && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {[...Array(6)].map((_, i) => (
                <Card key={i} className="p-4">
                  <div className="animate-pulse space-y-3">
                    <div className="h-4 bg-gray-300 rounded w-3/4"></div>
                    <div className="h-3 bg-gray-300 rounded w-1/2"></div>
                    <div className="space-y-2">
                      <div className="h-3 bg-gray-300 rounded"></div>
                      <div className="h-3 bg-gray-300 rounded w-5/6"></div>
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          )}

          {/* Notes Grid */}
          {!loading && getCurrentNotes().length > 0 && (
            <div className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {getCurrentNotes().map((note) => (
                  <NoteCard
                    key={note.id}
                    note={note}
                    showActions={true}
                    onView={() => handleViewNote(note)}
                    onVote={() => handleVote(note.id)}
                    onShare={() => handleShare(note)}
                  />
                ))}
              </div>

              {/* Load More Button */}
              {currentPage + 1 < totalPages && (
                <div className="flex justify-center pt-6">
                  <Button
                    variant="outline"
                    onClick={loadMoreNotes}
                    disabled={loading}
                  >
                    {loading ? '加载中...' : '加载更多'}
                  </Button>
                </div>
              )}
            </div>
          )}

          {/* Empty State */}
          {!loading && !error && getCurrentNotes().length === 0 && (
            <Card className="p-12">
              <div className="text-center space-y-4">
                <div className="text-6xl">📝</div>
                <h3 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                  {getEmptyMessage()}
                </h3>
                <p className="text-gray-600 dark:text-gray-400">
                  {activeTab === 'search' 
                    ? '尝试使用不同的关键词搜索' 
                    : '成为第一个分享笔记的人吧！'
                  }
                </p>
              </div>
            </Card>
          )}
        </TabsContent>
      </Tabs>

      {/* Note Viewer Dialog */}
      <Dialog open={noteDialogOpen} onOpenChange={setNoteDialogOpen}>
        <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {selectedNote?.title || `笔记 #${selectedNote?.id}`}
            </DialogTitle>
          </DialogHeader>
          {selectedNote && (
            <div className="mt-4">
              {/* Convert PublicNoteViewDTO to ProblemNoteDTO for viewer */}
              <NoteViewer
                note={{
                  ...selectedNote,
                  userId: 0, // Not available in public view
                  problemId: problemId || 0, // Use problemId from props
                  content: selectedNote.contentPreview, // Use preview content
                  noteType: 'SOLUTION', // Default type
                  solutionApproach: '', // Not available in public view
                  isPublic: true, // Public notes are always public
                  updatedAt: selectedNote.createdAt, // Use creation date
                  codeSnippets: [], // Not available in public view
                } as ProblemNoteDTO}
                showVoting={true}
                onVote={() => handleVote(selectedNote.id)}
                onShare={() => handleShare(selectedNote)}
              />
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}


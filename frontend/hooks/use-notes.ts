/**
 * Custom hook for managing notes data and operations
 * Provides comprehensive notes management functionality with React Query integration
 */

import { useQuery, useMutation, useQueryClient, UseQueryResult } from '@tanstack/react-query';
import { notesAPI, ProblemNoteDTO, CreateNoteRequest, PublicNoteViewDTO, PageResponse, UserNoteStats } from '@/lib/notes-api';
import { toast } from '@/hooks/use-toast';

/**
 * Hook for managing notes for a specific problem
 */
export function useNotes(problemId: number | null, enabled: boolean = true) {
  const queryClient = useQueryClient();
  
  // Get user's note for the specific problem
  const { 
    data: userNote, 
    isLoading, 
    error,
    refetch: refetchUserNote 
  } = useQuery({
    queryKey: ['user-note', problemId],
    queryFn: () => problemId ? notesAPI.getUserNote(problemId) : null,
    enabled: !!problemId && enabled,
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
  
  // Create or update note mutation
  const createOrUpdateMutation = useMutation({
    mutationFn: async (noteData: CreateNoteRequest) => {
      return notesAPI.createOrUpdateNote(noteData);
    },
    onSuccess: (data) => {
      // Update the specific note in cache
      queryClient.setQueryData(['user-note', data.problemId], data);
      
      // Invalidate related queries
      queryClient.invalidateQueries({ queryKey: ['user-notes'] });
      queryClient.invalidateQueries({ queryKey: ['user-stats'] });
      queryClient.invalidateQueries({ queryKey: ['public-notes', data.problemId] });
      
      toast({
        title: '保存成功',
        description: '笔记已保存',
      });
    },
    onError: (error: any) => {
      console.error('Failed to save note:', error);
      toast({
        title: '保存失败',
        description: error.message || '保存笔记时发生错误',
        variant: 'destructive',
      });
    },
  });
  
  // Delete note mutation
  const deleteMutation = useMutation({
    mutationFn: async (noteId: number) => {
      return notesAPI.deleteNote(noteId);
    },
    onSuccess: () => {
      if (problemId) {
        queryClient.setQueryData(['user-note', problemId], null);
      }
      
      // Invalidate related queries
      queryClient.invalidateQueries({ queryKey: ['user-notes'] });
      queryClient.invalidateQueries({ queryKey: ['user-stats'] });
      
      toast({
        title: '删除成功',
        description: '笔记已删除',
      });
    },
    onError: (error: any) => {
      console.error('Failed to delete note:', error);
      toast({
        title: '删除失败',
        description: error.message || '删除笔记时发生错误',
        variant: 'destructive',
      });
    },
  });
  
  // Vote on note mutation
  const voteMutation = useMutation({
    mutationFn: async ({ noteId, helpful }: { noteId: number; helpful: boolean }) => {
      return notesAPI.voteNote(noteId, helpful);
    },
    onSuccess: () => {
      // Invalidate queries to refresh vote counts
      queryClient.invalidateQueries({ queryKey: ['public-notes'] });
      queryClient.invalidateQueries({ queryKey: ['user-note'] });
      
      toast({
        title: '投票成功',
        description: '感谢您的反馈',
      });
    },
    onError: (error: any) => {
      console.error('Failed to vote on note:', error);
      toast({
        title: '投票失败',
        description: error.message || '投票时发生错误',
        variant: 'destructive',
      });
    },
  });
  
  // Update note visibility mutation
  const updateVisibilityMutation = useMutation({
    mutationFn: async ({ noteId, isPublic }: { noteId: number; isPublic: boolean }) => {
      return notesAPI.updateNoteVisibility(noteId, isPublic);
    },
    onSuccess: (_, { isPublic }) => {
      // Optimistically update the note data
      if (problemId && userNote) {
        const updatedNote = { ...userNote, isPublic };
        queryClient.setQueryData(['user-note', problemId], updatedNote);
      }
      
      // Invalidate related queries
      queryClient.invalidateQueries({ queryKey: ['user-notes'] });
      queryClient.invalidateQueries({ queryKey: ['public-notes'] });
      queryClient.invalidateQueries({ queryKey: ['user-stats'] });
      
      toast({
        title: '设置成功',
        description: `笔记已设为${isPublic ? '公开' : '私有'}`,
      });
    },
    onError: (error: any) => {
      console.error('Failed to update note visibility:', error);
      toast({
        title: '设置失败',
        description: error.message || '更新笔记可见性时发生错误',
        variant: 'destructive',
      });
    },
  });
  
  return {
    // Data
    userNote,
    isLoading,
    error,
    
    // Actions
    createOrUpdateNote: createOrUpdateMutation.mutate,
    deleteNote: deleteMutation.mutate,
    voteNote: voteMutation.mutate,
    updateVisibility: updateVisibilityMutation.mutate,
    refetchUserNote,
    
    // Loading states
    isCreating: createOrUpdateMutation.isPending,
    isDeleting: deleteMutation.isPending,
    isVoting: voteMutation.isPending,
    isUpdatingVisibility: updateVisibilityMutation.isPending,
  };
}

/**
 * Hook for managing public notes for a specific problem
 */
export function usePublicNotes(problemId: number, page = 0, size = 20, sort = 'helpfulVotes,desc') {
  return useQuery({
    queryKey: ['public-notes', problemId, page, size, sort],
    queryFn: () => notesAPI.getPublicNotes(problemId, page, size, sort),
    enabled: !!problemId,
    staleTime: 2 * 60 * 1000, // 2 minutes
    gcTime: 5 * 60 * 1000, // 5 minutes
  });
}

/**
 * Hook for managing user's personal notes
 */
export function useUserNotes(page = 0, size = 20, sort = 'updatedAt,desc') {
  const queryClient = useQueryClient();
  
  const query = useQuery({
    queryKey: ['user-notes', page, size, sort],
    queryFn: () => notesAPI.getUserNotes(page, size, sort),
    staleTime: 1 * 60 * 1000, // 1 minute
    gcTime: 5 * 60 * 1000, // 5 minutes
  });
  
  // Batch update visibility mutation
  const batchUpdateVisibilityMutation = useMutation({
    mutationFn: async ({ noteIds, isPublic }: { noteIds: number[]; isPublic: boolean }) => {
      return notesAPI.batchUpdateVisibility(noteIds, isPublic);
    },
    onSuccess: (_, { isPublic, noteIds }) => {
      // Invalidate user notes to refresh the list
      queryClient.invalidateQueries({ queryKey: ['user-notes'] });
      queryClient.invalidateQueries({ queryKey: ['user-stats'] });
      queryClient.invalidateQueries({ queryKey: ['public-notes'] });
      
      toast({
        title: '批量更新成功',
        description: `已将 ${noteIds.length} 个笔记设为${isPublic ? '公开' : '私有'}`,
      });
    },
    onError: (error: any) => {
      console.error('Failed to batch update visibility:', error);
      toast({
        title: '批量更新失败',
        description: error.message || '批量更新可见性时发生错误',
        variant: 'destructive',
      });
    },
  });
  
  // Batch delete mutation
  const batchDeleteMutation = useMutation({
    mutationFn: async (noteIds: number[]) => {
      return notesAPI.batchDeleteNotes(noteIds);
    },
    onSuccess: (_, noteIds) => {
      // Invalidate user notes to refresh the list
      queryClient.invalidateQueries({ queryKey: ['user-notes'] });
      queryClient.invalidateQueries({ queryKey: ['user-stats'] });
      
      toast({
        title: '批量删除成功',
        description: `已删除 ${noteIds.length} 个笔记`,
      });
    },
    onError: (error: any) => {
      console.error('Failed to batch delete notes:', error);
      toast({
        title: '批量删除失败',
        description: error.message || '批量删除笔记时发生错误',
        variant: 'destructive',
      });
    },
  });
  
  return {
    ...query,
    batchUpdateVisibility: batchUpdateVisibilityMutation.mutate,
    batchDelete: batchDeleteMutation.mutate,
    isBatchUpdating: batchUpdateVisibilityMutation.isPending,
    isBatchDeleting: batchDeleteMutation.isPending,
  };
}

/**
 * Hook for searching notes
 */
export function useNoteSearch(query: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['note-search', query, page, size],
    queryFn: () => notesAPI.searchNotes(query, page, size),
    enabled: query.length >= 2, // Only search with at least 2 characters
    staleTime: 30 * 1000, // 30 seconds
    gcTime: 2 * 60 * 1000, // 2 minutes
  });
}

/**
 * Hook for getting notes by tag
 */
export function useNotesByTag(tag: string, limit = 50) {
  return useQuery({
    queryKey: ['notes-by-tag', tag, limit],
    queryFn: () => notesAPI.getNotesByTag(tag, limit),
    enabled: !!tag,
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
}

/**
 * Hook for getting user note statistics
 */
export function useUserNoteStats() {
  return useQuery({
    queryKey: ['user-stats'],
    queryFn: () => notesAPI.getUserStats(),
    staleTime: 1 * 60 * 1000, // 1 minute
    gcTime: 5 * 60 * 1000, // 5 minutes
  });
}

/**
 * Hook for getting popular notes
 */
export function usePopularNotes(minVotes = 1, minViews = 10, page = 0, size = 20) {
  return useQuery({
    queryKey: ['popular-notes', minVotes, minViews, page, size],
    queryFn: () => notesAPI.getPopularNotes(minVotes, minViews, page, size),
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
}

/**
 * Hook for prefetching notes data
 */
export function usePrefetchNotes() {
  const queryClient = useQueryClient();
  
  const prefetchUserNote = (problemId: number) => {
    queryClient.prefetchQuery({
      queryKey: ['user-note', problemId],
      queryFn: () => notesAPI.getUserNote(problemId),
      staleTime: 5 * 60 * 1000,
    });
  };
  
  const prefetchPublicNotes = (problemId: number) => {
    queryClient.prefetchQuery({
      queryKey: ['public-notes', problemId, 0, 20, 'helpfulVotes,desc'],
      queryFn: () => notesAPI.getPublicNotes(problemId, 0, 20, 'helpfulVotes,desc'),
      staleTime: 2 * 60 * 1000,
    });
  };
  
  return {
    prefetchUserNote,
    prefetchPublicNotes,
  };
}

// Export types for easy importing
export type {
  ProblemNoteDTO,
  CreateNoteRequest,
  PublicNoteViewDTO,
  PageResponse,
  UserNoteStats,
} from '@/lib/notes-api';

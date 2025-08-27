'use client';

import React from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card';
import { ThumbsUp, Eye, Edit, Trash2, Share2, Lock, Globe, Clock } from 'lucide-react';
import { ProblemNoteDTO, PublicNoteViewDTO } from '@/lib/notes-api';

interface NoteCardProps {
  note: ProblemNoteDTO | PublicNoteViewDTO;
  showActions?: boolean;
  showEdit?: boolean;
  showDelete?: boolean;
  showVoting?: boolean;
  highlightQuery?: string;
  onEdit?: () => void;
  onDelete?: () => void;
  onView?: () => void;
  onVote?: (helpful: boolean) => void;
  onShare?: () => void;
  className?: string;
}

export function NoteCard({ 
  note,
  showActions = false,
  showEdit = false,
  showDelete = false,
  showVoting = false,
  highlightQuery,
  onEdit,
  onDelete,
  onView,
  onVote,
  onShare,
  className = ''
}: NoteCardProps) {
  const formatDate = (dateString: string) => {
    try {
      return new Date(dateString).toLocaleDateString('zh-CN', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return '未知时间';
    }
  };

  const isFullNote = (note: ProblemNoteDTO | PublicNoteViewDTO): note is ProblemNoteDTO => {
    return 'userId' in note && 'content' in note;
  };

  const isPublicView = (note: ProblemNoteDTO | PublicNoteViewDTO): note is PublicNoteViewDTO => {
    return 'contentPreview' in note;
  };

  const getContent = () => {
    if (isPublicView(note)) {
      return note.contentPreview;
    }
    if (isFullNote(note) && note.content) {
      // Truncate content for card view
      return note.content.length > 200 
        ? note.content.substring(0, 200) + '...' 
        : note.content;
    }
    return '';
  };

  const getTitle = () => {
    return note.title || `笔记 #${note.id}`;
  };

  return (
    <Card 
      className={`hover:shadow-md transition-shadow cursor-pointer ${className}`}
      onClick={onView}
    >
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="flex-1 min-w-0">
            <h3 className="font-semibold text-lg text-gray-900 dark:text-gray-100 truncate">
              {getTitle()}
            </h3>
            
            {/* Metadata */}
            <div className="flex items-center gap-3 mt-2 text-sm text-gray-600 dark:text-gray-400">
              <div className="flex items-center gap-1">
                <Clock className="w-4 h-4" />
                <span>{formatDate(note.createdAt)}</span>
              </div>
              
              <div className="flex items-center gap-1">
                <Eye className="w-4 h-4" />
                <span>{note.viewCount}</span>
              </div>
              
              <div className="flex items-center gap-1">
                <ThumbsUp className="w-4 h-4" />
                <span>{note.helpfulVotes}</span>
              </div>
            </div>
          </div>
          
          {/* Visibility indicator */}
          <div className="ml-2">
            {isFullNote(note) && (
              <div title={note.isPublic ? "公开" : "私有"}>
                {note.isPublic ? (
                  <Globe className="w-4 h-4 text-green-600" />
                ) : (
                  <Lock className="w-4 h-4 text-gray-400" />
                )}
              </div>
            )}
          </div>
        </div>
      </CardHeader>

      <CardContent className="pb-3">
        {/* Content preview */}
        {getContent() && (
          <p className="text-sm text-gray-700 dark:text-gray-300 line-clamp-3 mb-3">
            {getContent()}
          </p>
        )}

        {/* Complexity badges */}
        <div className="flex flex-wrap gap-2 mb-3">
          {note.timeComplexity && (
            <Badge variant="secondary" className="text-xs">
              时间: {note.timeComplexity}
            </Badge>
          )}
          {note.spaceComplexity && (
            <Badge variant="secondary" className="text-xs">
              空间: {note.spaceComplexity}
            </Badge>
          )}
          {isPublicView(note) && note.primaryLanguage && (
            <Badge variant="outline" className="text-xs">
              {note.primaryLanguage}
            </Badge>
          )}
          {isFullNote(note) && note.noteType && (
            <Badge variant="outline" className="text-xs">
              {note.noteType}
            </Badge>
          )}
        </div>

        {/* Tags */}
        {note.tags && note.tags.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {note.tags.slice(0, 3).map(tag => (
              <Badge key={tag} variant="outline" className="text-xs">
                #{tag}
              </Badge>
            ))}
            {note.tags.length > 3 && (
              <Badge variant="outline" className="text-xs">
                +{note.tags.length - 3}
              </Badge>
            )}
          </div>
        )}
      </CardContent>

      {showActions && (
        <CardFooter className="pt-0">
          <div className="flex items-center justify-between w-full">
            <div className="flex gap-2">
              {showEdit && (
                <Button 
                  variant="outline" 
                  size="sm" 
                  onClick={(e) => {
                    e.stopPropagation();
                    onEdit?.();
                  }}
                >
                  <Edit className="w-4 h-4 mr-1" />
                  编辑
                </Button>
              )}
              
              {showDelete && (
                <Button 
                  variant="outline" 
                  size="sm"
                  onClick={(e) => {
                    e.stopPropagation();
                    onDelete?.();
                  }}
                  className="text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-900/20"
                >
                  <Trash2 className="w-4 h-4 mr-1" />
                  删除
                </Button>
              )}
            </div>

            <div className="flex gap-2">
              <Button 
                variant="outline" 
                size="sm" 
                onClick={(e) => {
                  e.stopPropagation();
                  onShare?.();
                }}
              >
                <Share2 className="w-4 h-4 mr-1" />
                分享
              </Button>
              
              {onVote && (
                <Button 
                  variant="outline" 
                  size="sm" 
                  onClick={(e) => {
                    e.stopPropagation();
                    onVote(true);
                  }}
                  className="hover:bg-green-50 hover:text-green-700 dark:hover:bg-green-900/20"
                >
                  <ThumbsUp className="w-4 h-4 mr-1" />
                  有用
                </Button>
              )}
            </div>
          </div>
        </CardFooter>
      )}
    </Card>
  );
}


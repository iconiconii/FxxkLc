'use client';

import React from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ThumbsUp, Eye, Share2, Edit, Clock } from 'lucide-react';
import { ProblemNoteDTO, CodeSnippetDTO } from '@/lib/notes-api';
import { MarkdownRenderer } from './MarkdownRenderer';

interface NoteViewerProps {
  note: ProblemNoteDTO;
  showVoting?: boolean;
  showEdit?: boolean;
  onVote?: (helpful: boolean) => void;
  onEdit?: () => void;
  onShare?: () => void;
  className?: string;
}

export function NoteViewer({ 
  note, 
  showVoting = false, 
  showEdit = false,
  onVote,
  onEdit,
  onShare,
  className = ''
}: NoteViewerProps) {
  const formatDate = (dateString: string) => {
    try {
      return new Date(dateString).toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return '未知时间';
    }
  };

  const CodeSnippetRenderer = ({ snippet }: { snippet: CodeSnippetDTO }) => (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <Badge variant="secondary" className="text-xs">
          {snippet.language}
        </Badge>
        {snippet.type && (
          <Badge variant="outline" className="text-xs">
            {snippet.type}
          </Badge>
        )}
      </div>
      <MarkdownRenderer 
        content={`\`\`\`${snippet.language}\n${snippet.code}\n\`\`\``}
        className="!mt-0"
      />
      {snippet.explanation && (
        <div className="text-sm text-gray-600 dark:text-gray-400 bg-gray-50 dark:bg-gray-800 p-3 rounded">
          <strong>说明:</strong> {snippet.explanation}
        </div>
      )}
      {snippet.complexityNote && (
        <div className="text-xs text-gray-500 dark:text-gray-500">
          <strong>复杂度说明:</strong> {snippet.complexityNote}
        </div>
      )}
    </div>
  );

  return (
    <Card className={`p-6 space-y-6 ${className}`}>
      {/* Header with title and metadata */}
      <div className="space-y-3">
        {note.title && (
          <h3 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
            {note.title}
          </h3>
        )}
        
        <div className="flex flex-wrap items-center gap-4 text-sm text-gray-600 dark:text-gray-400">
          <div className="flex items-center gap-1">
            <Clock className="w-4 h-4" />
            <span>创建于 {formatDate(note.createdAt)}</span>
          </div>
          
          {note.updatedAt && note.updatedAt !== note.createdAt && (
            <div className="flex items-center gap-1">
              <span>更新于 {formatDate(note.updatedAt)}</span>
            </div>
          )}
          
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
          
          {note.noteType && (
            <Badge variant="outline" className="text-xs">
              {note.noteType}
            </Badge>
          )}
          
          {note.isPublic && (
            <Badge variant="default" className="text-xs">
              公开
            </Badge>
          )}
        </div>
      </div>

      {/* Action buttons */}
      <div className="flex justify-between items-center">
        <div className="flex gap-2">
          {showEdit && (
            <Button variant="outline" size="sm" onClick={onEdit}>
              <Edit className="w-4 h-4 mr-2" />
              编辑
            </Button>
          )}
          <Button variant="outline" size="sm" onClick={onShare}>
            <Share2 className="w-4 h-4 mr-2" />
            分享
          </Button>
        </div>
        
        {showVoting && (
          <Button 
            variant="outline" 
            size="sm" 
            onClick={() => onVote?.(true)}
            className="hover:bg-green-50 hover:text-green-700 dark:hover:bg-green-900/20"
          >
            <ThumbsUp className="w-4 h-4 mr-2" />
            有用 ({note.helpfulVotes})
          </Button>
        )}
      </div>

      {/* Main content */}
      {note.content && (
        <div className="space-y-4">
          <h4 className="font-semibold text-gray-900 dark:text-gray-100">笔记内容</h4>
          <div className="border-l-4 border-blue-500 pl-4">
            <MarkdownRenderer content={note.content} />
          </div>
        </div>
      )}

      {/* Solution approach */}
      {note.solutionApproach && (
        <div className="space-y-4">
          <h4 className="font-semibold text-gray-900 dark:text-gray-100">解题思路</h4>
          <div className="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg border-l-4 border-blue-500">
            <MarkdownRenderer content={note.solutionApproach} />
          </div>
        </div>
      )}

      {/* Code snippets */}
      {note.codeSnippets && note.codeSnippets.length > 0 && (
        <div className="space-y-4">
          <h4 className="font-semibold text-gray-900 dark:text-gray-100">代码实现</h4>
          <div className="space-y-4">
            {note.codeSnippets.map((snippet, index) => (
              <div key={index} className="border border-gray-200 dark:border-gray-700 rounded-lg p-4">
                <CodeSnippetRenderer snippet={snippet} />
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Complexity analysis */}
      {(note.timeComplexity || note.spaceComplexity) && (
        <div className="space-y-3">
          <h4 className="font-semibold text-gray-900 dark:text-gray-100">复杂度分析</h4>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {note.timeComplexity && (
              <div className="bg-yellow-50 dark:bg-yellow-900/20 p-3 rounded-lg">
                <span className="font-medium text-gray-700 dark:text-gray-300">时间复杂度:</span>
                <div className="mt-1">
                  <Badge variant="secondary">{note.timeComplexity}</Badge>
                </div>
              </div>
            )}
            {note.spaceComplexity && (
              <div className="bg-green-50 dark:bg-green-900/20 p-3 rounded-lg">
                <span className="font-medium text-gray-700 dark:text-gray-300">空间复杂度:</span>
                <div className="mt-1">
                  <Badge variant="secondary">{note.spaceComplexity}</Badge>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tags */}
      {note.tags && note.tags.length > 0 && (
        <div className="space-y-3">
          <h4 className="font-semibold text-gray-900 dark:text-gray-100">标签</h4>
          <div className="flex flex-wrap gap-2">
            {note.tags.map(tag => (
              <Badge key={tag} variant="outline" className="text-xs">
                #{tag}
              </Badge>
            ))}
          </div>
        </div>
      )}
    </Card>
  );
}


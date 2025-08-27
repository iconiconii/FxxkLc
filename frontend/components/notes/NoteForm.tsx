'use client';

import React, { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { notesAPI, CreateNoteRequest, ProblemNoteDTO, ApiError } from '@/lib/notes-api';
import { NoteEditor } from './NoteEditor';
import { useToast } from '@/hooks/use-toast';

interface NoteFormProps {
  isOpen: boolean;
  onClose: () => void;
  problemId: number;
  existingNote?: ProblemNoteDTO;
  onSuccess?: (note: ProblemNoteDTO) => void;
}

export function NoteForm({
  isOpen,
  onClose,
  problemId,
  existingNote,
  onSuccess
}: NoteFormProps) {
  const [saving, setSaving] = useState(false);
  const { toast } = useToast();

  const handleSave = async (noteData: CreateNoteRequest) => {
    setSaving(true);
    
    try {
      let savedNote: ProblemNoteDTO;
      
      if (existingNote) {
        // Update existing note
        savedNote = await notesAPI.updateNote(existingNote.id, noteData);
        toast({
          title: "笔记更新成功",
          description: "你的笔记已成功更新",
        });
      } else {
        // Create new note
        savedNote = await notesAPI.createOrUpdateNote(noteData);
        toast({
          title: "笔记创建成功",
          description: "你的笔记已成功保存",
        });
      }
      
      onSuccess?.(savedNote);
      onClose();
      
    } catch (error) {
      console.error('Error saving note:', error);
      
      let errorMessage = '保存笔记时出现错误';
      if (error instanceof ApiError) {
        errorMessage = error.message;
      }
      
      toast({
        title: "保存失败",
        description: errorMessage,
        variant: "destructive",
      });
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    if (saving) return;
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && handleCancel()}>
      <DialogContent className="max-w-7xl max-h-[90vh] overflow-hidden p-0">
        <DialogHeader className="px-6 pt-6 pb-4">
          <DialogTitle className="text-xl">
            {existingNote ? '编辑笔记' : '创建新笔记'}
          </DialogTitle>
        </DialogHeader>
        
        <div className="overflow-y-auto px-6 pb-6">
          <NoteEditor
            initialNote={existingNote}
            problemId={problemId}
            onSave={handleSave}
            onCancel={handleCancel}
          />
        </div>
      </DialogContent>
    </Dialog>
  );
}


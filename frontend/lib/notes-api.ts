/**
 * Notes API client for Problem Note management
 * Handles all note-related API operations with authentication
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || '/api/v1'

export interface ProblemNoteDTO {
  id: number;
  userId: number;
  problemId: number;
  title?: string;
  content: string;
  solutionApproach?: string;
  timeComplexity?: string;
  spaceComplexity?: string;
  tags?: string[];
  isPublic: boolean;
  noteType: string;
  helpfulVotes: number;
  viewCount: number;
  createdAt: string;
  updatedAt: string;
  codeSnippets?: CodeSnippetDTO[];
}

export interface CodeSnippetDTO {
  language: string;
  code: string;
  explanation?: string;
  type?: string;
  complexityNote?: string;
}

export interface CreateNoteRequest {
  problemId: number;
  title?: string;
  content: string;
  noteType: string;
  solutionApproach?: string;
  timeComplexity?: string;
  spaceComplexity?: string;
  tags?: string[];
  isPublic?: boolean;
  codeSnippets?: CreateCodeSnippetRequest[];
}

export interface CreateCodeSnippetRequest {
  language: string;
  code: string;
  explanation?: string;
  type?: string;
  complexityNote?: string;
}

export interface PublicNoteViewDTO {
  id: number;
  title?: string;
  contentPreview: string;
  timeComplexity?: string;
  spaceComplexity?: string;
  tags?: string[];
  helpfulVotes: number;
  viewCount: number;
  createdAt: string;
  primaryLanguage?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface UserNoteStats {
  totalNotes: number;
  publicNotes: number;
  privateNotes: number;
  totalHelpfulVotes: number;
  totalViews: number;
  popularTags: string[];
}

class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public response?: Response
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

// Base JSON headers (auth via HttpOnly cookies)
const jsonHeaders: HeadersInit = { 'Content-Type': 'application/json' }

// Helper function to handle API responses
async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let errorMessage = `API Error: ${response.status} ${response.statusText}`
    
    try {
      const errorData = await response.json()
      errorMessage = errorData.message || errorData.error || errorMessage
    } catch {
      // Fallback to status text if response is not JSON
    }
    
    throw new ApiError(errorMessage, response.status, response)
  }
  
  return response.json()
}

export class NotesAPI {
  private baseUrl: string;
  
  constructor() {
    this.baseUrl = `${API_BASE_URL}/notes`;
  }
  
  /**
   * Create or update a note
   */
  async createOrUpdateNote(request: CreateNoteRequest): Promise<ProblemNoteDTO> {
    const response = await fetch(this.baseUrl, {
      method: 'POST',
      headers: jsonHeaders,
      credentials: 'include',
      body: JSON.stringify(request),
    });
    
    return handleResponse<ProblemNoteDTO>(response);
  }
  
  /**
   * Update an existing note
   */
  async updateNote(noteId: number, request: Partial<CreateNoteRequest>): Promise<ProblemNoteDTO> {
    const response = await fetch(`${this.baseUrl}/${noteId}`, {
      method: 'PUT',
      headers: jsonHeaders,
      credentials: 'include',
      body: JSON.stringify(request),
    });
    
    return handleResponse<ProblemNoteDTO>(response);
  }
  
  /**
   * Get user's note for a specific problem
   */
  async getUserNote(problemId: number): Promise<ProblemNoteDTO | null> {
    const response = await fetch(`${this.baseUrl}/problem/${problemId}`, {
      headers: jsonHeaders,
      credentials: 'include',
    });
    
    if (response.status === 404) {
      return null;
    }
    
    return handleResponse<ProblemNoteDTO>(response);
  }
  
  /**
   * Get user's notes with pagination
   */
  async getUserNotes(page = 0, size = 20, sort = 'updatedAt,desc'): Promise<PageResponse<ProblemNoteDTO>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sort,
    });
    
    const response = await fetch(`${this.baseUrl}/my?${params}`, {
      headers: jsonHeaders,
      credentials: 'include',
    });
    
    return handleResponse<PageResponse<ProblemNoteDTO>>(response);
  }
  
  /**
   * Delete a note
   */
  async deleteNote(noteId: number): Promise<void> {
    const response = await fetch(`${this.baseUrl}/${noteId}`, {
      method: 'DELETE',
      headers: jsonHeaders,
      credentials: 'include',
    });
    
    if (!response.ok) {
      throw new ApiError(`Failed to delete note: ${response.statusText}`, response.status);
    }
  }
  
  /**
   * Get public notes for a problem
   */
  async getPublicNotes(
    problemId: number, 
    page = 0, 
    size = 20, 
    sort = 'helpfulVotes,desc'
  ): Promise<PageResponse<PublicNoteViewDTO>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sort,
    });
    
    const response = await fetch(`${this.baseUrl}/public/problem/${problemId}?${params}`, {
      headers: jsonHeaders,
      credentials: 'include',
    });
    
    return handleResponse<PageResponse<PublicNoteViewDTO>>(response);
  }
  
  /**
   * Get popular public notes
   */
  async getPopularNotes(
    minVotes = 1,
    minViews = 10,
    page = 0,
    size = 20
  ): Promise<PageResponse<PublicNoteViewDTO>> {
    const params = new URLSearchParams({
      minVotes: minVotes.toString(),
      minViews: minViews.toString(),
      page: page.toString(),
      size: size.toString(),
    });
    
    const response = await fetch(`${this.baseUrl}/public/popular?${params}`, {
      headers: jsonHeaders,
      credentials: 'include',
    });
    
    return handleResponse<PageResponse<PublicNoteViewDTO>>(response);
  }
  
  /**
   * Vote on a note (helpful/not helpful)
   */
  async voteNote(noteId: number, helpful: boolean): Promise<void> {
    const params = new URLSearchParams({
      helpful: helpful.toString(),
    });
    
    const response = await fetch(`${this.baseUrl}/${noteId}/vote?${params}`, {
      method: 'PUT',
      headers: jsonHeaders,
      credentials: 'include',
    });
    
    if (!response.ok) {
      throw new ApiError(`Failed to vote on note: ${response.statusText}`, response.status);
    }
  }
  
  /**
   * Update note visibility (public/private)
   */
  async updateNoteVisibility(noteId: number, isPublic: boolean): Promise<void> {
    const params = new URLSearchParams({
      isPublic: isPublic.toString(),
    });
    
    const response = await fetch(`${this.baseUrl}/${noteId}/visibility?${params}`, {
      method: 'PUT',
      headers: jsonHeaders,
      credentials: 'include',
    });
    
    if (!response.ok) {
      throw new ApiError(`Failed to update note visibility: ${response.statusText}`, response.status);
    }
  }
  
  /**
   * Search notes
   */
  async searchNotes(
    query: string,
    page = 0,
    size = 20
  ): Promise<PageResponse<PublicNoteViewDTO>> {
    const params = new URLSearchParams({
      query: query.trim(),
      page: page.toString(),
      size: size.toString(),
    });
    
    const response = await fetch(`${this.baseUrl}/search?${params}`, {
      headers: jsonHeaders,
      credentials: 'include',
    });
    
    return handleResponse<PageResponse<PublicNoteViewDTO>>(response);
  }
  
  /**
   * Get notes by tag
   */
  async getNotesByTag(tag: string, limit = 50): Promise<PublicNoteViewDTO[]> {
    const params = new URLSearchParams({
      limit: limit.toString(),
    });
    
    const response = await fetch(`${this.baseUrl}/tags/${encodeURIComponent(tag)}?${params}`, {
      headers: jsonHeaders,
      credentials: 'include',
    });
    
    return handleResponse<PublicNoteViewDTO[]>(response);
  }
  
  /**
   * Get user note statistics
   */
  async getUserStats(): Promise<UserNoteStats> {
    const response = await fetch(`${this.baseUrl}/stats`, {
      headers: jsonHeaders,
      credentials: 'include',
    });
    
    return handleResponse<UserNoteStats>(response);
  }
  
  /**
   * Batch update note visibility
   */
  async batchUpdateVisibility(noteIds: number[], isPublic: boolean): Promise<void> {
    const params = new URLSearchParams({
      isPublic: isPublic.toString(),
    });
    
    const response = await fetch(`${this.baseUrl}/batch/visibility?${params}`, {
      method: 'PUT',
      headers: jsonHeaders,
      credentials: 'include',
      body: JSON.stringify(noteIds),
    });
    
    if (!response.ok) {
      throw new ApiError(`Failed to batch update visibility: ${response.statusText}`, response.status);
    }
  }
  
  /**
   * Batch delete notes
   */
  async batchDeleteNotes(noteIds: number[]): Promise<void> {
    const response = await fetch(`${this.baseUrl}/batch`, {
      method: 'DELETE',
      headers: jsonHeaders,
      credentials: 'include',
      body: JSON.stringify(noteIds),
    });
    
    if (!response.ok) {
      throw new ApiError(`Failed to batch delete notes: ${response.statusText}`, response.status);
    }
  }
}

// Create singleton instance
export const notesAPI = new NotesAPI();

// Export error class for consumers
export { ApiError };

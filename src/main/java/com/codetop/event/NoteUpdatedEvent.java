package com.codetop.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Event published when a problem note is created or updated.
 * 
 * This event can be used by:
 * - Cache invalidation services
 * - Analytics tracking
 * - Search index updates
 * - User notification systems
 * 
 * @author CodeTop Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class NoteUpdatedEvent extends ApplicationEvent {
    
    private final Long noteId;
    private final Long userId;
    private final Long problemId;
    private final LocalDateTime eventTimestamp;
    
    // Convenience constructor without source
    public NoteUpdatedEvent(Long noteId, Long userId, Long problemId, LocalDateTime eventTimestamp) {
        super(new Object());
        this.noteId = noteId;
        this.userId = userId;
        this.problemId = problemId;
        this.eventTimestamp = eventTimestamp;
    }
}
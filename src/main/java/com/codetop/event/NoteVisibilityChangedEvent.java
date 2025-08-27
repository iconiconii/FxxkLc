package com.codetop.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Event published when note visibility changes.
 * 
 * @author CodeTop Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class NoteVisibilityChangedEvent extends ApplicationEvent {
    
    private final Long noteId;
    private final Long userId;
    private final boolean isPublic;
    private final LocalDateTime eventTimestamp;
    
    public NoteVisibilityChangedEvent(Long noteId, Long userId, boolean isPublic, LocalDateTime eventTimestamp) {
        super(new Object());
        this.noteId = noteId;
        this.userId = userId;
        this.isPublic = isPublic;
        this.eventTimestamp = eventTimestamp;
    }
}
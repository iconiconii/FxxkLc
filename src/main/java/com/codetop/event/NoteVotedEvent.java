package com.codetop.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Event published when a user votes on a note.
 * 
 * @author CodeTop Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class NoteVotedEvent extends ApplicationEvent {
    
    private final Long noteId;
    private final Long voterId;
    private final boolean helpful;
    private final LocalDateTime eventTimestamp;
    
    public NoteVotedEvent(Long noteId, Long voterId, boolean helpful, LocalDateTime eventTimestamp) {
        super(new Object());
        this.noteId = noteId;
        this.voterId = voterId;
        this.helpful = helpful;
        this.eventTimestamp = eventTimestamp;
    }
}
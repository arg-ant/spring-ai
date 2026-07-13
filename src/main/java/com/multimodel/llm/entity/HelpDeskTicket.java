package com.multimodel.llm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a help-desk support ticket raised by a user, persisted in the
 * {@code helpdesk_tickets} table.
 */
@Entity
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Table(name = "helpdesk_tickets")
public class HelpDeskTicket {

    /**
     * Auto-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Username of the person who raised the ticket.
     */
    private String username;

    /**
     * Description of the reported issue.
     */
    private String issue;

    /**
     * Current ticket status, e.g. {@code OPEN}, {@code IN_PROGRESS}, {@code CLOSED}.
     */
    private String status; // e.g., OPEN, IN_PROGRESS, CLOSED

    /**
     * Timestamp when the ticket was created.
     */
    private LocalDateTime createdAt;

    /**
     * Estimated time by which the ticket is expected to be resolved.
     */
    private LocalDateTime eta;
}

package com.vivek.deskflow.ticket;

import java.time.Instant;

import com.vivek.deskflow.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A reply on a ticket. Internal comments are private notes that only staff can see. */
@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private String body;

    @Column(nullable = false)
    private boolean internal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Comment() { }

    public Comment(Long tenantId, Long ticketId, User author, String body, boolean internal, Instant createdAt) {
        this.tenantId = tenantId;
        this.ticketId = ticketId;
        this.author = author;
        this.body = body;
        this.internal = internal;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public Long getTicketId() { return ticketId; }
    public User getAuthor() { return author; }
    public String getBody() { return body; }
    public boolean isInternal() { return internal; }
    public Instant getCreatedAt() { return createdAt; }
}

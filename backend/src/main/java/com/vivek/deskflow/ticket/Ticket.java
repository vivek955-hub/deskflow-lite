package com.vivek.deskflow.ticket;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.vivek.deskflow.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "ai_triaged", nullable = false)
    private boolean aiTriaged;

    @Column(name = "sla_breached", nullable = false)
    private boolean slaBreached;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Ticket() { }

    public Ticket(Long tenantId, String subject, String description, Priority priority, Category category,
                  User requester, Instant createdAt) {
        this.tenantId = tenantId;
        this.subject = subject;
        this.description = description;
        this.priority = priority;
        this.category = category;
        this.requester = requester;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        recalculateDueAt();
    }

    /** The SLA clock always starts when the ticket was created. */
    public void recalculateDueAt() {
        this.dueAt = createdAt.plus(priority.slaHours(), ChronoUnit.HOURS);
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public TicketStatus getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public Category getCategory() { return category; }
    public User getRequester() { return requester; }
    public User getAssignee() { return assignee; }
    public boolean isAiTriaged() { return aiTriaged; }
    public boolean isSlaBreached() { return slaBreached; }
    public Instant getDueAt() { return dueAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(TicketStatus status) { this.status = status; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public void setCategory(Category category) { this.category = category; }
    public void setAssignee(User assignee) { this.assignee = assignee; }
    public void setAiTriaged(boolean aiTriaged) { this.aiTriaged = aiTriaged; }
    public void setSlaBreached(boolean slaBreached) { this.slaBreached = slaBreached; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

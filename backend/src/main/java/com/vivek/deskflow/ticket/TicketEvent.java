package com.vivek.deskflow.ticket;

import java.time.Instant;

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

/** Audit trail: one row for every change made to a ticket. actor is null when the system did it. */
@Entity
@Table(name = "ticket_events")
public class TicketEvent {

    public enum Type { CREATED, AI_TRIAGE, STATUS, PRIORITY, CATEGORY, ASSIGNMENT, SLA_BREACH }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TicketEvent() { }

    public TicketEvent(Long tenantId, Long ticketId, User actor, Type type, String detail, Instant createdAt) {
        this.tenantId = tenantId;
        this.ticketId = ticketId;
        this.actor = actor;
        this.type = type;
        this.detail = detail.length() > 500 ? detail.substring(0, 500) : detail;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getTicketId() { return ticketId; }
    public User getActor() { return actor; }
    public Type getType() { return type; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}

package com.vivek.deskflow.ticket;

public enum TicketStatus {
    OPEN,         // new, nobody is working on it yet
    IN_PROGRESS,  // an agent is working on it
    PENDING,      // waiting for the customer to reply
    RESOLVED,     // fixed; the customer can still reopen by replying
    CLOSED;       // finished for good

    public boolean isActive() {
        return this == OPEN || this == IN_PROGRESS || this == PENDING;
    }
}

package com.vivek.deskflow.ticket;

/** Each priority carries its SLA: how many hours the team has to resolve the ticket. */
public enum Priority {
    LOW(72), MEDIUM(24), HIGH(8), URGENT(4);

    private final int slaHours;

    Priority(int slaHours) {
        this.slaHours = slaHours;
    }

    public int slaHours() { return slaHours; }
}

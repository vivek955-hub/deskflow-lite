package com.vivek.deskflow.user;

/**
 * ADMIN    - runs the company's helpdesk: manages the team, sees everything.
 * AGENT    - support staff: works on any ticket in their company.
 * CUSTOMER - raises tickets and sees only their own.
 */
public enum Role {
    ADMIN, AGENT, CUSTOMER
}

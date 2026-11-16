package com.vivek.deskflow.security;

import com.vivek.deskflow.user.Role;

/**
 * The logged-in user, rebuilt from the JWT on every request.
 * tenantId comes from the signed token, never from the request body or URL,
 * so a user cannot ask for another company's data by changing a parameter.
 */
public record CurrentUser(Long id, Long tenantId, String email, Role role) {

    public boolean isCustomer() { return role == Role.CUSTOMER; }

    public boolean isStaff() { return role == Role.ADMIN || role == Role.AGENT; }
}

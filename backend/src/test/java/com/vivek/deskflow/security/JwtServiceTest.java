package com.vivek.deskflow.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.vivek.deskflow.user.Role;
import com.vivek.deskflow.user.User;

class JwtServiceTest {
    private static final String SECRET = "test-secret-test-secret-test-secret-1234";

    private User userWithId(long id) {
        User user = new User(7L, "Ravi", "ravi@acme.test", "hash", Role.AGENT);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void tokenCarriesUserTenantAndRole() {
        JwtService jwt = new JwtService(SECRET, 60);
        CurrentUser parsed = jwt.parse(jwt.createToken(userWithId(42L)));
        assertEquals(42L, parsed.id());
        assertEquals(7L, parsed.tenantId());
        assertEquals("ravi@acme.test", parsed.email());
        assertEquals(Role.AGENT, parsed.role());
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = new JwtService(SECRET, 60).createToken(userWithId(1L));
        JwtService other = new JwtService("another-secret-another-secret-another-123", 60);
        assertThrows(Exception.class, () -> other.parse(token));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, -1); // already expired when created
        String token = jwt.createToken(userWithId(1L));
        assertThrows(Exception.class, () -> jwt.parse(token));
    }
}

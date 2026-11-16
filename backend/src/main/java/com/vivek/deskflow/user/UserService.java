package com.vivek.deskflow.user;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vivek.deskflow.common.ApiException;
import com.vivek.deskflow.security.CurrentUser;
import com.vivek.deskflow.user.UserDtos.CreateUserRequest;
import com.vivek.deskflow.user.UserDtos.UserResponse;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAll(CurrentUser me) {
        return users.findByTenantIdOrderByNameAsc(me.tenantId()).stream().map(UserResponse::from).toList();
    }

    /** Agents and admins a ticket can be assigned to. */
    @Transactional(readOnly = true)
    public List<UserResponse> listAssignable(CurrentUser me) {
        return users.findByTenantIdAndRoleInAndActiveTrueOrderByNameAsc(me.tenantId(), List.of(Role.ADMIN, Role.AGENT))
                .stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse create(CurrentUser me, CreateUserRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "That email is already registered");
        }
        // The new user always joins the admin's own company.
        User user = new User(me.tenantId(), req.name().trim(), email, passwordEncoder.encode(req.password()), req.role());
        return UserResponse.from(users.save(user));
    }

    @Transactional
    public UserResponse setActive(CurrentUser me, Long userId, boolean active) {
        User user = users.findByIdAndTenantId(userId, me.tenantId()).orElseThrow(() -> ApiException.notFound("User"));
        if (user.getId().equals(me.id()) && !active) {
            throw ApiException.badRequest("You cannot deactivate your own account");
        }
        user.setActive(active);
        return UserResponse.from(user);
    }
}

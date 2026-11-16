package com.vivek.deskflow.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vivek.deskflow.auth.AuthDtos.AuthResponse;
import com.vivek.deskflow.auth.AuthDtos.LoginRequest;
import com.vivek.deskflow.auth.AuthDtos.RegisterOrgRequest;
import com.vivek.deskflow.common.ApiException;
import com.vivek.deskflow.security.CurrentUser;
import com.vivek.deskflow.security.JwtService;
import com.vivek.deskflow.tenant.Tenant;
import com.vivek.deskflow.tenant.TenantRepository;
import com.vivek.deskflow.user.Role;
import com.vivek.deskflow.user.User;
import com.vivek.deskflow.user.UserDtos.UserResponse;
import com.vivek.deskflow.user.UserRepository;

@Service
public class AuthService {
    private final TenantRepository tenants;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(TenantRepository tenants, UserRepository users, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.tenants = tenants;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Signing up creates a brand-new company (tenant) and makes the person its first ADMIN. */
    @Transactional
    public AuthResponse registerOrg(RegisterOrgRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "That email is already registered");
        }
        Tenant tenant = tenants.save(new Tenant(req.companyName().trim(), uniqueSlug(req.companyName())));
        User admin = users.save(new User(tenant.getId(), req.name().trim(), email,
                passwordEncoder.encode(req.password()), Role.ADMIN));
        return new AuthResponse(jwtService.createToken(admin), UserResponse.from(admin), tenant.getName());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        // Same error for "no such email" and "wrong password", so attackers cannot discover which emails exist.
        ApiException invalid = new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
        User user = users.findByEmailIgnoreCase(req.email().trim()).orElseThrow(() -> invalid);
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw invalid;
        }
        if (!user.isActive()) {
            throw ApiException.forbidden("This account has been deactivated");
        }
        return new AuthResponse(jwtService.createToken(user), UserResponse.from(user), companyName(user.getTenantId()));
    }

    @Transactional(readOnly = true)
    public AuthResponse me(CurrentUser me) {
        User user = users.findByIdAndTenantId(me.id(), me.tenantId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please log in"));
        return new AuthResponse(null, UserResponse.from(user), companyName(user.getTenantId()));
    }

    private String companyName(Long tenantId) {
        return tenants.findById(tenantId).map(Tenant::getName).orElse("");
    }

    private String uniqueSlug(String companyName) {
        String base = companyName.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isEmpty()) base = "company";
        String slug = base;
        int n = 2;
        while (tenants.existsBySlug(slug)) {
            slug = base + "-" + n++;
        }
        return slug;
    }
}

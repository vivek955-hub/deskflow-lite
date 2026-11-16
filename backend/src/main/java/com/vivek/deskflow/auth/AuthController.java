package com.vivek.deskflow.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vivek.deskflow.auth.AuthDtos.AuthResponse;
import com.vivek.deskflow.auth.AuthDtos.LoginRequest;
import com.vivek.deskflow.auth.AuthDtos.RegisterOrgRequest;
import com.vivek.deskflow.security.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register-org")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerOrg(@Valid @RequestBody RegisterOrgRequest req) {
        return authService.registerOrg(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal CurrentUser me) {
        return authService.me(me);
    }
}

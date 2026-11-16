package com.vivek.deskflow.user;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vivek.deskflow.security.CurrentUser;
import com.vivek.deskflow.user.UserDtos.CreateUserRequest;
import com.vivek.deskflow.user.UserDtos.SetActiveRequest;
import com.vivek.deskflow.user.UserDtos.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> list(@AuthenticationPrincipal CurrentUser me) {
        return userService.listAll(me);
    }

    @GetMapping("/assignable")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public List<UserResponse> assignable(@AuthenticationPrincipal CurrentUser me) {
        return userService.listAssignable(me);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@AuthenticationPrincipal CurrentUser me, @Valid @RequestBody CreateUserRequest req) {
        return userService.create(me, req);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse setActive(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id,
                                  @Valid @RequestBody SetActiveRequest req) {
        return userService.setActive(me, id, req.active());
    }
}

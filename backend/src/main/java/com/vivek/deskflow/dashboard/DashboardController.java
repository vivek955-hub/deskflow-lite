package com.vivek.deskflow.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vivek.deskflow.dashboard.DashboardService.Dashboard;
import com.vivek.deskflow.security.CurrentUser;

@RestController
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public Dashboard dashboard(@AuthenticationPrincipal CurrentUser me) {
        return dashboardService.build(me);
    }
}

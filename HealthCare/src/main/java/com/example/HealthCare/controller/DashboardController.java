package com.example.HealthCare.controller;

import com.example.HealthCare.dto.DashboardStatsDTO;
import com.example.HealthCare.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Dashboard statistics for the connected user (role aware)")
    public ResponseEntity<DashboardStatsDTO> stats(Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getStats(authentication.getName()));
    }
}
package com.iamnotawhale.amneziabot.api;

import com.iamnotawhale.amneziabot.api.dto.AdminOverviewResponse;
import com.iamnotawhale.amneziabot.api.dto.AdminSubscriptionRow;
import com.iamnotawhale.amneziabot.api.dto.AdminUserRow;
import com.iamnotawhale.amneziabot.service.AdminAuthService;
import com.iamnotawhale.amneziabot.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAuthService adminAuthService;
    private final AdminService adminService;

    public AdminController(AdminAuthService adminAuthService, AdminService adminService) {
        this.adminAuthService = adminAuthService;
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public AdminOverviewResponse overview(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        adminAuthService.authorize(adminToken);
        return adminService.overview();
    }

    @GetMapping("/users")
    public List<AdminUserRow> users(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        adminAuthService.authorize(adminToken);
        return adminService.users();
    }

    @GetMapping("/subscriptions")
    public List<AdminSubscriptionRow> subscriptions(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        adminAuthService.authorize(adminToken);
        return adminService.subscriptions();
    }
}

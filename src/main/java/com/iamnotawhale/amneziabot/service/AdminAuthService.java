package com.iamnotawhale.amneziabot.service;

import com.iamnotawhale.amneziabot.config.AdminProperties;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private final AdminProperties adminProperties;

    public AdminAuthService(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }

    public void authorize(String providedToken) {
        if (!adminProperties.isEnabled()) {
            throw new BadRequestException("Admin API disabled");
        }
        if (providedToken == null || providedToken.isBlank()) {
            throw new BadRequestException("Missing X-Admin-Token header");
        }
        if (!adminProperties.getToken().equals(providedToken)) {
            throw new BadRequestException("Invalid admin token");
        }
    }
}

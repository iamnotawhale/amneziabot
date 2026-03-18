package com.iamnotawhale.amneziabot.api.dto;

public record AdminOverviewResponse(
        long totalUsers,
        long totalSubscriptions,
        long activeSubscriptions,
        long expiredSubscriptions,
        long revokedSubscriptions,
        long totalTrafficUsedBytes
) {
}

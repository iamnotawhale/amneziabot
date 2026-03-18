package com.iamnotawhale.amneziabot.api.dto;

import java.time.OffsetDateTime;

public record AdminUserRow(
        Long userId,
        Long telegramId,
        String username,
        OffsetDateTime createdAt,
        String activePlanCode,
        OffsetDateTime activeEndsAt,
        String activeStatus,
        String xrayClientUuid,
        String xrayClientEmail,
        long totalSubscriptions,
        long totalTrafficUsedBytes
) {
}

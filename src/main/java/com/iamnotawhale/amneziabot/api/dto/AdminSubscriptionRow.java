package com.iamnotawhale.amneziabot.api.dto;

import java.time.OffsetDateTime;

public record AdminSubscriptionRow(
        Long subscriptionId,
        Long userId,
        Long telegramId,
        String username,
        String planCode,
        String status,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Long trafficLimitBytes,
        long trafficUsedBytes,
        Integer deviceLimit,
        String xrayClientUuid,
        String xrayClientEmail,
        String vlessLink
) {
}

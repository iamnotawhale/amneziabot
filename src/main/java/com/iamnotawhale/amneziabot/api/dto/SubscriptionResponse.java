package com.iamnotawhale.amneziabot.api.dto;

import java.time.OffsetDateTime;

public record SubscriptionResponse(
        Long subscriptionId,
        String status,
        String planCode,
        String planName,
        Long trafficLimitBytes,
        long trafficUsedBytes,
        Integer deviceLimit,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String vlessLink
) {
}

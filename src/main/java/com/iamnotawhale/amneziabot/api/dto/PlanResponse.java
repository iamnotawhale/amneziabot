package com.iamnotawhale.amneziabot.api.dto;

public record PlanResponse(
        String code,
        String name,
        Integer durationDays,
        Long trafficLimitBytes,
        Integer deviceLimit,
        boolean trial
) {
}

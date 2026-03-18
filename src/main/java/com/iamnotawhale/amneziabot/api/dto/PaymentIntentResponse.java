package com.iamnotawhale.amneziabot.api.dto;

public record PaymentIntentResponse(
        Long paymentId,
        String status,
        String provider,
        String message
) {
}

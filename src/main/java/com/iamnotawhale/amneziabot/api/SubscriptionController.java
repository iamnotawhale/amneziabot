package com.iamnotawhale.amneziabot.api;

import com.iamnotawhale.amneziabot.api.dto.ActivatePlanRequest;
import com.iamnotawhale.amneziabot.api.dto.SubscriptionResponse;
import com.iamnotawhale.amneziabot.api.dto.TrialRequest;
import com.iamnotawhale.amneziabot.service.IdempotencyService;
import com.iamnotawhale.amneziabot.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final IdempotencyService idempotencyService;

    public SubscriptionController(SubscriptionService subscriptionService, IdempotencyService idempotencyService) {
        this.subscriptionService = subscriptionService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/trial")
    public SubscriptionResponse issueTrial(
            @Valid @RequestBody TrialRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return idempotencyService.execute(
                idempotencyKey,
                "SUB_TRIAL",
                request.getTelegramId(),
                () -> subscriptionService.issueTrial(request.getTelegramId(), request.getUsername())
        );
    }

    @PostMapping("/activate")
    public SubscriptionResponse activate(
            @Valid @RequestBody ActivatePlanRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return idempotencyService.execute(
                idempotencyKey,
                "SUB_ACTIVATE_" + request.getPlanCode(),
                request.getTelegramId(),
                () -> subscriptionService.activatePlan(request.getTelegramId(), request.getUsername(), request.getPlanCode())
        );
    }

    @GetMapping("/{telegramId}")
    public SubscriptionResponse activeByTelegramId(@PathVariable Long telegramId) {
        return subscriptionService.getActiveSubscription(telegramId);
    }
}

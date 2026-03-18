package com.iamnotawhale.amneziabot.api;

import com.iamnotawhale.amneziabot.api.dto.PaymentIntentRequest;
import com.iamnotawhale.amneziabot.api.dto.PaymentIntentResponse;
import com.iamnotawhale.amneziabot.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/intent")
    public PaymentIntentResponse createIntent(@Valid @RequestBody PaymentIntentRequest request) {
        return paymentService.createIntent(request.getTelegramId(), request.getUsername(), request.getPlanCode());
    }
}

package com.iamnotawhale.amneziabot.service;

import com.iamnotawhale.amneziabot.api.dto.PaymentIntentResponse;
import com.iamnotawhale.amneziabot.domain.AppUser;
import com.iamnotawhale.amneziabot.domain.Payment;
import com.iamnotawhale.amneziabot.domain.PaymentStatus;
import com.iamnotawhale.amneziabot.repository.AppUserRepository;
import com.iamnotawhale.amneziabot.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class PaymentService {

    private final AppUserRepository appUserRepository;
    private final PaymentRepository paymentRepository;

    public PaymentService(AppUserRepository appUserRepository, PaymentRepository paymentRepository) {
        this.appUserRepository = appUserRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentIntentResponse createIntent(Long telegramId, String username, String planCode) {
        AppUser user = appUserRepository.findByTelegramId(telegramId)
                .orElseGet(() -> {
                    AppUser created = new AppUser();
                    created.setTelegramId(telegramId);
                    created.setUsername(username);
                    created.setCreatedAt(OffsetDateTime.now());
                    return appUserRepository.save(created);
                });

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setPlanCode(planCode);
        payment.setProvider("MANUAL_PLACEHOLDER");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(OffsetDateTime.now());

        Payment saved = paymentRepository.save(payment);
        return new PaymentIntentResponse(
                saved.getId(),
                saved.getStatus().name(),
                saved.getProvider(),
                "Payment integration is planned. For now activate plan manually."
        );
    }
}

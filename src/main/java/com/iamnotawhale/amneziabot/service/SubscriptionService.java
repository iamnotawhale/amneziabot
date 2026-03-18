package com.iamnotawhale.amneziabot.service;

import com.iamnotawhale.amneziabot.api.dto.SubscriptionResponse;
import com.iamnotawhale.amneziabot.domain.AppUser;
import com.iamnotawhale.amneziabot.domain.Plan;
import com.iamnotawhale.amneziabot.domain.Subscription;
import com.iamnotawhale.amneziabot.domain.SubscriptionStatus;
import com.iamnotawhale.amneziabot.repository.AppUserRepository;
import com.iamnotawhale.amneziabot.repository.SubscriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final AppUserRepository appUserRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanService planService;
    private final XrayDockerService xrayDockerService;
    private final VlessLinkService vlessLinkService;

    public SubscriptionService(
            AppUserRepository appUserRepository,
            SubscriptionRepository subscriptionRepository,
            PlanService planService,
            XrayDockerService xrayDockerService,
            VlessLinkService vlessLinkService
    ) {
        this.appUserRepository = appUserRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planService = planService;
        this.xrayDockerService = xrayDockerService;
        this.vlessLinkService = vlessLinkService;
    }

    @Transactional
    public SubscriptionResponse issueTrial(Long telegramId, String username) {
        AppUser user = upsertUser(telegramId, username);
        if (findActive(user) != null) {
            throw new BadRequestException("У вас уже есть активный ключ");
        }
        if (subscriptionRepository.existsByUserAndPlan_TrialTrue(user)) {
            throw new BadRequestException("Trial already used");
        }
        Plan trialPlan = planService.getByCode("TRIAL_1D");
        Subscription subscription = createSubscription(user, trialPlan);
        return toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse activatePlan(Long telegramId, String username, String planCode) {
        AppUser user = upsertUser(telegramId, username);
        Plan plan = planService.getByCode(planCode);
        if (plan.isTrial()) {
            return issueTrial(telegramId, username);
        }
        Subscription active = findActive(user);
        if (active != null && active.getPlan().getCode().equals(plan.getCode())) {
            extendSamePlan(active, plan);
            return toResponse(active);
        }
        if (active != null) {
            deactivateCurrent(active);
        }
        Subscription subscription = createSubscription(user, plan);
        return toResponse(subscription);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getActiveSubscription(Long telegramId) {
        AppUser user = appUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Subscription subscription = subscriptionRepository.findFirstByUserAndStatusOrderByStartsAtDesc(user, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Active subscription not found"));
        return toResponse(subscription);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionById(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new NotFoundException("Subscription not found"));
        return toResponse(subscription);
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.expire-job-ms:300000}")
    public void expireSubscriptions() {
        List<Subscription> expired = subscriptionRepository.findAllByStatusAndEndsAtBefore(
                SubscriptionStatus.ACTIVE,
                OffsetDateTime.now()
        );
        for (Subscription subscription : expired) {
            xrayDockerService.removeClient(subscription.getXrayClientUuid());
            subscription.setStatus(SubscriptionStatus.EXPIRED);
        }
    }

    private AppUser upsertUser(Long telegramId, String username) {
        return appUserRepository.findByTelegramId(telegramId)
                .map(existing -> {
                    existing.setUsername(username);
                    return existing;
                })
                .orElseGet(() -> {
                    AppUser appUser = new AppUser();
                    appUser.setTelegramId(telegramId);
                    appUser.setUsername(username);
                    appUser.setCreatedAt(OffsetDateTime.now());
                    return appUserRepository.save(appUser);
                });
    }

    private void deactivateCurrentIfExists(AppUser user) {
        Subscription active = findActive(user);
        if (active != null) {
            deactivateCurrent(active);
        }
    }

    private Subscription findActive(AppUser user) {
        return subscriptionRepository.findFirstByUserAndStatusOrderByStartsAtDesc(user, SubscriptionStatus.ACTIVE)
                .orElse(null);
    }

    private void deactivateCurrent(Subscription existing) {
        xrayDockerService.removeClient(existing.getXrayClientUuid());
        existing.setStatus(SubscriptionStatus.REVOKED);
    }

    private void extendSamePlan(Subscription subscription, Plan plan) {
        Integer durationDays = plan.getDurationDays();
        if (durationDays == null) {
            return;
        }
        OffsetDateTime base = subscription.getEndsAt();
        OffsetDateTime now = OffsetDateTime.now();
        if (base == null || base.isBefore(now)) {
            base = now;
        }
        subscription.setEndsAt(base.plusDays(durationDays));
    }

    private Subscription createSubscription(AppUser user, Plan plan) {
        String uuid = UUID.randomUUID().toString();
        String email = "tg_" + user.getTelegramId() + "_" + System.currentTimeMillis();
        xrayDockerService.addClient(uuid, email);

        OffsetDateTime now = OffsetDateTime.now();
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartsAt(now);
        subscription.setEndsAt(plan.getDurationDays() == null ? null : now.plusDays(plan.getDurationDays()));
        subscription.setTrafficUsedBytes(0L);
        subscription.setXrayClientUuid(uuid);
        subscription.setXrayClientEmail(email);
        subscription.setVlessLink(vlessLinkService.buildLink(uuid, "amneziabot_" + user.getTelegramId()));

        return subscriptionRepository.save(subscription);
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getStatus().name(),
                subscription.getPlan().getCode(),
                subscription.getPlan().getName(),
                subscription.getPlan().getTrafficLimitBytes(),
                subscription.getTrafficUsedBytes(),
                subscription.getPlan().getDeviceLimit(),
                subscription.getStartsAt(),
                subscription.getEndsAt(),
                subscription.getVlessLink()
        );
    }
}

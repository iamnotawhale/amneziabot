package com.iamnotawhale.amneziabot.service;

import com.iamnotawhale.amneziabot.api.dto.AdminOverviewResponse;
import com.iamnotawhale.amneziabot.api.dto.AdminSubscriptionRow;
import com.iamnotawhale.amneziabot.api.dto.AdminUserRow;
import com.iamnotawhale.amneziabot.domain.AppUser;
import com.iamnotawhale.amneziabot.domain.Subscription;
import com.iamnotawhale.amneziabot.domain.SubscriptionStatus;
import com.iamnotawhale.amneziabot.repository.AppUserRepository;
import com.iamnotawhale.amneziabot.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final AppUserRepository appUserRepository;
    private final SubscriptionRepository subscriptionRepository;

    public AdminService(AppUserRepository appUserRepository, SubscriptionRepository subscriptionRepository) {
        this.appUserRepository = appUserRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public AdminOverviewResponse overview() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        long totalUsers = appUserRepository.count();
        long totalSubscriptions = subscriptions.size();
        long activeSubscriptions = subscriptions.stream().filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE).count();
        long expiredSubscriptions = subscriptions.stream().filter(s -> s.getStatus() == SubscriptionStatus.EXPIRED).count();
        long revokedSubscriptions = subscriptions.stream().filter(s -> s.getStatus() == SubscriptionStatus.REVOKED).count();
        long totalTrafficUsedBytes = subscriptions.stream().mapToLong(Subscription::getTrafficUsedBytes).sum();

        return new AdminOverviewResponse(
                totalUsers,
                totalSubscriptions,
                activeSubscriptions,
                expiredSubscriptions,
                revokedSubscriptions,
                totalTrafficUsedBytes
        );
    }

    @Transactional(readOnly = true)
    public List<AdminSubscriptionRow> subscriptions() {
        return subscriptionRepository.findAllWithUserAndPlan().stream()
                .sorted(Comparator.comparing(Subscription::getStartsAt).reversed())
                .map(this::toSubscriptionRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminUserRow> users() {
        List<AppUser> users = appUserRepository.findAll();
        List<Subscription> subscriptions = subscriptionRepository.findAllWithUserAndPlan();
        Map<Long, List<Subscription>> byUserId = subscriptions.stream()
                .collect(Collectors.groupingBy(s -> s.getUser().getId()));

        return users.stream()
                .sorted(Comparator.comparing(AppUser::getCreatedAt).reversed())
                .map(user -> toUserRow(user, byUserId.getOrDefault(user.getId(), List.of())))
                .toList();
    }

    private AdminSubscriptionRow toSubscriptionRow(Subscription s) {
        return new AdminSubscriptionRow(
                s.getId(),
                s.getUser().getId(),
                s.getUser().getTelegramId(),
                s.getUser().getUsername(),
                s.getPlan().getCode(),
                s.getStatus().name(),
                s.getStartsAt(),
                s.getEndsAt(),
                s.getPlan().getTrafficLimitBytes(),
                s.getTrafficUsedBytes(),
                s.getPlan().getDeviceLimit(),
                s.getXrayClientUuid(),
                s.getXrayClientEmail(),
                s.getVlessLink()
        );
    }

    private AdminUserRow toUserRow(AppUser user, List<Subscription> subscriptions) {
        Subscription active = subscriptions.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .max(Comparator.comparing(Subscription::getStartsAt))
                .orElse(null);
        long totalTraffic = subscriptions.stream().mapToLong(Subscription::getTrafficUsedBytes).sum();

        return new AdminUserRow(
                user.getId(),
                user.getTelegramId(),
                user.getUsername(),
                user.getCreatedAt(),
                active != null ? active.getPlan().getCode() : null,
                active != null ? active.getEndsAt() : null,
                active != null ? active.getStatus().name() : null,
                active != null ? active.getXrayClientUuid() : null,
                active != null ? active.getXrayClientEmail() : null,
                subscriptions.size(),
                totalTraffic
        );
    }
}

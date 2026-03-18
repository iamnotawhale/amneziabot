package com.iamnotawhale.amneziabot.repository;

import com.iamnotawhale.amneziabot.domain.AppUser;
import com.iamnotawhale.amneziabot.domain.Subscription;
import com.iamnotawhale.amneziabot.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findFirstByUserAndStatusOrderByStartsAtDesc(AppUser user, SubscriptionStatus status);

    List<Subscription> findAllByStatusAndEndsAtBefore(SubscriptionStatus status, OffsetDateTime threshold);

    boolean existsByUserAndPlan_TrialTrue(AppUser user);

    @Query("select s from Subscription s join fetch s.user join fetch s.plan")
    List<Subscription> findAllWithUserAndPlan();
}

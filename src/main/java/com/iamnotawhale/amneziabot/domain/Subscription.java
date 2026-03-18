package com.iamnotawhale.amneziabot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private OffsetDateTime startsAt;

    @Column
    private OffsetDateTime endsAt;

    @Column(nullable = false)
    private long trafficUsedBytes;

    @Column(nullable = false, unique = true)
    private String xrayClientUuid;

    @Column(nullable = false, unique = true)
    private String xrayClientEmail;

    @Column(nullable = false, length = 2048)
    private String vlessLink;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(OffsetDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(OffsetDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public long getTrafficUsedBytes() {
        return trafficUsedBytes;
    }

    public void setTrafficUsedBytes(long trafficUsedBytes) {
        this.trafficUsedBytes = trafficUsedBytes;
    }

    public String getXrayClientUuid() {
        return xrayClientUuid;
    }

    public void setXrayClientUuid(String xrayClientUuid) {
        this.xrayClientUuid = xrayClientUuid;
    }

    public String getXrayClientEmail() {
        return xrayClientEmail;
    }

    public void setXrayClientEmail(String xrayClientEmail) {
        this.xrayClientEmail = xrayClientEmail;
    }

    public String getVlessLink() {
        return vlessLink;
    }

    public void setVlessLink(String vlessLink) {
        this.vlessLink = vlessLink;
    }
}

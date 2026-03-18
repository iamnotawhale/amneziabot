package com.iamnotawhale.amneziabot.service;

import com.iamnotawhale.amneziabot.api.dto.SubscriptionResponse;
import com.iamnotawhale.amneziabot.domain.IdempotencyRecord;
import com.iamnotawhale.amneziabot.domain.IdempotencyStatus;
import com.iamnotawhale.amneziabot.repository.IdempotencyRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final long ttlDays;

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final SubscriptionService subscriptionService;

    public IdempotencyService(
            IdempotencyRecordRepository idempotencyRecordRepository,
            SubscriptionService subscriptionService,
            @org.springframework.beans.factory.annotation.Value("${app.idempotency-ttl-days:7}") long ttlDays
    ) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.subscriptionService = subscriptionService;
        this.ttlDays = ttlDays;
    }

    @Transactional
    public SubscriptionResponse execute(
            String idempotencyKey,
            String operation,
            Long telegramId,
            Supplier<SubscriptionResponse> supplier
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return supplier.get();
        }

        IdempotencyRecord existing = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return resolveExisting(existing, operation, telegramId);
        }

        IdempotencyRecord processing = new IdempotencyRecord();
        processing.setIdempotencyKey(idempotencyKey);
        processing.setOperation(operation);
        processing.setTelegramId(telegramId);
        processing.setStatus(IdempotencyStatus.PROCESSING);
        processing.setCreatedAt(OffsetDateTime.now());
        processing.setUpdatedAt(OffsetDateTime.now());
        try {
            idempotencyRecordRepository.save(processing);
        } catch (DataIntegrityViolationException ignored) {
            IdempotencyRecord raceRecord = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Failed to resolve idempotent request"));
            return resolveExisting(raceRecord, operation, telegramId);
        }

        try {
            SubscriptionResponse response = supplier.get();
            processing.setSubscriptionId(response.subscriptionId());
            processing.setStatus(IdempotencyStatus.COMPLETED);
            processing.setUpdatedAt(OffsetDateTime.now());
            return response;
        } catch (RuntimeException exception) {
            processing.setStatus(IdempotencyStatus.FAILED);
            processing.setUpdatedAt(OffsetDateTime.now());
            throw exception;
        }
    }

    private SubscriptionResponse resolveExisting(IdempotencyRecord record, String operation, Long telegramId) {
        if (!record.getOperation().equals(operation) || !record.getTelegramId().equals(telegramId)) {
            throw new BadRequestException("Idempotency-Key already used with another request");
        }
        if (record.getStatus() == IdempotencyStatus.COMPLETED && record.getSubscriptionId() != null) {
            return subscriptionService.getSubscriptionById(record.getSubscriptionId());
        }
        if (record.getStatus() == IdempotencyStatus.PROCESSING) {
            throw new BadRequestException("Request with this Idempotency-Key is processing");
        }
        throw new BadRequestException("Previous request with this Idempotency-Key failed, use a new key");
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.idempotency-cleanup-ms:3600000}")
    public void cleanupOldRecords() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(Math.max(ttlDays, 1));
        long deleted = idempotencyRecordRepository.deleteByUpdatedAtBefore(threshold);
        if (deleted > 0) {
            log.info("Idempotency cleanup removed {} records (ttlDays={}, threshold={})", deleted, ttlDays, threshold);
        } else {
            log.debug("Idempotency cleanup removed 0 records (ttlDays={}, threshold={})", ttlDays, threshold);
        }
    }
}

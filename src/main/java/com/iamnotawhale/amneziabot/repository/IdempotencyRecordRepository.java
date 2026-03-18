package com.iamnotawhale.amneziabot.repository;

import com.iamnotawhale.amneziabot.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    long deleteByUpdatedAtBefore(OffsetDateTime threshold);
}

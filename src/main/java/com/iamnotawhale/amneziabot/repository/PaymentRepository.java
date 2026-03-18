package com.iamnotawhale.amneziabot.repository;

import com.iamnotawhale.amneziabot.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}

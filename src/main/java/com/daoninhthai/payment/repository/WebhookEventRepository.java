package com.daoninhthai.payment.repository;

import com.daoninhthai.payment.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    List<WebhookEvent> findByStatusAndAttemptsLessThan(
            WebhookEvent.WebhookEventStatus status, int maxAttempts);
}

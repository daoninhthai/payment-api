package com.daoninhthai.payment.repository;

import com.daoninhthai.payment.entity.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, Long> {

    List<Webhook> findByUserIdAndActiveTrue(Long userId);

    List<Webhook> findByUserId(Long userId);
}

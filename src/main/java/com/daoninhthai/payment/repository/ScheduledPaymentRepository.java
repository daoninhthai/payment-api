package com.daoninhthai.payment.repository;

import com.daoninhthai.payment.entity.ScheduledPayment;
import com.daoninhthai.payment.entity.enums.ScheduledPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Long> {

    List<ScheduledPayment> findByStatus(ScheduledPaymentStatus status);

    List<ScheduledPayment> findByWalletId(Long walletId);

    @Query("SELECT sp FROM ScheduledPayment sp WHERE sp.status = :status AND sp.nextExecutionDate <= :dateTime")
    List<ScheduledPayment> findByStatusAndNextExecutionDateBefore(
            @Param("status") ScheduledPaymentStatus status,
            @Param("dateTime") LocalDateTime dateTime);

    List<ScheduledPayment> findByWalletIdAndStatus(Long walletId, ScheduledPaymentStatus status);
}

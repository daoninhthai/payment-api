package com.daoninhthai.payment.controller;

import com.daoninhthai.payment.dto.request.CreateScheduledPaymentRequest;
import com.daoninhthai.payment.dto.response.ScheduledPaymentResponse;
import com.daoninhthai.payment.entity.ScheduledPayment;
import com.daoninhthai.payment.service.ScheduledPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/scheduled-payments")
@RequiredArgsConstructor
public class ScheduledPaymentController {

    private final ScheduledPaymentService scheduledPaymentService;

    @PostMapping
    public ResponseEntity<ScheduledPaymentResponse> create(
            @Valid @RequestBody CreateScheduledPaymentRequest request) {
        ScheduledPayment payment = scheduledPaymentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ScheduledPaymentResponse.fromEntity(payment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledPaymentResponse> getById(@PathVariable Long id) {
        ScheduledPayment payment = scheduledPaymentService.getScheduledPayment(id);
        return ResponseEntity.ok(ScheduledPaymentResponse.fromEntity(payment));
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<List<ScheduledPaymentResponse>> getByWalletId(@PathVariable Long walletId) {
        List<ScheduledPaymentResponse> payments = scheduledPaymentService.getByWalletId(walletId)
                .stream()
                .map(ScheduledPaymentResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/active")
    public ResponseEntity<List<ScheduledPaymentResponse>> getActivePayments() {
        List<ScheduledPaymentResponse> payments = scheduledPaymentService.getActivePayments()
                .stream()
                .map(ScheduledPaymentResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ScheduledPaymentResponse> pause(@PathVariable Long id) {
        ScheduledPayment payment = scheduledPaymentService.pause(id);
        return ResponseEntity.ok(ScheduledPaymentResponse.fromEntity(payment));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ScheduledPaymentResponse> resume(@PathVariable Long id) {
        ScheduledPayment payment = scheduledPaymentService.resume(id);
        return ResponseEntity.ok(ScheduledPaymentResponse.fromEntity(payment));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ScheduledPaymentResponse> cancel(@PathVariable Long id) {
        ScheduledPayment payment = scheduledPaymentService.cancel(id);
        return ResponseEntity.ok(ScheduledPaymentResponse.fromEntity(payment));
    }
}

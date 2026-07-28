package com.shopmanagement.queueservice.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanagement.queueservice.model.QueueToken;
import com.shopmanagement.queueservice.repository.QueueTokenRepository;
import com.shopmanagement.queueservice.support.TenantContext;

@Service
public class QueueService {

    private final QueueTokenRepository queueTokenRepository;

    public QueueService(QueueTokenRepository queueTokenRepository) {
        this.queueTokenRepository = queueTokenRepository;
    }

    @Transactional(readOnly = true)
    public List<QueueToken> todayQueue(Long doctorId, LocalDate date) {
        if (doctorId == null) {
            throw new IllegalArgumentException("doctorId is required");
        }
        LocalDate tokenDate = date != null ? date : LocalDate.now();
        return queueTokenRepository.findByTenantIdAndShopIdAndDoctorIdAndTokenDateOrderByTokenNumberAsc(
                TenantContext.requireTenantId(), TenantContext.requireShopId(), doctorId, tokenDate);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> displayBoard(Long branchId, LocalDate date) {
        LocalDate tokenDate = date != null ? date : LocalDate.now();
        List<QueueToken> waiting = queueTokenRepository
                .findByTenantIdAndShopIdAndBranchIdAndTokenDateAndStatusOrderByTokenNumberAsc(
                        TenantContext.requireTenantId(), TenantContext.requireShopId(), branchId, tokenDate, "WAITING");
        List<QueueToken> called = queueTokenRepository
                .findByTenantIdAndShopIdAndBranchIdAndTokenDateAndStatusOrderByTokenNumberAsc(
                        TenantContext.requireTenantId(), TenantContext.requireShopId(), branchId, tokenDate, "CALLED");
        List<QueueToken> inConsult = queueTokenRepository
                .findByTenantIdAndShopIdAndBranchIdAndTokenDateAndStatusOrderByTokenNumberAsc(
                        TenantContext.requireTenantId(), TenantContext.requireShopId(), branchId, tokenDate, "IN_CONSULTATION");
        Map<String, Object> board = new LinkedHashMap<>();
        board.put("date", tokenDate.toString());
        board.put(
                "nowServing",
                inConsult.isEmpty() ? (called.isEmpty() ? null : called.get(0)) : inConsult.get(0));
        board.put("waiting", waiting);
        board.put("called", called);
        return board;
    }

    @Transactional
    public QueueToken generateToken(QueueToken payload) {
        TenantContext.requireAnyPermission("MANAGE_QUEUE", "MANAGE_APPOINTMENTS", "MANAGE_CONSULTATIONS");
        validate(payload);
        Long tenantId = TenantContext.requireTenantId();
        String shopId = TenantContext.requireShopId();
        LocalDate tokenDate = payload.getTokenDate() != null ? payload.getTokenDate() : LocalDate.now();
        int nextNumber = queueTokenRepository.findMaxTokenNumber(tenantId, shopId, payload.getDoctorId(), tokenDate) + 1;
        payload.setId(null);
        payload.setTenantId(tenantId);
        payload.setShopId(shopId);
        payload.setTokenDate(tokenDate);
        payload.setTokenNumber(nextNumber);
        payload.setStatus("WAITING");
        payload.setCheckedInAt(LocalDateTime.now());
        if (payload.getQueueType() == null || payload.getQueueType().isBlank()) {
            payload.setQueueType("NORMAL");
        }
        if (payload.getPriority() == null) {
            payload.setPriority(0);
        }
        payload.setEstimatedWaitMinutes(estimateWaitMinutes(payload.getDoctorId(), tokenDate));
        return queueTokenRepository.save(payload);
    }

    @Transactional
    public QueueToken callNext(Long doctorId) {
        TenantContext.requireAnyPermission("MANAGE_QUEUE", "MANAGE_APPOINTMENTS", "MANAGE_CONSULTATIONS");
        LocalDate today = LocalDate.now();
        List<QueueToken> waiting = queueTokenRepository
                .findByTenantIdAndShopIdAndDoctorIdAndTokenDateOrderByTokenNumberAsc(
                        TenantContext.requireTenantId(), TenantContext.requireShopId(), doctorId, today)
                .stream()
                .filter(token -> "WAITING".equals(token.getStatus()))
                .sorted(Comparator.comparing(QueueToken::getPriority).reversed()
                        .thenComparing(QueueToken::getTokenNumber))
                .toList();
        if (waiting.isEmpty()) {
            throw new IllegalArgumentException("No waiting patients in queue");
        }
        QueueToken next = waiting.get(0);
        next.setStatus("CALLED");
        next.setCalledAt(LocalDateTime.now());
        return queueTokenRepository.save(next);
    }

    /**
     * Start (or resume) consultation for a specific token — including out-of-order WAITING patients.
     * Any other same-doctor IN_CONSULTATION token for today is parked back to WAITING.
     */
    @Transactional
    public QueueToken startConsultation(Long tokenId) {
        TenantContext.requireAnyPermission(
                "MANAGE_CONSULTATIONS", "WRITE_PRESCRIPTION", "VIEW_DOCTOR_DASHBOARD");
        QueueToken token = require(tokenId);
        String status = token.getStatus() == null ? "" : token.getStatus().trim().toUpperCase();
        if ("COMPLETED".equals(status) || "CANCELLED".equals(status) || "NO_SHOW".equals(status)) {
            throw new IllegalArgumentException("Cannot start consultation for token status: " + status);
        }
        if ("IN_CONSULTATION".equals(status)) {
            return token;
        }
        if (!"WAITING".equals(status) && !"CALLED".equals(status)) {
            throw new IllegalArgumentException("Cannot start consultation for token status: " + status);
        }
        parkOtherInConsultation(token);
        if (token.getCalledAt() == null) {
            token.setCalledAt(LocalDateTime.now());
        }
        token.setStatus("IN_CONSULTATION");
        token.setConsultStartedAt(LocalDateTime.now());
        return queueTokenRepository.save(token);
    }

    /** Put other open consults for the same doctor/day back to WAITING so out-of-order start is safe. */
    private void parkOtherInConsultation(QueueToken starting) {
        LocalDate tokenDate = starting.getTokenDate() != null ? starting.getTokenDate() : LocalDate.now();
        queueTokenRepository
                .findByTenantIdAndShopIdAndDoctorIdAndTokenDateOrderByTokenNumberAsc(
                        TenantContext.requireTenantId(),
                        TenantContext.requireShopId(),
                        starting.getDoctorId(),
                        tokenDate)
                .stream()
                .filter(other -> other.getId() != null && !other.getId().equals(starting.getId()))
                .filter(other -> "IN_CONSULTATION".equalsIgnoreCase(other.getStatus()))
                .forEach(other -> {
                    other.setStatus("WAITING");
                    other.setConsultStartedAt(null);
                    other.setCalledAt(null);
                    queueTokenRepository.save(other);
                });
    }

    @Transactional
    public QueueToken complete(Long tokenId) {
        TenantContext.requireAnyPermission(
                "MANAGE_CONSULTATIONS", "WRITE_PRESCRIPTION", "VIEW_DOCTOR_DASHBOARD", "MANAGE_QUEUE");
        QueueToken token = require(tokenId);
        token.setStatus("COMPLETED");
        token.setCompletedAt(LocalDateTime.now());
        return queueTokenRepository.save(token);
    }

    @Transactional
    public QueueToken skip(Long tokenId) {
        QueueToken token = require(tokenId);
        token.setStatus("WAITING");
        token.setCalledAt(null);
        token.setConsultStartedAt(null);
        return queueTokenRepository.save(token);
    }

    private QueueToken require(Long id) {
        return queueTokenRepository.findByIdAndTenantIdAndShopId(
                        id, TenantContext.requireTenantId(), TenantContext.requireShopId())
                .orElseThrow(() -> new IllegalArgumentException("Queue token not found: " + id));
    }

    private int estimateWaitMinutes(Long doctorId, LocalDate tokenDate) {
        long waitingCount = queueTokenRepository
                .findByTenantIdAndShopIdAndDoctorIdAndTokenDateOrderByTokenNumberAsc(
                        TenantContext.requireTenantId(), TenantContext.requireShopId(), doctorId, tokenDate)
                .stream()
                .filter(token -> "WAITING".equals(token.getStatus()))
                .count();
        return (int) Math.min(waitingCount * 8, 120);
    }

    private static void validate(QueueToken payload) {
        if (payload.getBranchId() == null) {
            throw new IllegalArgumentException("branchId is required");
        }
        if (payload.getAppointmentId() == null) {
            throw new IllegalArgumentException("appointmentId is required");
        }
        if (payload.getDoctorId() == null) {
            throw new IllegalArgumentException("doctorId is required");
        }
        if (payload.getPatientId() == null) {
            throw new IllegalArgumentException("patientId is required");
        }
    }
}

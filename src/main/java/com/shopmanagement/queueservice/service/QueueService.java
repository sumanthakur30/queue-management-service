package com.shopmanagement.queueservice.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanagement.queueservice.model.QueueToken;
import com.shopmanagement.queueservice.repository.QueueTokenRepository;
import com.shopmanagement.queueservice.support.QueueDoctorAccess;
import com.shopmanagement.queueservice.support.QueueWaitingOrder;
import com.shopmanagement.queueservice.support.SlotAlreadyBookedException;
import com.shopmanagement.queueservice.support.TenantContext;

@Service
public class QueueService {

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_CALLED = "CALLED";
    public static final String STATUS_IN_CONSULTATION = "IN_CONSULTATION";
    public static final String STATUS_WAITING_FOR_LAB_RESULTS = "WAITING_FOR_LAB_RESULTS";
    public static final String STATUS_LAB_RESULTS_AVAILABLE = "LAB_RESULTS_AVAILABLE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_NO_SHOW = "NO_SHOW";

    private static final Set<String> STARTABLE = Set.of(
            STATUS_WAITING,
            STATUS_CALLED,
            STATUS_WAITING_FOR_LAB_RESULTS,
            STATUS_LAB_RESULTS_AVAILABLE);
    private static final List<String> OPEN_ENCOUNTER_STATUSES = List.of(
            STATUS_WAITING,
            STATUS_CALLED,
            STATUS_IN_CONSULTATION,
            STATUS_WAITING_FOR_LAB_RESULTS,
            STATUS_LAB_RESULTS_AVAILABLE);
    private static final List<String> ATTENTION_STATUSES = List.of(
            STATUS_WAITING_FOR_LAB_RESULTS,
            STATUS_LAB_RESULTS_AVAILABLE,
            STATUS_IN_CONSULTATION);
    private static final List<String> LAB_HOLD_STATUSES = List.of(
            STATUS_WAITING_FOR_LAB_RESULTS,
            STATUS_LAB_RESULTS_AVAILABLE);

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
        List<QueueToken> tokens = queueTokenRepository.findByTenantIdAndShopIdAndDoctorIdAndTokenDateOrderByTokenNumberAsc(
                TenantContext.requireTenantId(), TenantContext.requireShopId(), doctorId, tokenDate);
        return QueueWaitingOrder.orderTodayQueue(tokens);
    }

    /**
     * Open encounters that still need doctor attention (lab wait / results ready / in consult),
     * including prior calendar days so next-day report review resumes the same token.
     */
    @Transactional(readOnly = true)
    public List<QueueToken> attentionQueue(Long doctorId, Integer lookbackDays) {
        if (doctorId == null) {
            throw new IllegalArgumentException("doctorId is required");
        }
        int days = lookbackDays == null || lookbackDays < 1 ? 30 : Math.min(lookbackDays, 90);
        LocalDate from = LocalDate.now().minusDays(days);
        return queueTokenRepository
                .findByTenantIdAndShopIdAndDoctorIdAndStatusInAndTokenDateGreaterThanEqualOrderByTokenDateDescTokenNumberAsc(
                        TenantContext.requireTenantId(),
                        TenantContext.requireShopId(),
                        doctorId,
                        ATTENTION_STATUSES,
                        from);
    }

    @Transactional(readOnly = true)
    public List<QueueToken> openForPatient(Long patientId, Long doctorId) {
        if (patientId == null) {
            throw new IllegalArgumentException("patientId is required");
        }
        Long tenantId = TenantContext.requireTenantId();
        String shopId = TenantContext.requireShopId();
        if (doctorId != null) {
            return queueTokenRepository
                    .findByTenantIdAndShopIdAndPatientIdAndDoctorIdAndStatusInOrderByTokenDateDescTokenNumberDesc(
                            tenantId, shopId, patientId, doctorId, OPEN_ENCOUNTER_STATUSES);
        }
        return queueTokenRepository.findByTenantIdAndShopIdAndPatientIdAndStatusInOrderByTokenDateDescTokenNumberDesc(
                tenantId, shopId, patientId, OPEN_ENCOUNTER_STATUSES);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> displayBoard(Long branchId, LocalDate date) {
        LocalDate tokenDate = date != null ? date : LocalDate.now();
        List<QueueToken> waiting = queueTokenRepository
                .findByTenantIdAndShopIdAndBranchIdAndTokenDateAndStatusOrderByTokenNumberAsc(
                        TenantContext.requireTenantId(), TenantContext.requireShopId(), branchId, tokenDate, STATUS_WAITING);
        List<QueueToken> called = queueTokenRepository
                .findByTenantIdAndShopIdAndBranchIdAndTokenDateAndStatusOrderByTokenNumberAsc(
                        TenantContext.requireTenantId(), TenantContext.requireShopId(), branchId, tokenDate, STATUS_CALLED);
        List<QueueToken> inConsult = queueTokenRepository
                .findByTenantIdAndShopIdAndBranchIdAndTokenDateAndStatusOrderByTokenNumberAsc(
                        TenantContext.requireTenantId(), TenantContext.requireShopId(), branchId, tokenDate, STATUS_IN_CONSULTATION);
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
        payload.setStatus(STATUS_WAITING);
        payload.setCheckedInAt(LocalDateTime.now());
        if (payload.getQueueType() == null || payload.getQueueType().isBlank()) {
            payload.setQueueType("NORMAL");
        }
        if (payload.getPriority() == null) {
            payload.setPriority(0);
        }
        payload.setEstimatedWaitMinutes(estimateWaitMinutes(payload.getDoctorId(), tokenDate));
        applyBookingType(payload, tokenDate);
        if (payload.getSlotStart() != null) {
            reserveSlotOrFail(tenantId, shopId, payload.getDoctorId(), tokenDate, payload.getSlotStart());
        }
        try {
            return queueTokenRepository.save(payload);
        } catch (DataIntegrityViolationException ex) {
            throw new SlotAlreadyBookedException("This time slot is already booked");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> bookedSlots(Long doctorId, LocalDate date) {
        if (doctorId == null) {
            throw new IllegalArgumentException("doctorId is required");
        }
        LocalDate tokenDate = date != null ? date : LocalDate.now();
        List<Map<String, Object>> booked = queueTokenRepository
                .findByTenantIdAndShopIdAndDoctorIdAndTokenDateAndSlotStartIsNotNull(
                        TenantContext.requireTenantId(), TenantContext.requireShopId(), doctorId, tokenDate)
                .stream()
                .filter(token -> !isReleasedSlotStatus(token.getStatus()))
                .sorted(Comparator.comparing(QueueToken::getSlotStart))
                .map(token -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("slotStart", token.getSlotStart() != null ? token.getSlotStart().toString() : null);
                    row.put("tokenNumber", token.getTokenNumber());
                    row.put("status", token.getStatus());
                    row.put("bookingType", token.getBookingType());
                    row.put("tokenId", token.getId());
                    return row;
                })
                .toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("date", tokenDate.toString());
        body.put("doctorId", doctorId);
        body.put("bookedSlots", booked);
        return body;
    }

    @Transactional
    public QueueToken callNext(Long doctorId) {
        TenantContext.requireAnyPermission("MANAGE_QUEUE", "MANAGE_APPOINTMENTS", "MANAGE_CONSULTATIONS");
        LocalDate today = LocalDate.now();
        List<QueueToken> waiting = queueTokenRepository
                .findByTenantIdAndShopIdAndDoctorIdAndTokenDateOrderByTokenNumberAsc(
                        TenantContext.requireTenantId(), TenantContext.requireShopId(), doctorId, today)
                .stream()
                .filter(token -> STATUS_WAITING.equals(token.getStatus()))
                .sorted(QueueWaitingOrder.waitingComparator())
                .toList();
        if (waiting.isEmpty()) {
            throw new IllegalArgumentException("No waiting patients in queue");
        }
        QueueToken next = waiting.get(0);
        next.setStatus(STATUS_CALLED);
        next.setCalledAt(LocalDateTime.now());
        return queueTokenRepository.save(next);
    }

    @Transactional(readOnly = true)
    public QueueToken getToken(Long tokenId, Long doctorId) {
        QueueToken token = require(tokenId);
        QueueDoctorAccess.requireDoctorMatch(token, doctorId);
        return token;
    }

    @Transactional
    public QueueToken startConsultation(Long tokenId) {
        return startConsultation(tokenId, null);
    }

    /**
     * Start (or resume) consultation for a specific token — including lab-wait / results-ready resumes.
     * Any other same-doctor IN_CONSULTATION token for today is parked back to WAITING
     * (lab-hold tokens are never auto-parked).
     */
    @Transactional
    public QueueToken startConsultation(Long tokenId, Long doctorId) {
        TenantContext.requireAnyPermission(
                "MANAGE_CONSULTATIONS", "WRITE_PRESCRIPTION", "VIEW_DOCTOR_DASHBOARD");
        QueueToken token = require(tokenId);
        QueueDoctorAccess.requireDoctorMatch(token, doctorId);
        String status = normalizeStatus(token.getStatus());
        if (STATUS_COMPLETED.equals(status) || STATUS_CANCELLED.equals(status) || STATUS_NO_SHOW.equals(status)) {
            throw new IllegalArgumentException("Cannot start consultation for token status: " + status);
        }
        if (STATUS_IN_CONSULTATION.equals(status)) {
            return token;
        }
        if (!STARTABLE.contains(status)) {
            throw new IllegalArgumentException("Cannot start consultation for token status: " + status);
        }
        parkOtherInConsultation(token);
        if (token.getCalledAt() == null) {
            token.setCalledAt(LocalDateTime.now());
        }
        token.setStatus(STATUS_IN_CONSULTATION);
        token.setConsultStartedAt(LocalDateTime.now());
        token.setCompletedAt(null);
        return queueTokenRepository.save(token);
    }

    /**
     * Pause an active consult while investigations are pending — same encounter stays open.
     */
    @Transactional
    public QueueToken awaitLabResults(Long tokenId, Long consultationId) {
        TenantContext.requireAnyPermission(
                "MANAGE_CONSULTATIONS", "WRITE_PRESCRIPTION", "VIEW_DOCTOR_DASHBOARD");
        QueueToken token = require(tokenId);
        String status = normalizeStatus(token.getStatus());
        if (!STATUS_IN_CONSULTATION.equals(status)
                && !STATUS_WAITING_FOR_LAB_RESULTS.equals(status)
                && !STATUS_LAB_RESULTS_AVAILABLE.equals(status)) {
            throw new IllegalArgumentException(
                    "Await lab results only from IN_CONSULTATION / lab-hold statuses, got: " + status);
        }
        if (consultationId != null && consultationId > 0) {
            token.setConsultationId(consultationId);
        }
        token.setStatus(STATUS_WAITING_FOR_LAB_RESULTS);
        token.setCompletedAt(null);
        return queueTokenRepository.save(token);
    }

    /**
     * Lab verified/released results for a polyclinic order — flip matching open encounter to RESULTS AVAILABLE.
     * Idempotent: already LAB_RESULTS_AVAILABLE / IN_CONSULTATION is left alone (or promoted from WAITING_FOR_LAB).
     */
    @Transactional
    public QueueToken markLabResultsAvailable(Long patientId, Long doctorId, Long consultationId) {
        TenantContext.requireAnyPermission(
                "MANAGE_CONSULTATIONS", "WRITE_PRESCRIPTION", "VIEW_DOCTOR_DASHBOARD", "MANAGE_LAB_RESULTS", "RELEASE_LAB_REPORTS");
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("patientId is required");
        }
        Long tenantId = TenantContext.requireTenantId();
        String shopId = TenantContext.requireShopId();

        QueueToken token = null;
        if (consultationId != null && consultationId > 0) {
            token = queueTokenRepository
                    .findFirstByTenantIdAndShopIdAndConsultationIdAndStatusInOrderByIdDesc(
                            tenantId, shopId, consultationId, LAB_HOLD_STATUSES)
                    .orElse(null);
        }
        if (token == null && doctorId != null && doctorId > 0) {
            token = queueTokenRepository
                    .findByTenantIdAndShopIdAndPatientIdAndDoctorIdAndStatusInOrderByTokenDateDescTokenNumberDesc(
                            tenantId, shopId, patientId, doctorId, LAB_HOLD_STATUSES)
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
        if (token == null) {
            token = queueTokenRepository
                    .findByTenantIdAndShopIdAndPatientIdAndStatusInOrderByTokenDateDescTokenNumberDesc(
                            tenantId, shopId, patientId, LAB_HOLD_STATUSES)
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
        if (token == null) {
            // No lab-hold encounter — nothing to update (walk-in lab / already completed).
            return null;
        }
        if (STATUS_LAB_RESULTS_AVAILABLE.equalsIgnoreCase(token.getStatus())) {
            return token;
        }
        if (consultationId != null && consultationId > 0 && token.getConsultationId() == null) {
            token.setConsultationId(consultationId);
        }
        token.setStatus(STATUS_LAB_RESULTS_AVAILABLE);
        token.setCompletedAt(null);
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
                .filter(other -> STATUS_IN_CONSULTATION.equalsIgnoreCase(other.getStatus()))
                .forEach(other -> {
                    other.setStatus(STATUS_WAITING);
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
        token.setStatus(STATUS_COMPLETED);
        token.setCompletedAt(LocalDateTime.now());
        return queueTokenRepository.save(token);
    }

    @Transactional
    public QueueToken skip(Long tokenId) {
        QueueToken token = require(tokenId);
        String status = normalizeStatus(token.getStatus());
        if (STATUS_WAITING_FOR_LAB_RESULTS.equals(status) || STATUS_LAB_RESULTS_AVAILABLE.equals(status)) {
            throw new IllegalArgumentException("Cannot skip a lab-hold encounter; resume or complete it instead");
        }
        token.setStatus(STATUS_WAITING);
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
                .filter(token -> STATUS_WAITING.equals(token.getStatus()))
                .count();
        return (int) Math.min(waitingCount * 8, 120);
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private void applyBookingType(QueueToken payload, LocalDate tokenDate) {
        if (payload.getSlotStart() != null) {
            payload.setBookingType(QueueWaitingOrder.BOOKING_SLOT);
            if (payload.getPreferredSlotAt() == null) {
                payload.setPreferredSlotAt(tokenDate.atTime(payload.getSlotStart()));
            }
            return;
        }
        if (payload.getBookingType() == null || payload.getBookingType().isBlank()) {
            payload.setBookingType(QueueWaitingOrder.BOOKING_WALK_IN);
        }
    }

    private void reserveSlotOrFail(
            Long tenantId, String shopId, Long doctorId, LocalDate tokenDate, LocalTime slotStart) {
        queueTokenRepository
                .findOccupiedSlotForUpdate(tenantId, shopId, doctorId, tokenDate, slotStart)
                .ifPresent(existing -> {
                    throw new SlotAlreadyBookedException("This time slot is already booked");
                });
    }

    private static boolean isReleasedSlotStatus(String status) {
        String normalized = normalizeStatus(status);
        return STATUS_CANCELLED.equals(normalized) || STATUS_NO_SHOW.equals(normalized);
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

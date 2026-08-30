package com.shopmanagement.queueservice.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shopmanagement.queueservice.model.QueueToken;

public interface QueueTokenRepository extends JpaRepository<QueueToken, Long> {

    List<QueueToken> findByTenantIdAndShopIdAndDoctorIdAndTokenDateOrderByTokenNumberAsc(
            Long tenantId, String shopId, Long doctorId, LocalDate tokenDate);

    List<QueueToken> findByTenantIdAndShopIdAndBranchIdAndTokenDateAndStatusOrderByTokenNumberAsc(
            Long tenantId, String shopId, Long branchId, LocalDate tokenDate, String status);

    Optional<QueueToken> findByIdAndTenantIdAndShopId(Long id, Long tenantId, String shopId);

    List<QueueToken> findByTenantIdAndShopIdAndDoctorIdAndStatusInAndTokenDateGreaterThanEqualOrderByTokenDateDescTokenNumberAsc(
            Long tenantId, String shopId, Long doctorId, List<String> statuses, LocalDate fromDate);

    List<QueueToken> findByTenantIdAndShopIdAndPatientIdAndDoctorIdAndStatusInOrderByTokenDateDescTokenNumberDesc(
            Long tenantId, String shopId, Long patientId, Long doctorId, List<String> statuses);

    List<QueueToken> findByTenantIdAndShopIdAndPatientIdAndStatusInOrderByTokenDateDescTokenNumberDesc(
            Long tenantId, String shopId, Long patientId, List<String> statuses);

    List<QueueToken> findByTenantIdAndShopIdAndAppointmentId(Long tenantId, String shopId, Long appointmentId);

    Optional<QueueToken> findFirstByTenantIdAndShopIdAndConsultationIdAndStatusInOrderByIdDesc(
            Long tenantId, String shopId, Long consultationId, List<String> statuses);

    @Query("""
            SELECT COALESCE(MAX(q.tokenNumber), 0) FROM QueueToken q
            WHERE q.tenantId = :tenantId AND q.shopId = :shopId
              AND q.doctorId = :doctorId AND q.tokenDate = :tokenDate
            """)
    int findMaxTokenNumber(
            @Param("tenantId") Long tenantId,
            @Param("shopId") String shopId,
            @Param("doctorId") Long doctorId,
            @Param("tokenDate") LocalDate tokenDate);

    List<QueueToken> findByTenantIdAndShopIdAndDoctorIdAndTokenDateAndSlotStartIsNotNull(
            Long tenantId, String shopId, Long doctorId, LocalDate tokenDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT q FROM QueueToken q
            WHERE q.tenantId = :tenantId AND q.shopId = :shopId
              AND q.doctorId = :doctorId AND q.tokenDate = :tokenDate
              AND q.slotStart = :slotStart
              AND UPPER(q.status) NOT IN ('CANCELLED', 'NO_SHOW', 'COMPLETED')
            """)
    Optional<QueueToken> findOccupiedSlotForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("shopId") String shopId,
            @Param("doctorId") Long doctorId,
            @Param("tokenDate") LocalDate tokenDate,
            @Param("slotStart") LocalTime slotStart);
}

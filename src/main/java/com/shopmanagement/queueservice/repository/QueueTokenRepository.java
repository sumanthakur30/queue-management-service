package com.shopmanagement.queueservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
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
}

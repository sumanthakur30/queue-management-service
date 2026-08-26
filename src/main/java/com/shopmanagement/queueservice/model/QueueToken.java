package com.shopmanagement.queueservice.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.shopmanagement.queueservice.model.base.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "queue_tokens")
public class QueueToken extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "token_number", nullable = false)
    private Integer tokenNumber;

    @Column(name = "token_date", nullable = false)
    private LocalDate tokenDate;

    @Column(name = "queue_type", nullable = false, length = 20)
    private String queueType = "NORMAL";

    @Column(nullable = false, length = 30)
    private String status = "WAITING";

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt = LocalDateTime.now();

    @Column(name = "called_at")
    private LocalDateTime calledAt;

    @Column(name = "consult_started_at")
    private LocalDateTime consultStartedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "estimated_wait_minutes")
    private Integer estimatedWaitMinutes;

    @Column(name = "opd_room", length = 50)
    private String opdRoom;

    /** Clinical consultation for this OPD encounter (same visit across lab wait / review). */
    @Column(name = "consultation_id")
    private Long consultationId;

    /** Preferred appointment instant when Reception booked a slot (null = walk-in). */
    @Column(name = "preferred_slot_at")
    private LocalDateTime preferredSlotAt;

    /** Slot start on token_date; unique per doctor when set. */
    @Column(name = "slot_start")
    private LocalTime slotStart;

    /** WALK_IN (default / legacy) or SLOT. Null on old rows is treated as walk-in. */
    @Column(name = "booking_type", length = 20)
    private String bookingType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Integer getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(Integer tokenNumber) { this.tokenNumber = tokenNumber; }
    public LocalDate getTokenDate() { return tokenDate; }
    public void setTokenDate(LocalDate tokenDate) { this.tokenDate = tokenDate; }
    public String getQueueType() { return queueType; }
    public void setQueueType(String queueType) { this.queueType = queueType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }
    public LocalDateTime getCalledAt() { return calledAt; }
    public void setCalledAt(LocalDateTime calledAt) { this.calledAt = calledAt; }
    public LocalDateTime getConsultStartedAt() { return consultStartedAt; }
    public void setConsultStartedAt(LocalDateTime consultStartedAt) { this.consultStartedAt = consultStartedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Integer getEstimatedWaitMinutes() { return estimatedWaitMinutes; }
    public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) { this.estimatedWaitMinutes = estimatedWaitMinutes; }
    public String getOpdRoom() { return opdRoom; }
    public void setOpdRoom(String opdRoom) { this.opdRoom = opdRoom; }
    public Long getConsultationId() { return consultationId; }
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }
    public LocalDateTime getPreferredSlotAt() { return preferredSlotAt; }
    public void setPreferredSlotAt(LocalDateTime preferredSlotAt) { this.preferredSlotAt = preferredSlotAt; }
    public LocalTime getSlotStart() { return slotStart; }
    public void setSlotStart(LocalTime slotStart) { this.slotStart = slotStart; }
    public String getBookingType() { return bookingType; }
    public void setBookingType(String bookingType) { this.bookingType = bookingType; }
}

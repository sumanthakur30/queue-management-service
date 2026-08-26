package com.shopmanagement.queueservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import com.shopmanagement.queueservice.filter.RequestIdFilter;
import com.shopmanagement.queueservice.model.QueueToken;
import com.shopmanagement.queueservice.repository.QueueTokenRepository;
import com.shopmanagement.queueservice.support.QueueWaitingOrder;
import com.shopmanagement.queueservice.support.SlotAlreadyBookedException;

class QueueServicePreferredSlotTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 26);
    private static final LocalTime NINE_THIRTY = LocalTime.of(9, 30);

    private QueueTokenRepository repository;
    private QueueService service;

    @BeforeEach
    void bindTenant() {
        RequestIdFilter.bindForTest(1L, "POLY-DEMO-01", "CLINIC_ADMIN", List.of("MANAGE_QUEUE"));
        repository = Mockito.mock(QueueTokenRepository.class);
        service = new QueueService(repository);
        when(repository.findMaxTokenNumber(eq(1L), eq("POLY-DEMO-01"), any(), any())).thenReturn(2);
        when(repository.findByTenantIdAndShopIdAndDoctorIdAndTokenDateOrderByTokenNumberAsc(
                        eq(1L), eq("POLY-DEMO-01"), any(), any()))
                .thenReturn(List.of());
        when(repository.save(any(QueueToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearTenant() {
        RequestIdFilter.clearForTest();
    }

    @Test
    void walkInWithoutSlotStillGeneratesToken() {
        QueueToken saved = service.generateToken(basePayload(17L, TODAY, null));

        assertEquals(3, saved.getTokenNumber());
        assertEquals(QueueService.STATUS_WAITING, saved.getStatus());
        assertEquals(QueueWaitingOrder.BOOKING_WALK_IN, saved.getBookingType());
        assertNull(saved.getSlotStart());
        verify(repository, never()).findOccupiedSlotForUpdate(any(), any(), any(), any(), any());
    }

    @Test
    void slotConflictFailsWhenSlotAlreadyOccupied() {
        QueueToken existing = occupied(17L, TODAY, NINE_THIRTY);
        when(repository.findOccupiedSlotForUpdate(1L, "POLY-DEMO-01", 17L, TODAY, NINE_THIRTY))
                .thenReturn(Optional.of(existing));

        SlotAlreadyBookedException ex = assertThrows(
                SlotAlreadyBookedException.class,
                () -> service.generateToken(basePayload(17L, TODAY, NINE_THIRTY)));
        assertEquals(SlotAlreadyBookedException.MESSAGE, ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void uniqueConstraintConflictMapsTo409Message() {
        when(repository.findOccupiedSlotForUpdate(1L, "POLY-DEMO-01", 17L, TODAY, NINE_THIRTY))
                .thenReturn(Optional.empty());
        when(repository.save(any(QueueToken.class))).thenThrow(new DataIntegrityViolationException("ux_queue_tokens_doctor_date_slot"));

        SlotAlreadyBookedException ex = assertThrows(
                SlotAlreadyBookedException.class,
                () -> service.generateToken(basePayload(17L, TODAY, NINE_THIRTY)));
        assertEquals(SlotAlreadyBookedException.MESSAGE, ex.getMessage());
    }

    @Test
    void slotBookingPersistsPreferredTimeAndType() {
        when(repository.findOccupiedSlotForUpdate(1L, "POLY-DEMO-01", 17L, TODAY, LocalTime.of(9, 0)))
                .thenReturn(Optional.empty());

        QueueToken saved = service.generateToken(basePayload(17L, TODAY, LocalTime.of(9, 0)));

        ArgumentCaptor<QueueToken> captor = ArgumentCaptor.forClass(QueueToken.class);
        verify(repository).save(captor.capture());
        QueueToken stored = captor.getValue();
        assertEquals(QueueWaitingOrder.BOOKING_SLOT, stored.getBookingType());
        assertEquals(LocalTime.of(9, 0), stored.getSlotStart());
        assertEquals(TODAY.atTime(LocalTime.of(9, 0)), stored.getPreferredSlotAt());
        assertEquals(3, saved.getTokenNumber());
    }

    @Test
    void differentDoctorsCanShareTheSameSlot() {
        when(repository.findOccupiedSlotForUpdate(1L, "POLY-DEMO-01", 18L, TODAY, NINE_THIRTY))
                .thenReturn(Optional.empty());

        QueueToken saved = service.generateToken(basePayload(18L, TODAY, NINE_THIRTY));

        verify(repository).findOccupiedSlotForUpdate(1L, "POLY-DEMO-01", 18L, TODAY, NINE_THIRTY);
        assertEquals(NINE_THIRTY, saved.getSlotStart());
        assertEquals(18L, saved.getDoctorId());
    }

    @Test
    void sameDoctorCanReuseSlotOnAnotherDate() {
        LocalDate nextDay = TODAY.plusDays(1);
        when(repository.findOccupiedSlotForUpdate(1L, "POLY-DEMO-01", 17L, nextDay, NINE_THIRTY))
                .thenReturn(Optional.empty());

        QueueToken saved = service.generateToken(basePayload(17L, nextDay, NINE_THIRTY));

        verify(repository).findOccupiedSlotForUpdate(1L, "POLY-DEMO-01", 17L, nextDay, NINE_THIRTY);
        assertEquals(nextDay, saved.getTokenDate());
        assertEquals(NINE_THIRTY, saved.getSlotStart());
    }

    @Test
    void cancelFreesSlotForRebooking() {
        QueueToken live = occupied(17L, TODAY, NINE_THIRTY);
        live.setId(44L);
        live.setStatus(QueueService.STATUS_WAITING);
        when(repository.findByIdAndTenantIdAndShopId(44L, 1L, "POLY-DEMO-01")).thenReturn(Optional.of(live));

        QueueToken cancelled = service.cancel(44L);
        assertEquals(QueueService.STATUS_CANCELLED, cancelled.getStatus());

        when(repository.findOccupiedSlotForUpdate(1L, "POLY-DEMO-01", 17L, TODAY, NINE_THIRTY))
                .thenReturn(Optional.empty());
        QueueToken again = service.generateToken(basePayload(17L, TODAY, NINE_THIRTY));
        assertEquals(NINE_THIRTY, again.getSlotStart());
    }

    @Test
    void bookedSlotsOmitsReleasedStatuses() {
        QueueToken live = occupied(17L, TODAY, NINE_THIRTY);
        live.setId(1L);
        live.setTokenNumber(3);
        live.setStatus(QueueService.STATUS_WAITING);
        QueueToken cancelled = occupied(17L, TODAY, LocalTime.of(9, 45));
        cancelled.setId(2L);
        cancelled.setStatus(QueueService.STATUS_CANCELLED);
        QueueToken completed = occupied(17L, TODAY, LocalTime.of(10, 0));
        completed.setId(3L);
        completed.setStatus(QueueService.STATUS_COMPLETED);
        when(repository.findByTenantIdAndShopIdAndDoctorIdAndTokenDateAndSlotStartIsNotNull(
                        1L, "POLY-DEMO-01", 17L, TODAY))
                .thenReturn(List.of(live, cancelled, completed));

        Map<String, Object> body = service.bookedSlots(17L, TODAY);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> booked = (List<Map<String, Object>>) body.get("bookedSlots");
        assertEquals(1, booked.size());
        assertEquals("09:30", booked.get(0).get("slotStart"));
        assertEquals(3, booked.get(0).get("tokenNumber"));
    }

    private static QueueToken occupied(Long doctorId, LocalDate date, LocalTime slot) {
        QueueToken existing = new QueueToken();
        existing.setId(99L);
        existing.setDoctorId(doctorId);
        existing.setTokenDate(date);
        existing.setSlotStart(slot);
        existing.setStatus(QueueService.STATUS_WAITING);
        return existing;
    }

    private static QueueToken basePayload(Long doctorId, LocalDate date, LocalTime slotStart) {
        QueueToken payload = new QueueToken();
        payload.setBranchId(1L);
        payload.setAppointmentId(50L);
        payload.setDoctorId(doctorId);
        payload.setPatientId(80L);
        payload.setQueueType("WALK_IN");
        payload.setTokenDate(date);
        payload.setSlotStart(slotStart);
        return payload;
    }
}

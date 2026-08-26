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
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.shopmanagement.queueservice.filter.RequestIdFilter;
import com.shopmanagement.queueservice.model.QueueToken;
import com.shopmanagement.queueservice.repository.QueueTokenRepository;
import com.shopmanagement.queueservice.support.QueueWaitingOrder;
import com.shopmanagement.queueservice.support.SlotAlreadyBookedException;

class QueueServicePreferredSlotTest {

    private QueueTokenRepository repository;
    private QueueService service;

    @BeforeEach
    void bindTenant() {
        RequestIdFilter.bindForTest(1L, "POLY-DEMO-01", "CLINIC_ADMIN", List.of("MANAGE_QUEUE"));
        repository = Mockito.mock(QueueTokenRepository.class);
        service = new QueueService(repository);
        when(repository.findMaxTokenNumber(1L, "POLY-DEMO-01", 17L, LocalDate.now())).thenReturn(2);
        when(repository.findByTenantIdAndShopIdAndDoctorIdAndTokenDateOrderByTokenNumberAsc(
                        eq(1L), eq("POLY-DEMO-01"), eq(17L), any()))
                .thenReturn(List.of());
        when(repository.save(any(QueueToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearTenant() {
        RequestIdFilter.clearForTest();
    }

    @Test
    void walkInWithoutSlotStillGeneratesToken() {
        QueueToken saved = service.generateToken(basePayload(null));

        assertEquals(3, saved.getTokenNumber());
        assertEquals(QueueService.STATUS_WAITING, saved.getStatus());
        assertEquals(QueueWaitingOrder.BOOKING_WALK_IN, saved.getBookingType());
        assertNull(saved.getSlotStart());
        verify(repository, never()).findOccupiedSlotForUpdate(any(), any(), any(), any(), any());
    }

    @Test
    void slotConflictFailsWhenSlotAlreadyOccupied() {
        LocalTime slot = LocalTime.of(10, 30);
        QueueToken existing = new QueueToken();
        existing.setId(99L);
        existing.setSlotStart(slot);
        when(repository.findOccupiedSlotForUpdate(1L, "POLY-DEMO-01", 17L, LocalDate.now(), slot))
                .thenReturn(Optional.of(existing));

        SlotAlreadyBookedException ex =
                assertThrows(SlotAlreadyBookedException.class, () -> service.generateToken(basePayload(slot)));
        assertTrue(ex.getMessage().contains("already booked"));
        verify(repository, never()).save(any());
    }

    @Test
    void slotBookingPersistsPreferredTimeAndType() {
        LocalTime slot = LocalTime.of(9, 0);
        when(repository.findOccupiedSlotForUpdate(1L, "POLY-DEMO-01", 17L, LocalDate.now(), slot))
                .thenReturn(Optional.empty());

        QueueToken saved = service.generateToken(basePayload(slot));

        ArgumentCaptor<QueueToken> captor = ArgumentCaptor.forClass(QueueToken.class);
        verify(repository).save(captor.capture());
        QueueToken stored = captor.getValue();
        assertEquals(QueueWaitingOrder.BOOKING_SLOT, stored.getBookingType());
        assertEquals(slot, stored.getSlotStart());
        assertEquals(LocalDate.now().atTime(slot), stored.getPreferredSlotAt());
        assertEquals(3, saved.getTokenNumber());
    }

    private static QueueToken basePayload(LocalTime slotStart) {
        QueueToken payload = new QueueToken();
        payload.setBranchId(1L);
        payload.setAppointmentId(50L);
        payload.setDoctorId(17L);
        payload.setPatientId(80L);
        payload.setQueueType("WALK_IN");
        payload.setSlotStart(slotStart);
        return payload;
    }
}

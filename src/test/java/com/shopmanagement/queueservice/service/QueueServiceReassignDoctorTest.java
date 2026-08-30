package com.shopmanagement.queueservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.shopmanagement.queueservice.filter.RequestIdFilter;
import com.shopmanagement.queueservice.model.QueueToken;
import com.shopmanagement.queueservice.repository.QueueTokenRepository;

class QueueServiceReassignDoctorTest {

    private QueueTokenRepository repository;
    private QueueService service;

    @BeforeEach
    void setUp() {
        repository = mock(QueueTokenRepository.class);
        service = new QueueService(repository);
        threadLocal("currentTenantId").set(1L);
        threadLocal("currentShopId").set("POLY-DEMO-01");
        threadLocal("currentRole").set("SHOP_OWNER");
        threadLocal("currentPermissions").set(List.of("MANAGE_QUEUE"));
    }

    @AfterEach
    void tearDown() {
        threadLocal("currentTenantId").remove();
        threadLocal("currentShopId").remove();
        threadLocal("currentRole").remove();
        threadLocal("currentPermissions").remove();
    }

    @Test
    void movesOpenTokenToNewDoctorAndRenumbers() {
        QueueToken token = new QueueToken();
        token.setId(9L);
        token.setDoctorId(2L);
        token.setAppointmentId(41L);
        token.setStatus("WAITING");
        token.setTokenDate(LocalDate.of(2026, 8, 29));
        token.setTokenNumber(3);
        when(repository.findByTenantIdAndShopIdAndAppointmentId(1L, "POLY-DEMO-01", 41L))
                .thenReturn(List.of(token));
        when(repository.findMaxTokenNumber(1L, "POLY-DEMO-01", 7L, token.getTokenDate())).thenReturn(4);
        when(repository.findByTenantIdAndShopIdAndDoctorIdAndTokenDateOrderByTokenNumberAsc(
                1L, "POLY-DEMO-01", 7L, token.getTokenDate())).thenReturn(List.of());
        when(repository.save(token)).thenReturn(token);

        QueueToken moved = service.reassignAppointmentDoctor(41L, 7L);

        assertEquals(7L, moved.getDoctorId());
        assertEquals(5, moved.getTokenNumber());
        assertEquals("WALK_IN", moved.getBookingType());
        assertNull(moved.getSlotStart());
    }

    @Test
    void movesWaitingForLabResultsToken() {
        QueueToken token = new QueueToken();
        token.setId(272L);
        token.setDoctorId(7L);
        token.setAppointmentId(280L);
        token.setStatus("WAITING_FOR_LAB_RESULTS");
        token.setTokenDate(LocalDate.of(2026, 8, 30));
        token.setTokenNumber(4);
        when(repository.findByTenantIdAndShopIdAndAppointmentId(1L, "POLY-DEMO-01", 280L))
                .thenReturn(List.of(token));
        when(repository.findMaxTokenNumber(1L, "POLY-DEMO-01", 8L, token.getTokenDate())).thenReturn(1);
        when(repository.findByTenantIdAndShopIdAndDoctorIdAndTokenDateOrderByTokenNumberAsc(
                1L, "POLY-DEMO-01", 8L, token.getTokenDate())).thenReturn(List.of());
        when(repository.save(token)).thenReturn(token);

        QueueToken moved = service.reassignAppointmentDoctor(280L, 8L);

        assertEquals(8L, moved.getDoctorId());
        assertEquals("WAITING_FOR_LAB_RESULTS", moved.getStatus());
        assertEquals(2, moved.getTokenNumber());
    }

    @Test
    void movesInConsultationToken() {
        QueueToken token = new QueueToken();
        token.setId(10L);
        token.setDoctorId(7L);
        token.setAppointmentId(41L);
        token.setStatus("IN_CONSULTATION");
        token.setTokenDate(LocalDate.of(2026, 8, 30));
        token.setTokenNumber(1);
        when(repository.findByTenantIdAndShopIdAndAppointmentId(1L, "POLY-DEMO-01", 41L))
                .thenReturn(List.of(token));
        when(repository.findMaxTokenNumber(1L, "POLY-DEMO-01", 8L, token.getTokenDate())).thenReturn(0);
        when(repository.findByTenantIdAndShopIdAndDoctorIdAndTokenDateOrderByTokenNumberAsc(
                1L, "POLY-DEMO-01", 8L, token.getTokenDate())).thenReturn(List.of());
        when(repository.save(token)).thenReturn(token);

        QueueToken moved = service.reassignAppointmentDoctor(41L, 8L);

        assertEquals(8L, moved.getDoctorId());
        assertEquals("IN_CONSULTATION", moved.getStatus());
    }

    @SuppressWarnings("unchecked")
    private static <T> ThreadLocal<T> threadLocal(String field) {
        return (ThreadLocal<T>) ReflectionTestUtils.getField(RequestIdFilter.class, field);
    }
}

package com.shopmanagement.queueservice.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.shopmanagement.queueservice.model.QueueToken;
import com.shopmanagement.queueservice.service.QueueService;

class QueueWaitingOrderTest {

    @Test
    void slotPatientsSortByAppointmentTimeAmongWaiting() {
        QueueToken lateSlot = waiting(3, LocalTime.of(11, 0), LocalDateTime.of(2026, 8, 26, 8, 0));
        QueueToken earlySlot = waiting(8, LocalTime.of(9, 30), LocalDateTime.of(2026, 8, 26, 8, 5));
        QueueToken walkIn = waiting(1, null, LocalDateTime.of(2026, 8, 26, 8, 10));

        List<QueueToken> ordered = QueueWaitingOrder.orderTodayQueue(List.of(lateSlot, walkIn, earlySlot));

        assertEquals(List.of(1, 8, 3), ordered.stream().map(QueueToken::getTokenNumber).toList());
    }

    @Test
    void walkInsKeepArrivalTokenOrder() {
        QueueToken first = waiting(1, null, LocalDateTime.of(2026, 8, 26, 9, 0));
        QueueToken second = waiting(2, null, LocalDateTime.of(2026, 8, 26, 9, 5));
        QueueToken completed = token(4, null, QueueService.STATUS_COMPLETED, LocalDateTime.of(2026, 8, 26, 8, 0));

        List<QueueToken> ordered = QueueWaitingOrder.orderTodayQueue(List.of(second, completed, first));

        assertEquals(List.of(1, 2, 4), ordered.stream().map(QueueToken::getTokenNumber).toList());
    }

    private static QueueToken waiting(int tokenNumber, LocalTime slotStart, LocalDateTime checkedInAt) {
        return token(tokenNumber, slotStart, QueueService.STATUS_WAITING, checkedInAt);
    }

    private static QueueToken token(int tokenNumber, LocalTime slotStart, String status, LocalDateTime checkedInAt) {
        QueueToken token = new QueueToken();
        token.setTokenNumber(tokenNumber);
        token.setTokenDate(LocalDate.of(2026, 8, 26));
        token.setSlotStart(slotStart);
        token.setBookingType(slotStart != null ? QueueWaitingOrder.BOOKING_SLOT : QueueWaitingOrder.BOOKING_WALK_IN);
        token.setStatus(status);
        token.setPriority(0);
        token.setCheckedInAt(checkedInAt);
        return token;
    }
}

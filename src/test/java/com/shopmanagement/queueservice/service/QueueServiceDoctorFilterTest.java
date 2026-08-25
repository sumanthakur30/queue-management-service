package com.shopmanagement.queueservice.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QueueServiceDoctorFilterTest {

    @Test
    void todayQueueRequiresDoctorId() {
        QueueService service = new QueueService(null);
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> service.todayQueue(null, null));
        assertTrue(ex.getMessage().contains("doctorId"));
    }

    @Test
    void attentionQueueRequiresDoctorId() {
        QueueService service = new QueueService(null);
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> service.attentionQueue(null, 7));
        assertTrue(ex.getMessage().contains("doctorId"));
    }
}

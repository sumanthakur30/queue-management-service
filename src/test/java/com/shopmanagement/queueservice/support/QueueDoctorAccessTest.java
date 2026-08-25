package com.shopmanagement.queueservice.support;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.shopmanagement.queueservice.model.QueueToken;

class QueueDoctorAccessTest {

    @Test
    void allowsMatchingDoctorId() {
        QueueToken token = new QueueToken();
        token.setDoctorId(17L);
        assertDoesNotThrow(() -> QueueDoctorAccess.requireDoctorMatch(token, 17L));
    }

    @Test
    void allowsMissingDoctorIdForBackwardCompatibleClients() {
        QueueToken token = new QueueToken();
        token.setDoctorId(17L);
        assertDoesNotThrow(() -> QueueDoctorAccess.requireDoctorMatch(token, null));
    }

    @Test
    void rejectsAnotherDoctorsToken() {
        QueueToken token = new QueueToken();
        token.setDoctorId(17L);
        assertThrows(SecurityException.class, () -> QueueDoctorAccess.requireDoctorMatch(token, 99L));
    }

    @Test
    void rejectsNullToken() {
        assertThrows(IllegalArgumentException.class, () -> QueueDoctorAccess.requireDoctorMatch(null, 17L));
    }
}

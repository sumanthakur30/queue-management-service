package com.shopmanagement.queueservice.support;

import com.shopmanagement.queueservice.model.QueueToken;

/**
 * doctorId is the source of truth for OPD queue / visit access.
 * Display names are never used for authorization.
 */
public final class QueueDoctorAccess {

    private QueueDoctorAccess() {
    }

    public static void requireDoctorMatch(QueueToken token, Long doctorId) {
        if (token == null) {
            throw new IllegalArgumentException("Queue token is required");
        }
        if (doctorId == null || doctorId <= 0) {
            return;
        }
        if (token.getDoctorId() == null || !token.getDoctorId().equals(doctorId)) {
            throw new SecurityException("Forbidden: queue token belongs to another doctor");
        }
    }
}

package com.shopmanagement.queueservice.support;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.shopmanagement.queueservice.model.QueueToken;
import com.shopmanagement.queueservice.service.QueueService;

/**
 * Doctor Today is the same token list. Waiting SLOT rows sort by appointment time;
 * walk-ins (no slot) keep arrival / token-number order.
 */
public final class QueueWaitingOrder {

    public static final String BOOKING_WALK_IN = "WALK_IN";
    public static final String BOOKING_SLOT = "SLOT";

    private QueueWaitingOrder() {
    }

    public static boolean isSlotBooking(QueueToken token) {
        if (token == null) {
            return false;
        }
        if (token.getSlotStart() != null) {
            return true;
        }
        return BOOKING_SLOT.equalsIgnoreCase(token.getBookingType());
    }

    public static List<QueueToken> orderTodayQueue(List<QueueToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return tokens == null ? List.of() : tokens;
        }
        List<QueueToken> waiting = new ArrayList<>();
        List<QueueToken> rest = new ArrayList<>();
        for (QueueToken token : tokens) {
            if (QueueService.STATUS_WAITING.equalsIgnoreCase(token.getStatus())) {
                waiting.add(token);
            } else {
                rest.add(token);
            }
        }
        waiting.sort(waitingComparator());
        List<QueueToken> ordered = new ArrayList<>(waiting.size() + rest.size());
        ordered.addAll(waiting);
        ordered.addAll(rest);
        return ordered;
    }

    public static Comparator<QueueToken> waitingComparator() {
        return (a, b) -> {
            boolean aSlot = isSlotBooking(a);
            boolean bSlot = isSlotBooking(b);
            if (aSlot && bSlot) {
                int bySlot = compareSlotStart(a, b);
                if (bySlot != 0) {
                    return bySlot;
                }
                return Integer.compare(nz(a.getTokenNumber()), nz(b.getTokenNumber()));
            }
            if (!aSlot && !bSlot) {
                int byPriority = Integer.compare(nz(b.getPriority()), nz(a.getPriority()));
                if (byPriority != 0) {
                    return byPriority;
                }
                return Integer.compare(nz(a.getTokenNumber()), nz(b.getTokenNumber()));
            }
            int byTime = effectiveWaitTime(a).compareTo(effectiveWaitTime(b));
            if (byTime != 0) {
                return byTime;
            }
            return Integer.compare(nz(a.getTokenNumber()), nz(b.getTokenNumber()));
        };
    }

    public static LocalDateTime effectiveWaitTime(QueueToken token) {
        if (token.getSlotStart() != null && token.getTokenDate() != null) {
            return token.getTokenDate().atTime(token.getSlotStart());
        }
        if (token.getPreferredSlotAt() != null) {
            return token.getPreferredSlotAt();
        }
        if (token.getCheckedInAt() != null) {
            return token.getCheckedInAt();
        }
        return LocalDateTime.MIN;
    }

    private static int compareSlotStart(QueueToken a, QueueToken b) {
        if (a.getSlotStart() != null && b.getSlotStart() != null) {
            return a.getSlotStart().compareTo(b.getSlotStart());
        }
        return effectiveWaitTime(a).compareTo(effectiveWaitTime(b));
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}

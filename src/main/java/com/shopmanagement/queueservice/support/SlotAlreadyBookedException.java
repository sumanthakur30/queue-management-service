package com.shopmanagement.queueservice.support;

public class SlotAlreadyBookedException extends IllegalStateException {

    public static final String MESSAGE = "This time slot has already been booked. Please select another time.";

    public SlotAlreadyBookedException() {
        super(MESSAGE);
    }

    public SlotAlreadyBookedException(String message) {
        super(message != null && !message.isBlank() ? message : MESSAGE);
    }
}

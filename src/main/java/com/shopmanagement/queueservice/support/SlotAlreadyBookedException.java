package com.shopmanagement.queueservice.support;

public class SlotAlreadyBookedException extends IllegalStateException {

    public SlotAlreadyBookedException(String message) {
        super(message);
    }
}

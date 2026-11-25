package com.mirpur.advising.shared.exception;



public final class SeatFullException extends AppException {
    public SeatFullException(String message) {
        super(message);
    }
    @Override public int status() {
        return 400;
    }
}

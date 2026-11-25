package com.mirpur.advising.shared.exception;



public final class ValidationException extends AppException {
    public ValidationException(String message) {
        super(message);
    }
    @Override public int status() {
        return 400;
    }
}

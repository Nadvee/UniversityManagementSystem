package com.mirpur.advising.shared.exception;



public abstract class AppException extends RuntimeException {
    protected AppException(String message) {
        super(message);
    }
    public abstract int status();
}

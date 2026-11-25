package com.mirpur.advising.shared.web;

import com.mirpur.advising.shared.exception.AppException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice public class Errors {
    @ExceptionHandler(AppException.class) ResponseEntity < ? > application(AppException e) {
        return ResponseEntity.status(e.status()).body(java.util.Map.of("message", e.getMessage()));
    }
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity < ? > status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of("message", e.getReason() == null ? "Request failed." : e.getReason()));
    }
    @ExceptionHandler( {
        DataIntegrityViolationException.class
    }) ResponseEntity < ? > conflict(Exception e) {
        return ResponseEntity.status(409).body(java.util.Map.of("message", "The record already exists or conflicts with related data."));
    }
    @ExceptionHandler( {
        IllegalArgumentException.class, HttpMessageNotReadableException.class
    }) ResponseEntity < ? > bad(Exception e) {
        return ResponseEntity.badRequest().body(java.util.Map.of("message", "Please check the supplied values."));
    }
}

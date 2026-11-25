package com.mirpur.advising.account;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class PasswordSetupController {
    private final PasswordSetupService service;
    public PasswordSetupController(PasswordSetupService service) { this.service = service; }
    public record SetupRequest(String id, String password, String confirmPassword) {
        @Override public String toString() { return "SetupRequest[credentials redacted]"; }
    }
    @PostMapping("/setup-password")
    public Object setup(@RequestBody SetupRequest request) {
        return service.setPassword(request.id(), request.password(), request.confirmPassword());
    }
}

package com.mirpur.advising.account;

import com.mirpur.advising.account.CommonService.*;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api") public class CommonController {
    private final CommonService service;
    public CommonController(CommonService service) {
        this.service = service;
    }
    @PostMapping("/auth/login") public Map < String, Object > login(
    @RequestBody Map < String, String > body) {
        return service.login(body);
    }
    @GetMapping("/auth/me") public Map < String, Object > me() {
        return service.me();
    }
    @GetMapping("/reference") public Map < String, Object > reference() {
        return service.reference();
    }
    @GetMapping("/notices") public Object notices() {
        return service.notices();
    }
}

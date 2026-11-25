package com.mirpur.advising.advising;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api") public class StudentController {
    private final StudentService service;
    public StudentController(StudentService service) { this.service=service; }
    @GetMapping("/dashboard/me") public Object dashboard() { return service.dashboard(); }
    @GetMapping("/advising/state") public Object state() { return service.state(); }
    @GetMapping("/advising/courses") public Object courses() { return service.courses(); }
    @PostMapping("/advising/select") public Object select(@RequestBody Map<String,Object> body) { return service.select(body); }
    @PostMapping("/advising/confirm") public Object confirm(@RequestBody Map<String,Object> body) { return service.confirm(body); }
    @DeleteMapping("/advising/hold/{id}") public void remove(@PathVariable long id) { service.remove(id); }
    @GetMapping("/routine/me") public Object routine() { return service.routine(); }
    @GetMapping("/drop/eligible") public Object eligible() { return service.eligible(); }
    @PostMapping("/drop/confirm") public Object drop(@RequestBody StudentService.DropRequest body) { return service.drop(body); }
}

package com.mirpur.advising.admin;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin") public class AdminController {
    private final AdminConsoleService service;
    public AdminController(AdminConsoleService service) { this.service=service; }
    @GetMapping({"/students","/teachers"}) public Object accounts(jakarta.servlet.http.HttpServletRequest request,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="1") int page) { return service.accounts(request.getRequestURI().endsWith("students")?"STUDENT":"TEACHER",q,page); }
    @PostMapping("/students") public Object student(@RequestBody Map<String,Object> body) { return service.student(body); }
    @PostMapping("/teachers") public Object teacher(@RequestBody Map<String,Object> body) { return service.teacher(body); }
    @GetMapping("/students/{id}") public Object studentDetail(@PathVariable long id) { return service.studentDetail(id); }
    @GetMapping("/teachers/{id}") public Object teacherDetail(@PathVariable long id) { return service.teacherDetail(id); }
    @DeleteMapping("/students/{id}") public Object deleteStudent(@PathVariable long id) { return service.deleteAccount(id,"STUDENT"); }
    @DeleteMapping("/teachers/{id}") public Object deleteTeacher(@PathVariable long id) { return service.deleteAccount(id,"TEACHER"); }
    @PatchMapping("/users/{id}") public Object active(@PathVariable long id,@RequestBody Map<String,Object> body) { return service.active(id,body); }
    @GetMapping("/teacher-choices") public Object choices() { return service.choices(); }
    @PostMapping("/teacher-choices/{id}/assign") public Object assign(@PathVariable long id,@RequestBody Map<String,Object> body) { return service.assign(id,body); }
    @PostMapping("/teacher-choices/{id}/reject") public Object reject(@PathVariable long id) { return service.reject(id); }
    @GetMapping("/offerings") public Object offerings() { return service.offerings(); }
    @GetMapping("/offerings/{id}/schedules") public Object schedules(@PathVariable long id) { return service.schedules(id); }
    @PostMapping("/offerings") public Object offering(@RequestBody Map<String,Object> body) { return service.offering(body); }
    @PutMapping("/offerings/{id}") public Object edit(@PathVariable long id,@RequestBody Map<String,Object> body) { return service.edit(id,body); }
    @PatchMapping("/offerings/{id}/status") public Object offeringStatus(@PathVariable long id,@RequestBody Map<String,Object> body) { return service.offeringStatus(id,body); }
    @DeleteMapping("/offerings/{id}") public Object deleteOffering(@PathVariable long id) { return service.deleteOffering(id); }
    @GetMapping("/sections/{id}/students") public Object roster(@PathVariable long id) { return service.roster(id); }
    @GetMapping("/windows") public Object windows() { return service.windows(); }
    @PostMapping("/windows") public Object window(@RequestBody Map<String,Object> body) { return service.window(body); }
    @PostMapping("/windows/{id}/{action}") public Object windowAction(@PathVariable long id,@PathVariable String action) { return service.windowAction(id,action); }
    @DeleteMapping("/windows/{id}") public Object deleteWindow(@PathVariable long id) { return service.deleteWindow(id); }
}

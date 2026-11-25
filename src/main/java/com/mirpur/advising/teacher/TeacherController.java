package com.mirpur.advising.teacher;

import com.mirpur.advising.teacher.TeacherService.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher") public class TeacherController {
    private final TeacherService service;
    public TeacherController(TeacherService service) {
        this.service = service;
    }
    @GetMapping("/courses") public Object courses() {
        return service.courses();
    }
    @PostMapping("/choices") public Object save(
    @RequestBody Choices b) {
        return service.save(b);
    }
    @PostMapping("/choices/confirm") public Object confirm() {
        return service.confirm();
    }
    @PostMapping("/choices/{courseId}") public Object selectCourse(@PathVariable long courseId) { return service.selectCourse(courseId); }
    @PostMapping("/choices/{courseId}/confirm") public Object confirmCourse(@PathVariable long courseId) { return service.confirmCourse(courseId); }
    @DeleteMapping("/choices/{courseId}") public Object removeCourse(@PathVariable long courseId) { return service.removeCourse(courseId); }
    @GetMapping("/sections") public Object sections() {
        return service.sections();
    }
    @GetMapping("/sections/{id}/students") public Object students(
    @PathVariable long id) {
        return service.students(id);
    }

}
